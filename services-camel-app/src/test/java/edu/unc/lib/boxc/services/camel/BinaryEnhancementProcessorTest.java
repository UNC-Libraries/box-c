package edu.unc.lib.boxc.services.camel;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.rdf.Cdr.Collection;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.RUN_ENHANCEMENTS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author lfarrell
 *
 */
public class BinaryEnhancementProcessorTest {
    private BinaryEnhancementProcessor processor;
    private AutoCloseable closeable;

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String RESC_URI = FEDORA_BASE + "content/de/75/d8/11/" + RESC_ID + "/original_file";

    @Mock
    private Exchange exchange;
    @Mock
    private Message message;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObject repoObj;
    @Mock
    private CollectionObject collObj;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        processor = new BinaryEnhancementProcessor();
        processor.setRepositoryObjectLoader(repoObjLoader);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getIn().getHeader(FCREPO_URI)).thenReturn(null);
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(repoObj);
        when(repoObj.getTypes()).thenReturn(Collections.singletonList(Binary.getURI()));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testUpdateHeadersText() throws Exception {
        setMessageBody("text/plain", true, false);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, RESC_URI);
        verify(message).setHeader(RESOURCE_TYPE, Binary.getURI());
    }

    @Test
    public void testUpdateHeadersImageNonCollectionThumb() throws Exception {
        setMessageBody("image/png", true, false);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, RESC_URI);
        verify(message).setHeader(RESOURCE_TYPE, Binary.getURI());
        verify(message).setHeader("force", "false");
    }

    @Test
    public void testThumbForce() throws Exception {
        setMessageBody("image/png", true, true);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, RESC_URI);
        verify(message).setHeader(RESOURCE_TYPE, Binary.getURI());
        verify(message).setHeader("force", "true");
    }

    @Test
    public void testThumbNoForce() throws Exception {
        setMessageBody("image/png", true, false);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, RESC_URI);
        verify(message).setHeader(RESOURCE_TYPE, Binary.getURI());
        verify(message).setHeader("force", "false");
    }

    @Test
    public void testExistingUriHeader() throws Exception {
        when(exchange.getIn().getHeader(FCREPO_URI)).thenReturn(RESC_URI);
        setMessageBody("image/png", false, false);

        processor.process(exchange);

        verify(message, never()).setHeader(FCREPO_URI, RESC_URI);
        verify(message, never()).setHeader(RESOURCE_TYPE, Binary.getURI());
        verify(message, never()).setHeader("force", "false");
    }

    @Test
    public void testNonBinary() throws Exception {
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(collObj);
        when(collObj.getTypes()).thenReturn(Collections.singletonList(Collection.getURI()));
        setMessageBody("image/*", true, false);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, RESC_URI);
        verify(message).setHeader(RESOURCE_TYPE, Collection.getURI());
        verify(message).setHeader("force", "false");
    }

    private void setMessageBody(String mimeType, boolean addEnhancementHeader, boolean force) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        entry.addContent(new Element("mimeType", ATOM_NS).setText(mimeType));

        if (addEnhancementHeader) {
            Element enhancements = new Element(RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS);
            enhancements.addContent(new Element("pid", CDR_MESSAGE_NS).setText(RESC_URI));
            enhancements.addContent(new Element("force", CDR_MESSAGE_NS).setText(String.valueOf(force)));
            entry.addContent(enhancements);
        }

        msg.addContent(entry);

        when(message.getBody()).thenReturn(msg);
    }
}
