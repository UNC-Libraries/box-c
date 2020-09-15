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
package edu.unc.lib.dl.services.camel.destroyDerivatives;

import static edu.unc.lib.dl.model.DatastreamType.FULLTEXT_EXTRACTION;
import static edu.unc.lib.dl.model.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.dl.model.DatastreamType.THUMBNAIL_LARGE;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPidId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.charset.StandardCharsets;

import edu.unc.lib.dl.test.TestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.PIDs;

public class DestroyDerivativesProcessorTest {
    private DestroyDerivativesProcessor processor;
    private File file;
    private String pathId;
    private String derivativeDirBase;
    private File derivativeTypeBaseDir;
    private File derivativeFinalDir;
    private String derivativeTypeDir;
    private static final String FEDORA_BASE = "http://example.com/rest/";
    private static final String PID_ID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String RESC_ID = FEDORA_BASE + "content/de/75/d8/11/" + PID_ID;

    @Rule
    public TemporaryFolder derivativeDir = new TemporaryFolder();

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        pathId = PIDs.get(RESC_ID).getId();

        when(exchange.getIn()).thenReturn(message);

        when(message.getHeader(eq(CdrBinaryPidId)))
                .thenReturn(PID_ID);

        derivativeDirBase = derivativeDir.getRoot().getAbsolutePath();
    }

    @Test
    public void deleteFulltextTest() throws Exception {
        derivativeTypeDir = FULLTEXT_EXTRACTION.getId();
        derivativeFinalDir = derivativeDir.newFolder(derivativeTypeDir, "de", "75", "d8", "11");
        file = new File(derivativeFinalDir, pathId + ".txt");

        FileUtils.writeStringToFile(file, "my text", StandardCharsets.UTF_8);

        derivativeTypeBaseDir = new File(derivativeDirBase, derivativeTypeDir);
        processor = new DestroyDerivativesProcessor(".txt", derivativeTypeBaseDir.getAbsolutePath());

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("text/plain");

        processor.process(exchange);

        // Deleted file
        assertFalse(file.exists());
        // Deleted parent dirs
        assertFalse(new File(derivativeTypeBaseDir, "de").exists());
        // Didn't delete root derivative type dir
        assertTrue(derivativeTypeBaseDir.exists());
    }

    @Test
    public void deleteThumbnailTest() throws Exception {
        derivativeTypeDir = THUMBNAIL_LARGE.getId();
        derivativeFinalDir = derivativeDir.newFolder(derivativeTypeDir, "de", "75", "d8", "11");
        file = new File(derivativeFinalDir, pathId + ".png");

        FileUtils.writeStringToFile(file, "fake image", StandardCharsets.UTF_8);

        derivativeTypeBaseDir = new File(derivativeDirBase, derivativeTypeDir);
        processor = new DestroyDerivativesProcessor(".png", derivativeTypeBaseDir.getAbsolutePath());

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/png");

        processor.process(exchange);

        // Deleted file
        assertFalse(file.exists());
        // Deleted parent dirs
        assertFalse(new File(derivativeTypeBaseDir, "de").exists());
        // Didn't delete root derivative type dir
        assertTrue(derivativeTypeBaseDir.exists());
    }

    @Test
    public void deleteJp2Test() throws Exception {
        derivativeTypeDir = JP2_ACCESS_COPY.getId();
        derivativeFinalDir = derivativeDir.newFolder(derivativeTypeDir, "de", "75", "d8", "11");
        file = new File(derivativeFinalDir, pathId + ".jp2");

        FileUtils.writeStringToFile(file, "fake jp2", StandardCharsets.UTF_8);

        derivativeTypeBaseDir = new File(derivativeDirBase, derivativeTypeDir);
        processor = new DestroyDerivativesProcessor(".jp2", derivativeTypeBaseDir.getAbsolutePath());

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("image/jp2");

        processor.process(exchange);

        // Deleted file
        assertFalse(file.exists());
        // Deleted parent dirs
        assertFalse(new File(derivativeTypeBaseDir, "de").exists());
        // Didn't delete root derivative type dir
        assertTrue(derivativeTypeBaseDir.exists());
    }
}
