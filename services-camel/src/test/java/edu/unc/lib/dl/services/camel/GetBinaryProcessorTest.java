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

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class GetBinaryProcessorTest {

    private static final String FILE_CONTENT = "text content";
    private static final String CONTENT_BASE_URI = "http://localhost:48085/rest";
    private static final String TEST_URI = "http://localhost:48085/rest/content/12/34/56/78/1234567890";

    private GetBinaryProcessor processor;

    @Mock
    private RepositoryObjectLoader repoObjLoader;

    @Mock
    private BinaryObject binary;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    private File localFile;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Before
    public void init() throws Exception {
        TestHelper.setContentBase(CONTENT_BASE_URI);
        initMocks(this);

        File tempFileDir = tmpFolder.newFolder();

        processor = new GetBinaryProcessor();
        processor.setRepositoryObjectLoader(repoObjLoader);
        processor.setTempDirectory(tempFileDir.getAbsolutePath());

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getOut()).thenReturn(message);

        when(message.getHeader(eq(CdrBinaryUri)))
                .thenReturn(TEST_URI);

        when(repoObjLoader.getBinaryObject(any(PID.class))).thenReturn(binary);
        when(binary.getBinaryStream()).thenReturn(new ByteArrayInputStream(FILE_CONTENT.getBytes()));
    }

    @Test
    public void binaryPresentTest() throws Exception {
        localFile = tmpFolder.newFile();
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(localFile.getAbsolutePath());

        processor.process(exchange);

        verify(message, never()).setHeader(anyString(), anyString());
    }

    @Test
    public void binaryDoesNotExistTest() throws Exception {
        localFile = tmpFolder.newFile();
        localFile.delete();
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(localFile.getAbsolutePath());

        processor.process(exchange);

        verify(message).setHeader(eq(CdrBinaryPath), stringCaptor.capture());

        String binaryPath = stringCaptor.getValue();
        File downloadedFile = new File(binaryPath);
        assertTrue(downloadedFile.exists());
        assertEquals(FILE_CONTENT, FileUtils.readFileToString(downloadedFile));
    }

    @Test
    public void noBinaryPathTest() throws Exception {
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(null);

        processor.process(exchange);

        verify(message).setHeader(eq(CdrBinaryPath), anyString());
    }
}
