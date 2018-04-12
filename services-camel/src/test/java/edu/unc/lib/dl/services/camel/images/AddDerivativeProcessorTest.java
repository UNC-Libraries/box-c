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
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinarySubPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;

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
    private final String derivativeSubPath = "derivative";
    private File file;
    private File mvFile;

    private AddDerivativeProcessor processor;

    private String extensionlessPath;

    private String extensionlessName;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder moveDir = new TemporaryFolder();

    @Mock
    private BinaryObject binary;
    @Mock
    private FileObject parent;
    @Mock
    private ExecResult result;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        processor = new AddDerivativeProcessor(fileExtension, moveDir.getRoot().getAbsolutePath());

        file = tmpDir.newFile(fileName + ".PNG");
        file.deleteOnExit();

        when(exchange.getIn()).thenReturn(message);

        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn("http://fedora/test/original_file");

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/png");

        try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
            writeFile.write("fake image");
        }

        extensionlessPath = file.getAbsolutePath().split("\\.")[0];
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(extensionlessPath);
        when(message.getHeader(eq(CdrBinarySubPath)))
                .thenReturn(derivativeSubPath);

        extensionlessName = new File(extensionlessPath).getName();

        when(result.getStdout()).thenReturn(new ByteArrayInputStream(extensionlessPath.getBytes()));
        when(message.getBody()).thenReturn(result);
    }

    @Test
    public void createEnhancementTest() throws Exception {
        mvFile = moveDir.newFile(fileName + ".PNG");
        mvFile.deleteOnExit();

        processor.process(exchange);

        assertTrue(mvFile.exists());
    }
}
