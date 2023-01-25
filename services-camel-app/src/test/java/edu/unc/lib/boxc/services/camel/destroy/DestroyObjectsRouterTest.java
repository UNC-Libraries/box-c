package edu.unc.lib.boxc.services.camel.destroy;

import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsHelper.serializeDestroyRequest;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.UUID;

import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@UseAdviceWith
public class DestroyObjectsRouterTest extends CamelSpringTestSupport {
    private static final String DESTROY_ROUTE = "CdrDestroyObjects";

    private static final String USER_NAME = "user";
    private static final String USER_GROUPS = "edu:lib:staff_grp";

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

    @BeanInject(value = "transactionManager")
    private TransactionManager txManager;

    @Mock
    private WorkObject workObj;
    @Mock
    private FedoraTransaction tx;
    @Mock
    private ObjectPath objPath;

    @BeforeEach
    public void setup() {
        initMocks(this);
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(USER_GROUPS);
        agent = new AgentPrincipalsImpl(USER_NAME, testPrincipals);

        when(txManager.startTransaction()).thenReturn(tx);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/destroy-objects-context.xml");
    }

    @Test
    public void destroyObject() throws Exception {
        createContext(DESTROY_ROUTE, "direct:start");

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
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith(currentRoute);
            a.mockEndpointsAndSkip("*");
        });

        context.start();
    }
}
