package com.felixmorgner.clycer.ui.command;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.felixmorgner.clycer.Activator;
import com.felixmorgner.clycer.algorithm.ORBS;
import com.felixmorgner.clycer.ui.SelectionHelper;

public class StartSlicing extends AbstractHandler {

    private IFile getSelectedFile() throws ExecutionException {
        IAdaptable selection = SelectionHelper.getSelectedAdaptable(IAdaptable.class);

        IFile file = selection.getAdapter(IFile.class);
        if (file == null) {
            throw new ExecutionException("The selected entity is not a file");
        }

        return file;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IFile selectedFile = null;
        try {
            selectedFile = getSelectedFile();
        } catch (ExecutionException e) {
            Activator.getDefault().logThrown(e, "Failed to acquire selected file!");
            return null;
        }

        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfiguration launchConfiguration = null;
        try {
            ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations();
            if (launchConfigurations.length == 0) {
                Activator.getDefault().logError("No launch configurations found!");
                return null;
            }
            for (ILaunchConfiguration entry : launchConfigurations) {
                if (entry.getAttribute("org.eclipse.cdt.launch.PROJECT_ATTR", "")
                        .equals(selectedFile.getProject().getName())) {
                    launchConfiguration = entry;
                    break;
                }
            }
        } catch (CoreException e) {
            Activator.getDefault().logThrown(e, "Failed to retrieve launch configurations!");
            return null;
        }

        IWorkbench workbench = PlatformUI.getWorkbench();
        IProgressService progressService = workbench.getProgressService();
        IEditorDescriptor editorDescriptor = PlatformUI.getWorkbench().getEditorRegistry()
                .getDefaultEditor(selectedFile.getName());
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        IEditorPart part;
        try {
            part = window.getActivePage().openEditor(new FileEditorInput(selectedFile), editorDescriptor.getId());
        } catch (PartInitException e) {
            throw new ExecutionException("ORBS Start failed", e);
        }
        ITextEditor editor = (ITextEditor) part;
        try {
            final IFile target = selectedFile;
            final ILaunchConfiguration configuration = launchConfiguration;
            progressService.busyCursorWhile(progressMonitor -> {
                ORBS algorithm = new ORBS(target, editor.getDocumentProvider(), editor.getEditorInput(),
                        progressMonitor, configuration);
                algorithm.startProcessing();
            });
        } catch (InvocationTargetException | InterruptedException e) {
            Activator.getDefault().logThrown(e, "ORBS execution failed");
        }
        return null;
    }

}
