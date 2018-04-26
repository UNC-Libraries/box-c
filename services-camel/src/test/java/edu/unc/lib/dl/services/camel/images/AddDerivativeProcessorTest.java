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
package edu.unc.lib.dl.services.camel.images;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryId;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;

import edu.unc.lib.dl.fcrepo4.PIDs;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.exec.ExecResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;

public class AddDerivativeProcessorTest {

    private final String fileName = "small_thumb";
    private final String fileExtension = "PNG";
    private String pathId;
    private File file;
    private File mvFile;

    private AddDerivativeProcessor processor;

    private String derivPath;

    private File finalDir;

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String RESC_ID = FEDORA_BASE + "content/de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder moveDir = new TemporaryFolder();

    @Mock
    private ExecResult result;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        finalDir = moveDir.getRoot();

        processor = new AddDerivativeProcessor(fileExtension, finalDir.getAbsolutePath());

        pathId = PIDs.get(RESC_ID).getId();

        file = tmpDir.newFile(pathId);

        when(exchange.getIn()).thenReturn(message);

        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn("http://fedora/test/original_file");

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/png");

        try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
            writeFile.write("fake image");
        }

        when(message.getHeader(eq(CdrBinaryId)))
                .thenReturn(fileName);

        when(result.getStdout()).thenReturn(new ByteArrayInputStream(
                file.getAbsolutePath().getBytes()
        ));
        when(message.getBody()).thenReturn(result);
    }

    @Test
    public void createEnhancementTest() throws Exception {
        mvFile = new File(finalDir.getAbsolutePath() + "/"+ fileName + ".PNG");
        processor.process(exchange);

        assertTrue(mvFile.exists());
    }
}
