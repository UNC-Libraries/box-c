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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.URIUtil;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class GetBinaryProcessorIT {

    private static final String BINARY_CONTENT = "binary content";
    private static final String MIMETYPE = "text/plain";

    private GetBinaryProcessor processor;

    @Autowired
    protected RepositoryObjectLoader repoObjLoader;
    @Autowired
    protected RepositoryObjectFactory repoObjFactory;
    @Autowired
    protected RepositoryPIDMinter pidMinter;
    @Autowired
    protected String baseAddress;
    @Autowired
    protected FcrepoClient client;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock
    Exchange exchange;
    @Mock
    Message message;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Before
    public void init() throws Exception {
        initMocks(this);
        TestHelper.setContentBase("http://localhost:48085/rest");

        File tempFileDir = tmpFolder.newFolder();

        processor = new GetBinaryProcessor();
        processor.setRepositoryObjectLoader(repoObjLoader);
        processor.setTempDirectory(tempFileDir.getAbsolutePath());

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getOut()).thenReturn(message);

        createBaseContainer(CONTENT_BASE);

        PID pid = pidMinter.mintContentPid();
        WorkObject work = repoObjFactory.createWorkObject(pid, null);
        FileObject fileObj = work.addDataFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), "file",
                MIMETYPE, null, null);

        when(message.getHeader(CdrBinaryUri)).thenReturn(
                fileObj.getOriginalFile().getPid().getRepositoryPath());
    }

    @Test
    public void binaryPresentTest() throws Exception {
        File localFile = tmpFolder.newFile();

        when(message.getHeader(CdrBinaryPath)).thenReturn(localFile.getAbsolutePath());

        processor.process(exchange);

        assertEquals(0, localFile.length());
    }

    @Test
    public void binaryNotFoundTest() throws Exception {
        File localFile = tmpFolder.newFile();
        localFile.delete();

        when(message.getHeader(CdrBinaryPath)).thenReturn(localFile.getAbsolutePath());

        processor.process(exchange);

        verify(message).setHeader(eq(CdrBinaryPath), stringCaptor.capture());

        String filePath = stringCaptor.getValue();
        File binaryFile = new File(filePath);
        assertTrue(binaryFile.exists());
        assertEquals(BINARY_CONTENT, FileUtils.readFileToString(binaryFile));
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
}
