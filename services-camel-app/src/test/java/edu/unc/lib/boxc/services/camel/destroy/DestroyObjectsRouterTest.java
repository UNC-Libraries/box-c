package edu.unc.lib.boxc.services.camel.destroy;

import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsHelper.serializeDestroyRequest;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.UUID;

import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;

/**
 * See edu.unc.lib.dl.persist.services.destroy.DestroyObjectsJobIT for related integration test
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsRouterTest extends CamelSpringTestSupport {
    private static final String DESTROY_ROUTE = "CdrDestroyObjects";

    private static final String USER_NAME = "user";
    private static final String USER_GROUPS = "edu:lib:staff_grp";

    private AutoCloseable closeable;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private AgentPrincipals agent;

    @BeanInject(value = "repositoryObjectLoader")
    private RepositoryObjectLoader repoObjLoader;
    @BeanInject(value = "repositoryObjectFactory")
    private RepositoryObjectFactory repoObjFactory;
    @BeanInject(value = "inheritedAclFactory")
    private InheritedAclFactory inheritedAclFactory;
    @BeanInject(value = "objectPathFactory")
    private ObjectPathFactory objectPathFactory;
    @BeanInject(value = "premisLoggerFactory")
    private PremisLoggerFactory premisLoggerFactory;

    @BeanInject(value = "transactionManager")
    private TransactionManager txManager;

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

    @Before
    public void setup() {
        closeable = openMocks(this);
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(USER_GROUPS);
        agent = new AgentPrincipalsImpl(USER_NAME, testPrincipals);

        when(txManager.startTransaction()).thenReturn(tx);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/destroy-objects-context.xml");
    }

    @Test
    public void destroyObject() throws Exception {
        createContext(DESTROY_ROUTE, "direct:start");
        when(premisLoggerFactory.createPremisLogger(any())).thenReturn(premisLogger);
        when(premisLogger.buildEvent(Premis.Deletion)).thenReturn(eventBuilder);
        when(eventBuilder.addAuthorizingAgent(any())).thenReturn(eventBuilder);
        when(eventBuilder.addOutcome(true)).thenReturn(eventBuilder);
        when(eventBuilder.addEventDetail(anyString(), any())).thenReturn(eventBuilder);

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

    private void createContext(String routeName, String currentRoute) throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith(currentRoute);
                mockEndpointsAndSkip("*");
            }
        });

        context.start();
    }
}
