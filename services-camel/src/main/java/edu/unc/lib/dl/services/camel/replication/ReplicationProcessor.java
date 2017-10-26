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
package edu.unc.lib.dl.services.camel.replication;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryUri;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

/**
 * Replicates binary files ingested into fedora to a series of one or more remote storage locations.
 * It checksums the remote file to make sure it's the same file that was originally ingested.
 *
 * @author lfarrell
 *
 */
public class ReplicationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ReplicationProcessor.class);

    private final String[] replicationLocations;
    private final int maxRetries;
    private final long retryDelay;

    public ReplicationProcessor(String replicationLocations,
            int maxRetries, long retryDelay) {
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

        // Pass mimetype and checksum headers along to enhancements
        exchange.getOut().setHeaders(in.getHeaders());
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

    private String createRemoteSubDirectory(String baseDirectory, String binaryChecksum) throws IOException {
        String replicationPath = createFilePath(baseDirectory, binaryChecksum);
        Path fullPath = Paths.get(replicationPath);

        if (!Files.exists(fullPath)) {
            Files.createDirectories(fullPath);
        }

        return replicationPath;
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

        try {
            for (String location : replicationLocations) {
                String destinationDirectory = createRemoteSubDirectory(location, originalFileChecksum);

                destinationFullPath = destinationDirectory + "/" + originalFileChecksum;
                String[] cmd = new String[]{"rsync", "--update", "--whole-file", "--times", "--verbose",
                        binaryPath, destinationFullPath};
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
            }
        } catch (IOException e) {
            throw new ReplicationException(String.format("Unable to replicate %s", binaryPath), e);
        }
    }
}