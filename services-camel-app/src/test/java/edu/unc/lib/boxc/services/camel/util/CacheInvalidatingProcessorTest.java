package edu.unc.lib.boxc.services.camel.util;

import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import edu.unc.lib.boxc.indexing.solr.utils.MemberOrderService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectLoaderImpl;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author bbpennel
 */
public class CacheInvalidatingProcessorTest {
    private static final String FEDORA_BASE = "http://example.com/rest/";
    private static final String PID_UUID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String PID_PATH = "/de/75/d8/11/" + PID_UUID;

    @Mock
    private RepositoryObjectLoaderImpl repoObjLoader;
    @Mock
    private ObjectAclFactory objectAclFactory;
    @Mock
    private ContentPathFactory contentPathFactory;
    @Mock
    private TitleRetrievalService titleRetrievalService;
    @Mock
    private MemberOrderService memberOrderService;

    private CacheInvalidatingProcessor processor;

    @Before
    public void init() throws Exception {
        initMocks(this);
        TestHelper.setContentBase(FEDORA_BASE);
        processor = new CacheInvalidatingProcessor();
        processor.setRepositoryObjectLoader(repoObjLoader);
        processor.setObjectAclFactory(objectAclFactory);
        processor.setContentPathFactory(contentPathFactory);
        processor.setTitleRetrievalService(titleRetrievalService);
        processor.setMemberOrderService(memberOrderService);
    }

    @Test
    public void invalidatesContentPidTest() throws Exception {
        String objPath = PIDConstants.CONTENT_QUALIFIER + PID_PATH;
        Exchange exchange = mockExchange(objPath);

        processor.process(exchange);

        PID pid = PIDs.get(FEDORA_BASE + objPath);
        verify(repoObjLoader).invalidate(pid);
        verify(objectAclFactory).invalidate(pid);
        verify(contentPathFactory).invalidate(pid);
        verify(memberOrderService).invalidate(pid);
    }

    @Test
    public void ignoresDepositPidTest() throws Exception {
        String objPath = PIDConstants.DEPOSITS_QUALIFIER + PID_PATH;
        Exchange exchange = mockExchange(objPath);

        processor.process(exchange);

        verify(repoObjLoader, never()).invalidate(any(PID.class));
        verify(objectAclFactory, never()).invalidate(any(PID.class));
        verify(contentPathFactory, never()).invalidate(any(PID.class));
        verify(memberOrderService, never()).invalidate(any(PID.class));
    }

    @Test
    public void ignoresInvalidPidTest() throws Exception {
        String objPath = "/not/a/pid/okay";
        Exchange exchange = mockExchange(objPath);

        processor.process(exchange);

        verify(repoObjLoader, never()).invalidate(any(PID.class));
        verify(objectAclFactory, never()).invalidate(any(PID.class));
        verify(contentPathFactory, never()).invalidate(any(PID.class));
        verify(memberOrderService, never()).invalidate(any(PID.class));
    }

    private Exchange mockExchange(String rescPath) {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);
        when(exchange.getIn()).thenReturn(message);

        when(message.getHeader(eq(FcrepoJmsConstants.BASE_URL)))
                .thenReturn(FEDORA_BASE);
        when(message.getHeader(eq(FcrepoJmsConstants.IDENTIFIER)))
                .thenReturn(rescPath);

        return exchange;
    }
 }
