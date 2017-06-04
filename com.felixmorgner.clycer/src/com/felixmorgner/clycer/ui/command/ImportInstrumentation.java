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
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

import com.felixmorgner.clycer.Activator;
import com.felixmorgner.clycer.ui.SelectionHelper;

public class ImportInstrumentation extends AbstractHandler {

	private static class Messages extends NLS {

		private static final String BUNDLE_NAME = "com.felixmorgner.clycer.ui.command.importinstrumentation"; //$NON-NLS-1$

		public static String Error_FolderCreate;
		public static String Error_LocateClycerHeader;
		public static String Error_ReadClycerHeader;
		public static String Error_EntitiyNotAResource;
		public static String Error_DeleteExistingHeader;
		public static String Error_CreateHeader;
		public static String Error_RegisterIncludePath;

		static {
			NLS.initializeMessages(BUNDLE_NAME, ImportInstrumentation.class);
		}
		
	}

	private static final String HEADER_FILENAME = "clycer.hpp";
	private static final String RESOURCES_PATH = "resources/headers";
	private static final String FOLDER_NAME = "clycer";
	private static final String WORKSPACE_PATH_FORMAT = "\"${workspace_loc:%s}\"";

	private IProject fProject = null;

	private IFolder getClycerFolder() throws ExecutionException {
		IFolder clycerFolder = fProject.getFolder(FOLDER_NAME);
		if (clycerFolder.exists()) {
			return clycerFolder;
		}

		try {
			clycerFolder.create(0, true, null);
		} catch (CoreException e) {
			throw new ExecutionException(Messages.Error_FolderCreate, e);
		}

		return clycerFolder;
	}

	private InputStream getHeaderSourceStream() throws ExecutionException {
		Bundle pluginBundle = Activator.getDefault().getBundle();
		Enumeration<URL> entries = pluginBundle.findEntries(RESOURCES_PATH, HEADER_FILENAME, false);
		if (!entries.hasMoreElements()) {
			throw new ExecutionException(Messages.Error_LocateClycerHeader);
		}

		URL headerUrl = entries.nextElement();
		InputStream fileStream = null;
		try {
			fileStream = headerUrl.openStream();
		} catch (IOException e) {
			throw new ExecutionException(Messages.Error_ReadClycerHeader, e);
		}

		return fileStream;
	}

	@SuppressWarnings("unchecked")
	private void registerIncludePath() throws BuildException, ExecutionException {
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(fProject);
		IConfiguration[] configurations = buildInfo.getManagedProject().getConfigurations();

		String pathString = getClycerFolder().getFullPath().toPortableString();
		String headerPath = String.format(WORKSPACE_PATH_FORMAT, pathString);
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
			throw new ExecutionException(Messages.Error_EntitiyNotAResource);
		}
		fProject = resource.getProject();

		IFolder clycerFolder = getClycerFolder();
		IFile file = clycerFolder.getFile(HEADER_FILENAME);

		if (file.exists()) {
			try {
				file.delete(true, null);
			} catch (CoreException e) {
				throw new ExecutionException(Messages.Error_DeleteExistingHeader, e);
			}
		}

		InputStream headerSource = getHeaderSourceStream();
		try {
			file.create(headerSource, true, null);
		} catch (CoreException e) {
			throw new ExecutionException(Messages.Error_CreateHeader, e);
		}

		try {
			registerIncludePath();
		} catch (BuildException e) {
			throw new ExecutionException(Messages.Error_RegisterIncludePath, e);
		}

		return null;
	}

}
