package org.talend.repository.resource.ui.util;

import org.talend.commons.ui.runtime.image.IImage;

public enum EResourcesImage implements IImage {
    RESOURCE_ICON("/icons/resource.png");//$NON-NLS-1$

    private String path;

    EResourcesImage(String path) {
        this.path = path;
    }

    @Override
    public Class<?> getLocation() {
        return EResourcesImage.class;
    }

    @Override
    public String getPath() {
        return this.path;
    }

}
