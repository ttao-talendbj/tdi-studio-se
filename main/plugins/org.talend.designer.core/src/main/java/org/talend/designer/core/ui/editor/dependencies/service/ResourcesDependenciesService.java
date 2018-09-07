// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.editor.dependencies.service;

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.properties.Property;
import org.talend.core.model.relationship.RelationshipItemBuilder;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.model.resources.ResourceItem;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.service.IResourcesDependenciesService;
import org.talend.designer.core.ui.AbstractMultiPageTalendEditor;
import org.talend.designer.core.ui.editor.dependencies.model.JobResourceDependencyModel;
import org.talend.designer.core.ui.editor.dependencies.util.ResourceDependenciesUtil;

public class ResourcesDependenciesService implements IResourcesDependenciesService {

    @Override
    public void copyToExtResourceFolder(IRepositoryViewObject repoObject, String jobId, String jobVersion, String version,
            String rootJobLabel) {
        ResourceDependenciesUtil.copyToExtResourceFolder(repoObject, jobId, jobVersion, version, rootJobLabel);
    }

    @Override
    public String getResourcePathForContext(IProcess process, String resourceContextValue) {
        String resPath = null;
        if (process instanceof IProcess2) {
            IProcess2 process2 = (IProcess2) process;
            Property property = process2.getProperty();
            if (StringUtils.isBlank(resourceContextValue)) {
                return null;
            }
            try {
                String[] parts = resourceContextValue.split("\\|");
                if (parts.length > 2) {
                    IRepositoryViewObject repoObject = null;
                    if (RelationshipItemBuilder.LATEST_VERSION.equals(parts[1])) {
                        repoObject = ProxyRepositoryFactory.getInstance().getLastVersion(parts[0]);
                    } else {
                        repoObject = ProxyRepositoryFactory.getInstance().getSpecificVersion(parts[0], parts[1], true);
                    }
                    if (repoObject != null) {
                        JobResourceDependencyModel model = new JobResourceDependencyModel(
                                (ResourceItem) repoObject.getProperty().getItem());
                        StringBuffer joblabel = new StringBuffer();
                        if (StringUtils.isNotBlank(property.getItem().getState().getPath())) {
                            joblabel.append(property.getItem().getState().getPath() + "/");
                        }
                        joblabel.append(property.getLabel() + "_" + property.getVersion());
                        resPath = ResourceDependenciesUtil.getResourcePath(model, joblabel.toString(), parts[1]);
                        // to check if file exist, if not copy it
                        ResourceDependenciesUtil.copyToExtResourceFolder(model, property.getId(), property.getVersion(), parts[1],
                                null);
                    }

                }

            } catch (PersistenceException e) {
                ExceptionHandler.process(e);
            }
        }
        return resPath;
    }

    @Override
    public void refreshDependencyViewer() {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        IEditorReference[] editors = activePage.getEditorReferences();
        for (IEditorReference er : editors) {
            IEditorPart part = er.getEditor(false);
            if (part instanceof AbstractMultiPageTalendEditor) {
                int editorPage = ((AbstractMultiPageTalendEditor) part).getActivePage();
                if (editorPage == 3) {
                    ((AbstractMultiPageTalendEditor) part).getDependenciesEditor().setFocus();
                }
            }
        }
    }

}
