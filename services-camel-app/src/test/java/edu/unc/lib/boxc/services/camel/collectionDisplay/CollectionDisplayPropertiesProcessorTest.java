package edu.unc.lib.boxc.services.camel.collectionDisplay;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesSerializationHelper;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class CollectionDisplayPropertiesProcessorTest {
    private CollectionDisplayPropertiesRequestProcessor processor;
    private CollectionObject collectionObject;
    private PID collectionPid;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    private AutoCloseable closeable;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private IndexingMessageSender indexingMessageSender;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new CollectionDisplayPropertiesRequestProcessor();
        processor.setAclService(accessControlService);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setRepositoryObjectFactory(repositoryObjectFactory);
        processor.setIndexingMessageSender(indexingMessageSender);

        collectionPid = TestHelper.makePid();
        collectionObject = mock(CollectionObject.class);
        when(collectionObject.getPid()).thenReturn(collectionPid);
        when(repositoryObjectLoader.getCollectionObject(collectionPid)).thenReturn(collectionObject);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testCollectionDisplayPropertiesUpdateNoPermission() throws IOException {
        var exchange = createRequestExchange(collectionPid.getId(), "gallery-display", "default,normal", false);

        assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                    .assertHasAccess(any(), any(PID.class), any(), eq(Permission.viewHidden));
            processor.process(exchange);
        });
    }

    @Test
    public void testCollectionDisplayPropertiesUpdateNotCollectionObject() throws IOException {
        var anotherPid = TestHelper.makePid();
        var exchange = createRequestExchange(anotherPid.getId(), "list-display", "default,normal", false);

        assertThrows(IllegalArgumentException.class, () -> {
            doThrow(new ObjectTypeMismatchException("not a collection object")).when(repositoryObjectLoader)
                    .getCollectionObject(eq(anotherPid));
            processor.process(exchange);
        });
    }

    @Test
    public void testCollectionSetDisplayProperties() throws IOException {
        var exchange = createRequestExchange(collectionPid.getId(), "gallery-display", "default,normal", true);
        processor.process(exchange);

        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(collectionObject), eq(Cdr.collectionDefaultDisplaySettings),
                eq("{\"displayType\":\"gallery-display\",\"sortType\":\"default,normal\",\"worksOnly\":true}"));
    }

    @Test
    public void testCollectionUpdateDisplayProperties() throws IOException {
        var exchange = createRequestExchange(collectionPid.getId(), "gallery-display", "default,normal", true);
        processor.process(exchange);

        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(collectionObject), eq(Cdr.collectionDefaultDisplaySettings), eq("{\"displayType\":\"gallery-display\",\"sortType\":\"default,normal\",\"worksOnly\":true}"));

        var exchange_update = createRequestExchange(collectionPid.getId(), "list-display", "title,reverse", false);
        processor.process(exchange_update);

        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(collectionObject), eq(Cdr.collectionDefaultDisplaySettings), eq("{\"displayType\":\"list-display\",\"sortType\":\"title,reverse\",\"worksOnly\":false}"));
    }

    private Exchange createRequestExchange(String id, String displayType, String sortType, boolean worksOnly) throws IOException {
        var request = new CollectionDisplayPropertiesRequest();
        request.setAgent(agent);
        request.setId(id);
        request.setDisplayType(displayType);
        request.setSortType(sortType);
        request.setWorksOnly(worksOnly);
        return TestHelper.mockExchange(CollectionDisplayPropertiesSerializationHelper.toJson(request));
    }
}
