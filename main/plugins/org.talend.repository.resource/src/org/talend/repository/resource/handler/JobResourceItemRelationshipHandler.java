package org.talend.repository.resource.handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.talend.core.model.properties.Item;
import org.talend.core.model.relationship.AbstractJobItemRelationshipHandler;
import org.talend.core.model.relationship.Relation;
import org.talend.core.model.relationship.RelationshipItemBuilder;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;

public class JobResourceItemRelationshipHandler extends AbstractJobItemRelationshipHandler {
    
    private static final String COMMA_TAG = ",";
    private static final String REPACE_SLASH_TAG = "\\|";
    private static final String RESOURCES_PROP = "RESOURCES_PROP";

    @Override
    protected Set<Relation> collect(Item baseItem) {
        ProcessType processType = getProcessType(baseItem);
        String resourceProperties = (String) baseItem.getProperty().getAdditionalProperties().get(RESOURCES_PROP);
        if (processType == null || StringUtils.isBlank(resourceProperties)) {
            return Collections.emptySet();
        }

        Set<Relation> relationSet = new HashSet<Relation>();
        String[] resources = resourceProperties.split(COMMA_TAG);
        for (String res : resources) {
            Relation addedRelation = new Relation(); 
            addedRelation.setId(res.split(REPACE_SLASH_TAG)[0]);
            addedRelation.setType(RelationshipItemBuilder.RESOURCE_RELATION);
            addedRelation.setVersion(RelationshipItemBuilder.LATEST_VERSION); 
            relationSet.add(addedRelation);
        }
        return relationSet;
    }

}
