package org.talend.repository.resource.view.tester;

import java.util.HashMap;
import java.util.Map;

import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.repository.tester.AbstractNodeTypeTester;

public class ResourceNodeTester extends AbstractNodeTypeTester {

    @SuppressWarnings("serial")
    private static final Map<String, ERepositoryObjectType> PROPERTY_MAPPING = new HashMap<String, ERepositoryObjectType>() {
        {
            put("isRouteResourceNode", ERepositoryObjectType.RESOURCES); //$NON-NLS-1$
        }
    };

    @Override
    protected Map<String, ERepositoryObjectType> getPropertyMapping() {
        return PROPERTY_MAPPING;
    }

}
