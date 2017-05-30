package com.felixmorgner.clycer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "com.felixmorgner.clycer"; //$NON-NLS-1$
	private static Activator plugin;
	
	public Activator() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	private static IStatus makeInfo(String message) {
		return new Status(IStatus.INFO, PLUGIN_ID, message);
	}
	
	private static IStatus makeError(String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message);
	}
	
	private static IStatus makeError(Throwable thrown) {
		return new Status(IStatus.ERROR, PLUGIN_ID, thrown.getLocalizedMessage(), thrown);
	}
	
	private static IStatus makeError(Throwable thrown, String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message, thrown);
	}
	
	private Activator log(IStatus status) {
		getLog().log(status);
		if (status.getSeverity() == IStatus.ERROR) {
			StatusManager.getManager().handle(status, StatusManager.SHOW);
		}
		return this;
	}
	
	public Activator logInfo(String message) {
		return log(makeInfo(message));
	}
	
	public Activator logError(String message) {
		return log(makeError(message));
	}
	
	public Activator logThrown(Throwable thrown) {
		return log(makeError(thrown));
	}
	
	public Activator logThrown(Throwable thrown, String message) {
		return log(makeError(thrown, message));
	}
}
