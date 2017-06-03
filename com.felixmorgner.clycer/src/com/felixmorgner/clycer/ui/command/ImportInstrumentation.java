package com.felixmorgner.clycer.ui.command;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import com.felixmorgner.clycer.ui.SelectionHelper;

public class ImportInstrumentation extends AbstractHandler {

    private ICProject findRelevantProject() throws ExecutionException {
        IAdaptable selection = SelectionHelper.getSelectedAdaptable(IAdaptable.class);
        IResource resource = selection.getAdapter(IResource.class);
        if (resource == null) {
            throw new ExecutionException("The selected entity is not a resource");
        }

        IProject relevantProject = resource.getProject();
        ICProject cproject = CoreModel.getDefault().create(relevantProject);
        if (cproject == null) {
            throw new ExecutionException("The project for the selected entity could not be found");
        }
        return cproject;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ICProject project = findRelevantProject();
        return null;
    }

}
