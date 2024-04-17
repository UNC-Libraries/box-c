package edu.unc.lib.boxc.services.camel.cdrEvents;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/**
 *
 * @author lfarrell
 *
 */
public class CdrEventProcessorTest {
    private CdrEventProcessor processor;
    private String actionType = "action_placeholder";
    private AutoCloseable closeable;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Captor
    private ArgumentCaptor<Object> bodyCaptor;

    @Before
    public void init() throws Exception {
        closeable = openMocks(this);

        processor = new CdrEventProcessor();

        when(exchange.getIn())
            .thenReturn(message);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testCreateSolrActionHeader() throws Exception {
        Document msg = new Document();
        createAtomEntry(msg, actionType);

        when(message.getBody())
            .thenReturn(msg);

        processor.process(exchange);

        verify(message).setHeader(CdrUpdateAction, actionType);
        verify(message).setBody(eq(msg));
    }

    @Test
    public void testBodyInputStream() throws Exception {
        Document msg = new Document();
        createAtomEntry(msg, actionType);

        XMLOutputter outputter = new XMLOutputter();
        when(message.getBody())
            .thenReturn(new ByteArrayInputStream(outputter.outputString(msg).getBytes()));

        processor.process(exchange);

        verify(message).setHeader(CdrUpdateAction, actionType);

        verify(message).setBody(bodyCaptor.capture());
        Object bodyObject = bodyCaptor.getValue();
        assertTrue(bodyObject instanceof Document);
    }

    @Test
    public void testBodyString() throws Exception {
        Document msg = new Document();
        createAtomEntry(msg, actionType);

        XMLOutputter outputter = new XMLOutputter();
        when(message.getBody())
            .thenReturn(outputter.outputString(msg));

        processor.process(exchange);

        verify(message).setHeader(CdrUpdateAction, actionType);

        verify(message).setBody(bodyCaptor.capture());
        Object bodyObject = bodyCaptor.getValue();
        assertTrue(bodyObject instanceof Document);
    }

    @Test
    public void testCreateSolrNoActionHeader() throws Exception {
        Document msg = new Document();
        createAtomEntry(msg, null);

        when(message.getBody())
            .thenReturn(msg);

        processor.process(exchange);

        verify(message).setHeader(CdrUpdateAction, null);
    }

    private Element createAtomEntry(Document msg, String operation) {
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String timestamp = fmt.print(DateTimeUtils.currentTimeMillis());
        entry.addContent(new Element("updated", ATOM_NS).setText(timestamp));

        if (operation != null) {
            entry.addContent(new Element("title", ATOM_NS).setText(operation).setAttribute("type", "text"));
        }

        Element content = new Element("content", ATOM_NS).setAttribute("type", "text/xml");
        entry.addContent(content);

        return content;
    }
}