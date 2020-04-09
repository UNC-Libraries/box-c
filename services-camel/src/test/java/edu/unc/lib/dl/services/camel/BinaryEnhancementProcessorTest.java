/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.services.camel;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrEditThumbnail;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrEnhancementSet;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author lfarrell
 *
 */
public class BinaryEnhancementProcessorTest {
    private BinaryEnhancementProcessor processor;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String RESC_URI = FEDORA_BASE + "content/de/75/d8/11/" + RESC_ID + "/original_file";
    private static final String THUMBNAIL_URI = FEDORA_BASE + "content/de/75/d8/11/" + RESC_ID;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        processor = new BinaryEnhancementProcessor();

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getIn().getHeader(FCREPO_URI)).thenReturn(null);
    }

    @Test
    public void testUpdateHeadersText() throws Exception {
        setMessageBody("text/plain", false);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, RESC_URI);
        verify(message).setHeader(FCREPO_RESOURCE_TYPE, Binary.getURI());
    }

    @Test
    public void testUpdateHeadersImageNonCollectionThumb() throws Exception {
        setMessageBody("image/png", false);

        processor.process(exchange);

        verify(message).setHeader(FCREPO_URI, RESC_URI);
        verify(message).setHeader(FCREPO_RESOURCE_TYPE, Binary.getURI());

        verify(message, never()).setHeader(CdrEditThumbnail, "true");
        verify(message, never()).setHeader(CdrBinaryMimeType, "image/png");
        verify(message, never()).setHeader(CdrBinaryPath, RESC_URI);
    }

    @Test
    public void testExistingUriHeader() throws Exception {
        when(exchange.getIn().getHeader(FCREPO_URI)).thenReturn(RESC_URI);
        setMessageBody("image/png", false);

        processor.process(exchange);

        verify(message, never()).setHeader(FCREPO_URI, RESC_URI);
        verify(message, never()).setHeader(FCREPO_RESOURCE_TYPE, Binary.getURI());
    }

    @Test
    public void testEditThumbnail() throws Exception {
        setMessageBody("image/png", true);
        processor.process(exchange);

        verify(message).setHeader(CdrEditThumbnail, "true");
        verify(message).setHeader(CdrBinaryMimeType, "image/png");
        verify(message).setHeader(CdrBinaryPath, THUMBNAIL_URI);
    }

    private void setMessageBody(String mimeType, boolean editThumb) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        entry.addContent(new Element("mimeType", ATOM_NS).setText(mimeType));

        if (editThumb) {
            entry.addContent(new Element("editThumbnail", ATOM_NS).setText("true"));
            entry.addContent(new Element("pid", ATOM_NS).setText(THUMBNAIL_URI));
        } else {
            entry.addContent(new Element("pid", ATOM_NS).setText(RESC_URI));
        }

        msg.addContent(entry);

        when(message.getBody()).thenReturn(msg);
    }
}
