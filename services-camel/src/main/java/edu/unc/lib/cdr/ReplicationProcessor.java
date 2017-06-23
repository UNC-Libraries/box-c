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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;

/**
 * Replicates binary files ingested into fedora to a series of one or more remote storage locations.
 * It checksums the remote file to make sure it's the same file that was originally ingested.
 * 
 * @author lfarrell
 *
 */
public class ReplicationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ReplicationProcessor.class);

    private final Repository repository;
    private final String[] replicationLocations;
    private final int maxRetries;
    private final long retryDelay;

    public ReplicationProcessor(Repository repository, String replicationLocations, int maxRetries, long retryDelay) {
        this.repository = repository;
        this.replicationLocations = splitReplicationLocations(replicationLocations);
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;

        checkReplicationLocations(this.replicationLocations);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String binaryPath = (String) in.getHeader(CdrBinaryPath);
        String binaryChecksum = (String) in.getHeader(CdrBinaryChecksum);
        String binaryMimetype = (String) in.getHeader(CdrBinaryMimeType);
        String binaryUri = (String) in.getHeader(CdrBinaryUri);

        int retryAttempt = 0;

        while (true) {
            try {
                replicate(binaryPath, binaryChecksum, replicationLocations, binaryMimetype, binaryUri);
                break;
            } catch (Exception e) {
                if (retryAttempt == maxRetries) {
                    throw e;
                }

                retryAttempt++;
                TimeUnit.MILLISECONDS.sleep(retryDelay);
            }
        }

        // Pass mime type and checksum headers along to enhancements
        //exchange.getOut().setHeaders(in.getHeaders());
    }

    private String[] splitReplicationLocations(String replicationLocations) {
        return replicationLocations.split(";");
    }

    private void checkReplicationLocations(String[] replicationPaths)
            throws ReplicationDestinationUnavailableException {
        for (String replicationPath : replicationPaths) {
            if (!Files.exists(Paths.get(replicationPath))) {
                String errorMsg = String.format("Unable to find replication destination %s", replicationPath);
                throw new ReplicationDestinationUnavailableException(errorMsg);
            }
        }
    }

    private String createFilePath(String basePath, String originalFileChecksum) {
        String[] tokens = Iterables.toArray
                (Splitter.fixedLength(2).split( originalFileChecksum), String.class);

        String remotePath = new StringJoiner("/")
            .add(basePath)
            .add(tokens[0])
            .add(tokens[1])
            .add(tokens[2])
            .toString();

        return remotePath;
    }

    private String createRemoteSubDirectory(String baseDirectory, String binaryChecksum) {
        String replicationPath = createFilePath(baseDirectory, binaryChecksum);
        Path fullPath = Paths.get(replicationPath);

        if (!Files.exists(fullPath)) {
            new File(replicationPath).mkdirs();
        }

        return replicationPath;
    }

    private File fcrepoBinaryDownload(String uri, String binaryChecksum) throws IOException {
        File binaryFile = File.createTempFile(binaryChecksum, null);
        InputStream response = null;

        try {
            BinaryObject binary = repository.getBinary(PIDs.get(uri));
            response = binary.getBinaryStream();
            Files.copy(response, Paths.get(binaryFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return binaryFile;
    }

    private String getFileChecksum(String filePath) {
        String checksum = null;
        try {
            checksum = DigestUtils.sha1Hex(new FileInputStream(filePath));
        } catch (IOException e) {
            throw new ReplicationException(String.format("Unable to compute checksum for %s", filePath), e);
        }

        return checksum;
    }

    private void verifyChecksums(String originalFileChecksum, String replicatedFilePath) {
        String remoteChecksum = getFileChecksum(replicatedFilePath);

        if (originalFileChecksum.equals(remoteChecksum)) {
            log.info("Local and remote checksums match for {}", replicatedFilePath);
        } else {
                String errorMsg = String.format("Local and remote checksums did not match %s %s",
                        originalFileChecksum, remoteChecksum);
            throw new ReplicationException(errorMsg);
        }
    }

    private void replicate(String binaryPath, String originalFileChecksum, String[] replicationLocations,
        String binaryMimeType, String binaryUri) throws InterruptedException {

        String destinationFullPath = null;
        String localBinary = null;
        File fcrepoBinary = null;

        /*
         * Checks to see if binary has been persisted to disk.
         * If file is below a certain size it is saved directly into fedora
         * and must be retrieved through a REST request to fedora.
         */
        if (Files.exists(Paths.get(binaryPath))) {
            localBinary = binaryPath;
        } else {
            try {
                fcrepoBinary = fcrepoBinaryDownload(binaryUri, originalFileChecksum);
            } catch (IOException e) {
                throw new ReplicationException(String.format("Unable to replicate %s", binaryPath), e);
            }
            localBinary = fcrepoBinary.getAbsolutePath();
        }

        try {
            for (String location : replicationLocations) {
                String destinationDirectory = createRemoteSubDirectory(location, originalFileChecksum);

                destinationFullPath = destinationDirectory + "/" + originalFileChecksum;
                String[] cmd = new String[]{"rsync", "--update", "--whole-file", "--times", "--verbose",
                        localBinary, destinationFullPath};
                Process runCmd = Runtime.getRuntime().exec(cmd);
                int exitCode = runCmd.waitFor();

                if (exitCode != 0) {
                    BufferedReader errInput = new BufferedReader(new InputStreamReader(
                            runCmd.getErrorStream()));
                    String message = errInput.readLine();
                    String errorMsg = String.format("Error replicating %s to %s with error code %d and message %s",
                            binaryPath, destinationFullPath, exitCode, message);
                    throw new ReplicationException(errorMsg);
                }

                verifyChecksums(originalFileChecksum, destinationFullPath);

                // Delete temp file on disk if the binary came directly from fedora
                if (fcrepoBinary != null) {
                    fcrepoBinary.delete();
                }
            }
        } catch (IOException e) {
            throw new ReplicationException(String.format("Unable to replicate %s", binaryPath), e);
        }
    }
}