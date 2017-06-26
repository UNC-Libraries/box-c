/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryUri;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

/**
 * @author harring
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml","/spring-test/cdr-client-container.xml"})
public class ReplicationProcessorIT extends CamelTestSupport {

    @Autowired
    protected String baseAddress;

    @Autowired
    protected FcrepoClient client;
    
    @Autowired
    protected Repository repository;

    ReplicationProcessor processor;

    @Mock
    Exchange exchange;
    @Mock
    Message message;

    @Before
    public void init() throws IOException {
        PIDs.setRepository(repository);
        processor = new ReplicationProcessor(repository, "/tmp", 3, 100L);
        initMocks(this);
        
        when(exchange.getOut()).thenReturn(message);
    }

    private URI createBaseContainer(String name) throws IOException, FcrepoOperationFailedException {
        URI baseUri = URI.create(URIUtil.join(baseAddress, name));
        // Create a parent object to put the binary into
        try (FcrepoResponse response = client.put(baseUri).perform()) {
            return response.getLocation();
        } catch(FcrepoOperationFailedException e) {
            if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
                throw e;
            }
            // Ignore duplicate creation of base container
            return baseUri;
        }
    }

    @Test
    public void replicateFileFromFedoraTest() throws Exception {
        // Create a parent object to put the binary into
        URI contentBase = createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        PID parentPid;
        try (FcrepoResponse response = client.post(contentBase).perform()) {
            parentPid = PIDs.get(response.getLocation());
        }

        URI uri = parentPid.getRepositoryUri();

        String bodyString = "Test text";
        String filename = "test.txt";
        String mimetype = "text/plain";
        String checksum = "82022e1782b92dce5461ee636a6c5bea8509ffee";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

        BinaryObject internalObj = repository.createBinary(uri, "binary_test", contentStream, filename, mimetype, checksum, null);

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(CdrBinaryPath)).thenReturn("path/to/bin");
        when(message.getHeader(CdrBinaryChecksum)).thenReturn(checksum);
        when(message.getHeader(CdrBinaryMimeType)).thenReturn(mimetype);
        when(message.getHeader(CdrBinaryUri)).thenReturn(internalObj.getUri().toString());

        processor.process(exchange);
    }
    
    @Test
    public void replicateExternalFileTest() throws Exception {
        // Create a parent object to put the binary into
        URI contentBase = createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        PID parentPid;
        try (FcrepoResponse response = client.post(contentBase).perform()) {
            parentPid = PIDs.get(response.getLocation());
        }

        URI uri = parentPid.getRepositoryUri();

        String filename = "src/test/resources/external_file.txt";
        File testFile = new File(filename);
        InputStream contentStream = new FileInputStream(testFile);
        String mimetype = "text/plain";
        String checksum = "41cfe91611de4f56689ca6258237c448d3f91a84";

        BinaryObject externalObj = repository.createBinary(uri, "external_binary_test", contentStream, filename, mimetype, null, null);

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(CdrBinaryPath)).thenReturn("src/test/resources/external_file.txt");
        when(message.getHeader(CdrBinaryChecksum)).thenReturn(checksum);
        when(message.getHeader(CdrBinaryMimeType)).thenReturn(mimetype);
        when(message.getHeader(CdrBinaryUri)).thenReturn(externalObj.getUri().toString());

        processor.process(exchange);
    }

}
