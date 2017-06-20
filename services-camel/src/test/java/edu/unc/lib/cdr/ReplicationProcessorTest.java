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
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;

public class ReplicationProcessorTest {
    private ReplicationProcessor processor; 
    private final String replicationLocations = "/tmp";
    private final String badReplicationLocations = "/no_replicate";
    private final String fileName = "replication_text.txt";
    private final String testText = "Test text, see if it can be replicated.";
    private int maxRetries = 3; 
    private long retryDelay = 10;
    private File file;
    private String filePath;
    private String localChecksum;
    private InputStream binaryStream;
    @Mock
    private BinaryObject binary;

    @Mock
    private BinaryObject binaryFcrepo;

    @Mock
    private Repository repository;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);

        processor = new ReplicationProcessor(repository, replicationLocations, maxRetries, retryDelay);

        file = File.createTempFile(fileName, "txt");
        file.deleteOnExit();

        binaryStream = new ByteArrayInputStream(testText.getBytes());

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getOut()).thenReturn(message);

        PIDs.setRepository(repository);
        when(repository.getBaseUri()).thenReturn("http://fedora");

        when(repository.getBinary(any(PID.class))).thenReturn(binary);

        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn("http://fedora/test/replicate");

        when(message.getHeader(eq(CdrBinaryUri)))
                .thenReturn("http://fedora/test/uuid:1234");

        when(message.getHeader(eq(CdrBinaryMimeType)))
                .thenReturn("text/plain");

        try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
            writeFile.write(testText);
        }

        filePath = file.getAbsolutePath().toString();

        localChecksum = DigestUtils.sha1Hex(new FileInputStream(filePath));

        when(message.getHeader(eq(CdrBinaryChecksum)))
        .thenReturn(localChecksum);

        when(binary.getBinaryStream())
                .thenReturn(binaryStream);
    }

    @Test
    public void replicateFile() throws Exception {
        when(message.getHeader(eq(CdrBinaryPath)))
        .thenReturn(filePath);

        processor.process(exchange);

        String remoteChecksum = DigestUtils.sha1Hex(binaryStream);
        assertEquals(localChecksum, remoteChecksum);
    }

    @Test
    public void replicateFileFromFedora() throws Exception {
        when(message.getHeader(eq(CdrBinaryPath)))
        .thenReturn(badReplicationLocations);

        processor.process(exchange);

        String remoteChecksum = DigestUtils.sha1Hex(new ByteArrayInputStream(testText.getBytes()));
        assertEquals(localChecksum, remoteChecksum);
    }

    @Test(expected = ReplicationDestinationUnavailableException.class)
    public void replicationLocations() throws Exception {
        processor = new ReplicationProcessor(repository, badReplicationLocations, maxRetries, retryDelay);

        try {
            processor.process(exchange);
        } finally {
            verify(processor).equals(ReplicationDestinationUnavailableException.class);
        }
    }

    @Test(expected = RuntimeException.class)
    public void replicationtFailTest() throws Exception {
        when(binaryStream)
                .thenThrow(new IOException());

        try {
            processor.process(exchange);
        } finally {
            verify(binaryStream, times(maxRetries + 1));
        }
    }
}