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
package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryUri;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.util.URIUtil;

/**
 * @author harring
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml","/spring-test/cdr-client-container.xml"})
public class ReplicationProcessorIT extends CamelTestSupport {
    private static final String MIMETYPE = "text/plain";

    @Autowired
    protected String baseAddress;

    @Autowired
    protected FcrepoClient client;

    @Autowired
    protected Repository repository;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private File replicationDir;

    private ReplicationProcessor processor;

    @Mock
    Exchange exchange;
    @Mock
    Message message;

    @Before
    public void init() throws IOException {
        replicationDir = tmpFolder.newFolder("tmp");
        replicationDir.mkdir();
        PIDs.setRepository(repository);
        processor = new ReplicationProcessor(repository, replicationDir.getAbsolutePath(), 3, 100L);
        initMocks(this);

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(CdrBinaryPath)).thenReturn("path/to/bin");
        when(message.getHeader(CdrBinaryMimeType)).thenReturn(MIMETYPE);
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

    private URI determineRepositoryPath(URI baseUri) throws IOException, FcrepoOperationFailedException {
        URI repoPath = null;
        try (FcrepoResponse response = client.post(baseUri).perform()) {
            repoPath = response.getLocation();
        } catch(FcrepoOperationFailedException e) {
            if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
                throw e;
            }
        }
        return repoPath;
    }

    @Test
    public void replicateFileInFedoraDatastoreTest() throws Exception {
        // Create a parent object to put the binary into
        URI contentBase = createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        URI binaryUri = determineRepositoryPath(contentBase);

        String bodyString = "Test text";
        String filename = "test.txt";
        String checksum = "82022e1782b92dce5461ee636a6c5bea8509ffee";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

        BinaryObject internalObj = repository.createBinary(binaryUri, "binary_test", contentStream,
                filename, MIMETYPE, checksum, null);

        when(message.getHeader(CdrBinaryChecksum)).thenReturn(checksum);
        when(message.getHeader(CdrBinaryUri)).thenReturn(internalObj.getUri().toString());

        processor.process(exchange);

        String checksumPath = "/82/02/2e/82022e1782b92dce5461ee636a6c5bea8509ffee";
        File replicatedFile = new File(replicationDir.getAbsolutePath() + checksumPath);
        String replicatedContent = FileUtils.readFileToString(replicatedFile);
        assertEquals(bodyString, replicatedContent);
    }

    @Test
    public void replicateFileStoredOnDiskTest() throws Exception {
        // Create a parent object to put the binary into
        URI contentBase = createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        URI binaryUri = determineRepositoryPath(contentBase);

        String filename = "src/test/resources/external_file.txt";
        File testFile = new File(filename);
        InputStream contentStream = new FileInputStream(testFile);
        String checksum = "9db3fcbaec92b9ccf9aa16f820184813080e77d2";

        BinaryObject externalObj = repository.createBinary(binaryUri, "external_binary_test", contentStream,
                filename, MIMETYPE, null, null);

        when(message.getHeader(CdrBinaryChecksum)).thenReturn(checksum);
        when(message.getHeader(CdrBinaryUri)).thenReturn(externalObj.getUri().toString());

        processor.process(exchange);

        String checksumPath = "/9d/b3/fc/9db3fcbaec92b9ccf9aa16f820184813080e77d2";
        File replicatedFile = new File(replicationDir.getAbsolutePath() + checksumPath);
        String replicatedContent = FileUtils.readFileToString(replicatedFile);
        InputStream streamFromTestFile = new FileInputStream(testFile);
        assertEquals(IOUtils.toString(streamFromTestFile, "UTF-8"), replicatedContent);
    }

    @Test (expected = ReplicationException.class)
    public void checksumMismatchTest() throws Exception {
        // Create a parent object to put the binary into
        URI contentBase = createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        URI binaryUri = determineRepositoryPath(contentBase);

        String filename = "src/test/resources/external_file.txt";
        File testFile = new File(filename);
        InputStream contentStream = new FileInputStream(testFile);
        String badChecksum = "41cfe91611de4f56689ca6258237c448d3f91a84";

        BinaryObject externalObj = repository.createBinary(binaryUri, "external_binary_test", contentStream,
                filename, MIMETYPE, null, null);

        when(message.getHeader(CdrBinaryChecksum)).thenReturn(badChecksum);
        when(message.getHeader(CdrBinaryUri)).thenReturn(externalObj.getUri().toString());

        processor.process(exchange);
    }

    @Test (expected = ReplicationDestinationUnavailableException.class)
    public void badReplicationLocationTest() throws Exception {
        processor = new ReplicationProcessor(repository, "/some/bad/location", 3, 100L);
    }

}
