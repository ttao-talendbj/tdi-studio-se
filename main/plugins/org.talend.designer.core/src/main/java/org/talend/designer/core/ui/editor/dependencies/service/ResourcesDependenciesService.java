package org.talend.designer.core.ui.editor.dependencies.service;

import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.service.IResourcesDependenciesService;
import org.talend.designer.core.ui.editor.dependencies.util.ResourceDependenciesUtil;

public class ResourcesDependenciesService implements IResourcesDependenciesService {

    @Override
    public void copyToExtResourceFolder(IRepositoryViewObject repoObject, String jobLabel, String version) {
        ResourceDependenciesUtil.copyToExtResourceFolder(repoObject, jobLabel, version);
    }

}
