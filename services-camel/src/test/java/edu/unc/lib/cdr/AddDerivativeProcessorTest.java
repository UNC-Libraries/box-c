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

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.exec.ExecResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmUse;

public class AddDerivativeProcessorTest {

    private final String fileName = "small_thumb";
    private final String slug = "small_thumbnail";
    private final String fileExtension = "PNG";
    private final String mimetype = "image/png";
    private int maxRetries = 3;
    private long retryDelay = 10;
    private File file;

    private AddDerivativeProcessor processor;

    private String extensionlessPath;

    @Mock
    private BinaryObject binary;
    @Mock
    private FileObject parent;
    @Mock
    private ExecResult result;

    @Mock
    private Repository repository;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);
        processor = new AddDerivativeProcessor(repository, slug, fileExtension, mimetype, maxRetries, retryDelay);
        file = File.createTempFile(fileName, ".PNG");
        file.deleteOnExit();
        when(exchange.getIn()).thenReturn(message);
        PIDs.setRepository(repository);
        when(repository.getBaseUri()).thenReturn("http://fedora");

        when(repository.getBinary(any(PID.class))).thenReturn(binary);

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

        when(result.getStdout()).thenReturn(new ByteArrayInputStream(extensionlessPath.getBytes()));
        when(message.getBody()).thenReturn(result);
    }

    @Test
    public void createEnhancementTest() throws Exception {

        when(repository.getBinary(any(PID.class))).thenReturn(binary);
        when(binary.getParent()).thenReturn(parent);
        when(message.getBody()).thenReturn(result);

        processor.process(exchange);

        ArgumentCaptor<InputStream> requestCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(parent).addDerivative(eq(slug), requestCaptor.capture(), eq(extensionlessPath), eq("image/png"), eq(PcdmUse.ThumbnailImage));
    }

    @Test
    public void createEnhancementRetryTest() throws Exception {

        when(binary.getParent())
                .thenThrow(new MockitoException("Can't add derivative"))
                .thenReturn(parent);;

        processor.process(exchange);

        ArgumentCaptor<InputStream> requestCaptor = ArgumentCaptor.forClass(InputStream.class);

        verify(binary, times(2)).getParent();
        verify(parent).addDerivative(eq(slug), requestCaptor.capture(), eq(extensionlessPath), eq("image/png"), eq(PcdmUse.ThumbnailImage));
    }

    @Test(expected = RuntimeException.class)
    public void createEnhancementRetryFailTest() throws Exception {

        when(binary.getParent())
                .thenThrow(new RuntimeException());

        try {
            processor.process(exchange);
        } finally {
            verify(binary, times(maxRetries + 1)).getParent();
        }
    }
}
