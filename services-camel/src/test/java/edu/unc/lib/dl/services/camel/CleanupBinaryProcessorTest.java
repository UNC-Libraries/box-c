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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

/**
 *
 * @author bbpennel
 *
 */
public class CleanupBinaryProcessorTest {

    private CleanupBinaryProcessor processor;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    private File localFile;

    @Before
    public void init() throws Exception {
        initMocks(this);

        processor = new CleanupBinaryProcessor();

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getOut()).thenReturn(message);
    }

    @Test
    public void binaryInTempTest() throws Exception {
        localFile = tmpFolder.newFile();
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(localFile.getAbsolutePath());

        processor.process(exchange);

        assertFalse(localFile.exists());
        verify(message).removeHeader(eq(CdrBinaryPath));
    }

    @Test
    public void binaryNotInTempTest() throws Exception {
        localFile = new File("test_file" + System.currentTimeMillis());
        localFile.createNewFile();
        localFile.deleteOnExit();

        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(localFile.getAbsolutePath());

        assertTrue(localFile.exists());

        processor.process(exchange);

        assertTrue(localFile.exists());
        verify(message, never()).removeHeader(anyString());
    }

    @Test
    public void binaryNotSetTest() throws Exception {
        when(message.getHeader(eq(CdrBinaryPath)))
                .thenReturn(null);

        processor.process(exchange);

        verify(message, never()).removeHeader(anyString());
    }
}
