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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinarySubPath;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.Premis;

/**
 *
 * @author bbpennel
 *
 */
public class BinaryMetadataProcessorTest {

    private BinaryMetadataProcessor processor;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private String binaryBase;

    private static final String RESC_ID = FEDORA_BASE + "content/de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        binaryBase = tmpFolder.newFolder().getAbsolutePath();

        processor = new BinaryMetadataProcessor(binaryBase);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getIn().getHeader("CamelFcrepoUri")).thenReturn(RESC_ID);
    }

    @Test
    public void validTest() throws Exception {
        String mimetype = "text/plain";
        String checksumPrefix = "urn:sha1:";
        String checksum = "61673dacf6c6eea104e77b151584ed7215388ea3";
        File file = new File(binaryBase + "/61/67/3d/" + checksum);
        file.getParentFile().mkdirs();
        file.createNewFile();

        String binarySubPath = PIDs.get(RESC_ID).getId();

        Model model = ModelFactory.createDefaultModel();

        Resource resc = model.createResource(RESC_ID);
        resc.addProperty(RDF.type, Fcrepo4Repository.Binary);
        resc.addProperty(Ebucore.hasMimeType, mimetype);
        resc.addProperty(Premis.hasMessageDigest, checksumPrefix + checksum);

        setMessageBody(model);

        processor.process(exchange);

        verify(message).setHeader(CdrBinaryChecksum, checksum);
        verify(message).setHeader(CdrBinaryMimeType, mimetype);
        verify(message).setHeader(CdrBinaryPath, file.getAbsolutePath());
        verify(message).setHeader(CdrBinarySubPath, RepositoryPaths.idToPath(binarySubPath, HASHED_PATH_DEPTH, HASHED_PATH_SIZE));
    }

    @Test
    public void nonbinaryTest() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource(RESC_ID);
        resc.addProperty(RDF.type, createResource(Fcrepo4Repository.Resource.getURI()));

        setMessageBody(model);

        processor.process(exchange);

        verify(message, never()).setHeader(anyString(), anyString());
    }

    @Test
    public void noLocalBinaryTest() throws Exception {
        String mimetype = "text/plain";
        String checksumPrefix = "urn:sha1:";
        String checksum = "61673dacf6c6eea104e77b151584ed7215388ea3";

        Model model = ModelFactory.createDefaultModel();

        Resource resc = model.createResource(RESC_ID);
        resc.addProperty(RDF.type, Fcrepo4Repository.Binary);
        resc.addProperty(Ebucore.hasMimeType, mimetype);
        resc.addProperty(Premis.hasMessageDigest, checksumPrefix + checksum);

        setMessageBody(model);

        processor.process(exchange);

        verify(message).setHeader(CdrBinaryChecksum, checksum);
        verify(message).setHeader(CdrBinaryMimeType, mimetype);
        verify(message, never()).setHeader(eq(CdrBinaryPath), anyString());
        verify(message, never()).setHeader(eq(CdrBinarySubPath), anyString());
    }

    private void setMessageBody(Model model) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            RDFDataMgr.write(bos, model, RDFFormat.TURTLE_PRETTY);
            when(message.getBody(eq(InputStream.class)))
                    .thenReturn(new ByteArrayInputStream(bos.toByteArray()));
        }
    }
}
