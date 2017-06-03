package com.felixmorgner.clycer.ui.command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import com.felixmorgner.clycer.Activator;
import com.felixmorgner.clycer.ui.SelectionHelper;

public class ImportInstrumentation extends AbstractHandler {

	private IProject fProject = null;

	private IFolder getClycerFolder() throws ExecutionException {
		IFolder clycerFolder = fProject.getFolder("clycer");
		if (clycerFolder.exists()) {
			return clycerFolder;
		}

		try {
			clycerFolder.create(0, true, null);
		} catch (CoreException e) {
			throw new ExecutionException("Failed to clycer folder", e);
		}

		return clycerFolder;
	}

	private InputStream getHeaderSourceStream() throws ExecutionException {
		Enumeration<URL> entries = Activator.getDefault().getBundle().findEntries("resources/headers", "clycer.hpp",
				false);
		if (!entries.hasMoreElements()) {
			throw new ExecutionException("Failed to locate clycer.hpp source");
		}

		URL headerUrl = entries.nextElement();
		InputStream fileStream = null;
		try {
			fileStream = headerUrl.openStream();
		} catch (IOException e) {
			throw new ExecutionException("Failed to read clycer.hpp source", e);
		}

		return fileStream;
	}

	@SuppressWarnings("unchecked")
	private void registerIncludePath() throws BuildException, ExecutionException {
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(fProject);
		IConfiguration[] configurations = buildInfo.getManagedProject().getConfigurations();
		String headerPath = "\"${workspace_loc:" + getClycerFolder().getFullPath().toPortableString() + "}\"";
		for (IConfiguration configuration : configurations) {
			IToolChain toolchain = configuration.getToolChain();
			for (ITool tool : toolchain.getTools()) {
				for (IOption option : tool.getOptions()) {
					if (option.getValueType() == IOption.INCLUDE_PATH) {
						ArrayList<String> includePaths = (ArrayList<String>) option.getValue();
						includePaths.add(headerPath);
						ManagedBuildManager.setOption(configuration, tool, option,
								includePaths.toArray(new String[includePaths.size()]));
					}
				}
			}
		}
		ManagedBuildManager.saveBuildInfo(fProject, true);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IAdaptable selection = SelectionHelper.getSelectedAdaptable(IAdaptable.class);
		IResource resource = selection.getAdapter(IResource.class);
		if (resource == null) {
			throw new ExecutionException("The selected entity is not a resource");
		}
		fProject = resource.getProject();

		IFolder clycerFolder = getClycerFolder();
		IFile file = clycerFolder.getFile("clycer.hpp");

		if (file.exists()) {
			try {
				file.delete(true, null);
			} catch (CoreException e) {
				throw new ExecutionException("Failed to delete existing clycer.hpp", e);
			}
		}

		InputStream headerSource = getHeaderSourceStream();
		try {
			file.create(headerSource, true, null);
		} catch (CoreException e) {
			throw new ExecutionException("Failed to create clycer projection header", e);
		}

		try {
			registerIncludePath();
		} catch (BuildException e) {
			throw new ExecutionException("Failed to register clycer include path", e);
		}

		return null;
	}

}
