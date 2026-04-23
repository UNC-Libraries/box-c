package edu.unc.lib.boxc.services.camel.collectionDisplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesSerializationHelper;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Processing requests to edit default display properties on a CollectionObject
 */
public class CollectionDisplayPropertiesRequestProcessor implements Processor {
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private AccessControlService aclService;
    private IndexingMessageSender indexingMessageSender;

    @Override
    public void process(Exchange exchange) throws IOException {
        var in = exchange.getIn();
        var request = CollectionDisplayPropertiesSerializationHelper.toRequest(in.getBody(String.class));
        var agent = request.getAgent();
        var pid = PIDs.get(request.getId());

        aclService.assertHasAccess("User does not have permission to set default collection display properties",
                pid, agent.getPrincipals(), Permission.editViewSettings);

        assertValid(request, pid);

        var collection = repositoryObjectLoader.getCollectionObject(pid);

        var collectionSettings = new HashMap<>();
        collectionSettings.put("sortType", request.getSortType());
        collectionSettings.put("displayType", request.getDisplayType());
        collectionSettings.put("worksOnly", request.getWorksOnly());
        ObjectMapper objectMapper = new ObjectMapper();

        repositoryObjectFactory.createExclusiveRelationship(
                collection, Cdr.collectionDefaultDisplaySettings, objectMapper.writeValueAsString(collectionSettings));

        indexingMessageSender.sendIndexingOperation(agent.getUsername(), pid,
                IndexingActionType.UPDATE_COLLECTION_DISPLAY_PROPERTIES);
    }

    private String validate(CollectionDisplayPropertiesRequest request, PID pid) {
        try {
            repositoryObjectLoader.getCollectionObject(pid);
        } catch (ObjectTypeMismatchException e) {
            return "Object is not a CollectionObject";
        }
        // Request is valid
        return null;
    }

    /**
     *  Throws an IllegalArgumentException if validate method returns any message
     * @param request CollectionDisplayPropertiesRequest
     * @param pid PID of the object to add streaming properties to
     */
    private void assertValid(CollectionDisplayPropertiesRequest request, PID pid) {
        var message = validate(request, pid);
        if (!StringUtils.isBlank(message)) {
            throw new IllegalArgumentException(message);
        }
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
