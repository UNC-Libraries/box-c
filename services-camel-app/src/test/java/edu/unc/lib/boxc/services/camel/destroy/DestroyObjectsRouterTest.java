package edu.unc.lib.boxc.services.camel.destroy;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsHelper.serializeDestroyRequest;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * See edu.unc.lib.dl.persist.services.destroy.DestroyObjectsJobIT for related integration test
 *
 * @author bbpennel
 *
 */
@ExtendWith(MockitoExtension.class)
public class DestroyObjectsRouterTest extends CamelTestSupport {
    private static final String DESTROY_ROUTE = "CdrDestroyObjects";

    private static final String USER_NAME = "user";
    private static final String USER_GROUPS = "edu:lib:staff_grp";

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private AgentPrincipals agent;

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObjectFactory repoObjFactory;
    @Mock
    private InheritedAclFactory inheritedAclFactory;
    @Mock
    private AccessControlService aclService;
    @Mock
    private ObjectPathFactory objectPathFactory;
    @Mock
    private PremisLoggerFactory premisLoggerFactory;
    @Mock
    private MessageSender binaryDestroyedMessageSender;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private TransactionManager txManager;
    private DestroyObjectsProcessor destroyObjectsProcessor;

    @Mock
    private WorkObject workObj;
    @Mock
    private FedoraTransaction tx;
    @Mock
    private ObjectPath objPath;
    @Mock
    private PremisLogger premisLogger;
    @Mock
    private PremisEventBuilder eventBuilder;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(USER_GROUPS);
        agent = new AgentPrincipalsImpl(USER_NAME, testPrincipals);

        destroyObjectsProcessor = new DestroyObjectsProcessor();
        destroyObjectsProcessor.setRepositoryObjectFactory(repoObjFactory);
        destroyObjectsProcessor.setRepositoryObjectLoader(repoObjLoader);
        destroyObjectsProcessor.setInheritedAclFactory(inheritedAclFactory);
        destroyObjectsProcessor.setObjectPathFactory(objectPathFactory);
        destroyObjectsProcessor.setPremisLoggerFactory(premisLoggerFactory);
        destroyObjectsProcessor.setTransactionManager(txManager);
        destroyObjectsProcessor.setAclService(aclService);
        destroyObjectsProcessor.setBinaryDestroyedMessageSender(binaryDestroyedMessageSender);
        destroyObjectsProcessor.setIndexingMessageSender(indexingMessageSender);

        var router = new DestroyObjectsRouter();
        router.setCdrDestroyStreamCamel("direct:destroyObjects");
        router.setErrorRetryDelay(0);
        router.setErrorBackOffMultiplier(1);
        router.setErrorMaxRedeliveries(1);
        router.setCdrDestroyDerivativesStreamCamel("mock:direct:destroyDerivatives");
        router.setCdrDestroyPostStreamCamel("direct:destroyPost");
        router.setLongleafDeregisterEndpoint("mock:direct:longleafDeregister");
        router.setDestroyObjectsProcessor(destroyObjectsProcessor);
        return router;
    }

    @Test
    public void destroyObject() throws Exception {
        when(txManager.startTransaction()).thenReturn(tx);

        createContext(DESTROY_ROUTE, "direct:start");
        when(premisLoggerFactory.createPremisLogger(any())).thenReturn(premisLogger);
        when(premisLogger.buildEvent(Premis.Deletion)).thenReturn(eventBuilder);
        when(eventBuilder.addAuthorizingAgent(any())).thenReturn(eventBuilder);
        when(eventBuilder.addOutcome(true)).thenReturn(eventBuilder);
        when(eventBuilder.addEventDetail(anyString(), any(Object[].class))).thenReturn(eventBuilder);

        String id = UUID.randomUUID().toString();
        PID pid = PIDs.get(id);
        when(repoObjLoader.getRepositoryObject(pid)).thenReturn(workObj);
        Model model = createDefaultModel();
        Resource resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, Cdr.Work);
        when(workObj.getResource(true)).thenReturn(resc);
        when(workObj.getPid()).thenReturn(pid);
        when(workObj.getUri()).thenReturn(pid.getRepositoryUri());
        when(workObj.getTypes()).thenReturn(Collections.singletonList(Cdr.Work.getURI()));

        when(objectPathFactory.getPath(pid)).thenReturn(objPath);
        when(objPath.toNamePath()).thenReturn("/path/to/stuff");
        when(objPath.toIdPath()).thenReturn(id);

        when(inheritedAclFactory.isMarkedForDeletion(pid)).thenReturn(true);

        String jobId = UUID.randomUUID().toString();
        DestroyObjectsRequest request = new DestroyObjectsRequest(jobId, agent, id);
        template.sendBodyAndHeaders(serializeDestroyRequest(request), null);

        verify(repoObjFactory).createOrTransformObject(eq(pid.getRepositoryUri()), any(Model.class));
    }

    @Test
    public void destroyObjectsCleanupTest() throws Exception {
        createContext("CdrDestroyObjectsCleanup", "direct:start");

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(3)
                .create();

        template.sendBody("");

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);
        // The mocked routes don't match when using parallel processing
    }

    private void createContext(String routeName, String currentRoute) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith(currentRoute);
        });
    }
}
