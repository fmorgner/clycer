package com.felixmorgner.clycer.algorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.launch.AbstractCLaunchDelegate2;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.felixmorgner.clycer.Activator;

public class ORBS extends AbstractCLaunchDelegate2 {

	private final IFile fTargetFile;
	private final IProject fProject;
	private final IProgressMonitor fProgressMonitor;
	private final ILaunchConfiguration fLaunchConfiguration;
	private final File fLogFile;
	private final IDocumentProvider fDocumentProvider;
	private final IEditorInput fEditorInput;

	private String fBaselineTrajectory;

	public ORBS(IFile targetFile, IDocumentProvider provider, IEditorInput input, IProgressMonitor progressMonitor,
			ILaunchConfiguration launchConfiguration) {
		fTargetFile = targetFile;
		fDocumentProvider = provider;
		fEditorInput = input;
		fProject = targetFile.getProject();
		fProgressMonitor = progressMonitor;
		fLaunchConfiguration = launchConfiguration;
		fLogFile = getWorkingDirectory().append("clycer.log").toFile();
	}

	public void startProcessing() {
		SubMonitor baselineMonitor = SubMonitor.convert(fProgressMonitor);
		baselineMonitor.beginTask("Establishing baseline trajectory", 3);

		if (!build(IncrementalProjectBuilder.FULL_BUILD, baselineMonitor) || !run(baselineMonitor)) {
			Activator.getDefault().logError("Failed to establish baseline trajectory");
			return;
		}

		try {
			fBaselineTrajectory = readTrajectory(baselineMonitor);
		} catch (IOException e) {
			Activator.getDefault().logThrown(e, "Failed to read trajectory '" + fLogFile.getAbsolutePath() + "'");
			return;
		}
		Activator.getDefault().logInfo("Established baseline trajectory:\n" + fBaselineTrajectory);
		baselineMonitor.done();

		SubMonitor orbsMonitor = SubMonitor.convert(fProgressMonitor, "Running ORBS", IProgressMonitor.UNKNOWN);
		try {
			slice(orbsMonitor);
		} catch (Exception e) {
			Activator.getDefault().logThrown(e, "Error during main algorithm execution");
		} finally {
			Activator.getDefault().logInfo("Finished slicing '" + fTargetFile + "'");
			orbsMonitor.done();
		}
	}

	private IPath getWorkingDirectory() {
		IPath workingDirectory = null;
		try {
			workingDirectory = getWorkingDirectoryPath(fLaunchConfiguration);
		} catch (CoreException e) {
		}

		if (workingDirectory == null) {
			workingDirectory = fProject.getLocation();
		}
		return workingDirectory;
	}

	private boolean build(int buildType, IProgressMonitor monitor) {
		monitor.setTaskName("Building project");
		try {
			fProject.build(buildType, monitor);
		} catch (CoreException e) {
			Activator.getDefault().logThrown(e, "Build failed");
			return false;
		}
		try {
			IMarker[] markers = fProject.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true,
					IResource.DEPTH_INFINITE);
			if (markers.length != 0) {
				for (IMarker marker : markers) {
					if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
						return false;
					}
				}
			}
		} catch (CoreException e) {
			Activator.getDefault().logThrown(e, "Build failed");
			return false;
		}
		monitor.worked(1);
		return true;
	}

	private boolean run(IProgressMonitor monitor) {
		monitor.setTaskName("Capturing trajectory");
		try {
			ILaunch launch = fLaunchConfiguration.launch(ILaunchManager.RUN_MODE, monitor);
			SubMonitor recordingMonitor = SubMonitor.convert(monitor, "Recording trajectory", IProgressMonitor.UNKNOWN);
			while (!launch.isTerminated() && !recordingMonitor.isCanceled()) {
				Thread.sleep(1000);
			}
			if (recordingMonitor.isCanceled()) {
				Activator.getDefault().logInfo("Recording of trajectory aborted by user.");
				return false;
			}
			recordingMonitor.done();
		} catch (InterruptedException | CoreException e) {
			Activator.getDefault().logThrown(e, "Trajectory recording failed: ");
			return false;
		}
		monitor.worked(1);
		return true;
	}

	private String readTrajectory(IProgressMonitor monitor) throws IOException {
		monitor.setTaskName("Reading captured trajectory");
		if (!fLogFile.exists()) {
			throw new FileNotFoundException();
		}

		byte[] trajectory;
		trajectory = Files.readAllBytes(fLogFile.toPath());
		fLogFile.delete();

		monitor.worked(1);
		return new String(trajectory, Charset.defaultCharset());
	}

	private IDocument getDocument() throws CoreException {
		return fDocumentProvider.getDocument(fEditorInput);
	}

	private void saveDocument() throws CoreException {
		fDocumentProvider.saveDocument(new NullProgressMonitor(), fEditorInput, getDocument(), true);
	}

	private void slice(IProgressMonitor monitor) throws CoreException, BadLocationException {
		IDocument document = getDocument();
		boolean didDelete;
		do {
			didDelete = false;
			int lineIndex = document.getNumberOfLines() - 1;
			while (lineIndex > 0) {
				UndoEdit undoEdit = null;
				boolean didBuild = false;
				for (int i = 1; i <= 3; ++i) {
					if (lineIndex - i >= 0) {
						undoEdit = removeWindow(document, lineIndex, lineIndex - 1);
						didBuild = build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
						if (!didBuild) {
							undoRemoval(document, undoEdit);
						} else {
							break;
						}
					} else {
						break;
					}
				}
				if (didBuild) {
					run(monitor);
					String currentTrajectory = null;
					try {
						currentTrajectory = readTrajectory(monitor);
					} catch (IOException e) {
					} finally {
						if (!fBaselineTrajectory.equals(currentTrajectory)) {
							undoRemoval(document, undoEdit);
						} else {
							didDelete = true;
							continue;
						}
					}
				}
				--lineIndex;
			}
		} while (didDelete);
	}

	private UndoEdit removeWindow(IDocument document, int first, int last) throws BadLocationException {
		int lower = document.getLineOffset(first);
		int upper = document.getLineOffset(last);
		DeleteEdit deleteEdit = new DeleteEdit(upper, lower - upper);

		UndoEdit[] undoEdit = new UndoEdit[1];
		Display.getDefault().syncExec(() -> {
			try {
				undoEdit[0] = deleteEdit.apply(document);
				saveDocument();
			} catch (MalformedTreeException | BadLocationException | CoreException e) {
				e.printStackTrace();
			}
		});
		return undoEdit[0];
	}

	private void undoRemoval(IDocument document, UndoEdit undoEdit) {
		Display.getDefault().syncExec(() -> {
			try {
				undoEdit.apply(document);
				saveDocument();
			} catch (MalformedTreeException | BadLocationException | CoreException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	protected String getPluginID() {
		return Activator.PLUGIN_ID;
	}
}
