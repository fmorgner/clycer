package com.felixmorgner.clycer.ui;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class SelectionHelper {

    /**
     * Retrieve the current selection and cast it as desired
     * 
     * @param <T>
     *            The target type
     * @return The current selection cast to {@code desiredClass} if compatible.
     * @throws ExecutionException
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSelectedAdaptable(Class<T> desiredClass) throws ExecutionException {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            throw new ExecutionException("No window found");
        }

        ISelection selection = window.getSelectionService().getSelection();
        if (selection.isEmpty()) {
            throw new ExecutionException("Selection is empty");
        } else if (!(selection instanceof IStructuredSelection)) {
            throw new ExecutionException("Wrong selection type");
        }

        IStructuredSelection treeSelection = (IStructuredSelection) selection;
        if (treeSelection.size() > 1) {
            throw new ExecutionException("Too many resources selected");
        }

        Object element = treeSelection.getFirstElement();
        if (!(desiredClass.isInstance(element))) {
            throw new ExecutionException("The selected entity is not an instance of " + desiredClass.getName());
        }

        return (T) element;
    }

}
