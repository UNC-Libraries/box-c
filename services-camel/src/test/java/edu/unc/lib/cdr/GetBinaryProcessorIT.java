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

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryUri;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml","/spring-test/cdr-client-container.xml"})
public class GetBinaryProcessorIT extends CamelTestSupport {

    private static final String BINARY_CONTENT = "binary content";
    private static final String MIMETYPE = "text/plain";


    private GetBinaryProcessor processor;

    @Autowired
    protected Repository repository;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock
    Exchange exchange;
    @Mock
    Message message;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Before
    public void init() throws IOException {
        initMocks(this);

        PIDs.setRepository(repository);

        processor = new GetBinaryProcessor();
        processor.setRepository(repository);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getOut()).thenReturn(message);

        PID pid = repository.mintContentPid();
        WorkObject work = repository.createWorkObject(pid);
        FileObject fileObj = work.addDataFile("file", new ByteArrayInputStream(BINARY_CONTENT.getBytes()),
                MIMETYPE, null);

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
        tmpFolder.delete();

        when(message.getHeader(CdrBinaryPath)).thenReturn(localFile.getAbsolutePath());

        processor.process(exchange);

        verify(message).setHeader(eq(CdrBinaryPath), stringCaptor.capture());

        String filePath = stringCaptor.getValue();
        File binaryFile = new File(filePath);
        assertTrue(binaryFile.exists());
        assertEquals(BINARY_CONTENT, FileUtils.readFileToString(binaryFile));
    }
}
