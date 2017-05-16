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

import static java.nio.charset.StandardCharsets.UTF_8;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryUri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.Arrays;
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
	
	public ReplicationProcessor(String replicationLocations, int maxRetries, long retryDelay) {
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
				// Pass mime type and checksum headers along to enhancements
				exchange.getOut().setHeaders(in.getHeaders()); 
				break;
			} catch (Exception e) {
				if (retryAttempt == maxRetries) {
					throw e;
				}

				retryAttempt++;
				TimeUnit.MILLISECONDS.sleep(retryDelay);
			}
		}
	}
	
	private String[] splitReplicationLocations(String replicationLocations) {
		return replicationLocations.split(";");
	}
	
	private boolean checkReplicationLocations(String[] replicationPaths) {
		for (String replicationPath : replicationPaths) {
			if (!Files.exists(Paths.get(replicationPath))) {
				throw new ReplicationDestinationUnavailableException(String.format("Unable to find replication destination %s", replicationPath));
			}
		}
		
		return true;
	}
	
	private String createFilePath(String basePath, String originalFileChecksum) {
		String[] tokens = Iterables.toArray
				(Splitter.fixedLength(2).split( originalFileChecksum), 
						String.class);
		
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
	
	private String dbFilePath(String uri, String BinaryChecksum) {
		String filePath = "/tmp/" + BinaryChecksum;
		InputStream response = null;
		
		try {
			URLConnection connection = new URL(uri).openConnection();
			connection.setRequestProperty("Accept-Charset", UTF_8.toString());
			response = connection.getInputStream();
			
			byte[] buffer = new byte[response.available()];
			response.read(buffer);
		 
			File targetFile = new File(filePath);
			try(OutputStream outStream = new FileOutputStream(targetFile)) {
				outStream.write(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return filePath;
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
			throw new ReplicationException(String.format("Local and remote checksums did not match %s %s", originalFileChecksum, remoteChecksum));
		}
	}
	
	private void replicate(String binaryPath, String originalFileChecksum, String[] replicationLocations, String binaryMimeType, String binaryUri) throws InterruptedException {
		String remoteFile = null;
		String localBinary = null;
		
		try {
			for (String location : replicationLocations) {
				String fullPath = createRemoteSubDirectory(location, originalFileChecksum);
				
				if (Files.exists(Paths.get(binaryPath))) {
					localBinary = binaryPath;
				} else {
					localBinary = dbFilePath(binaryUri, originalFileChecksum);
				}
				
				remoteFile = fullPath + "/" + originalFileChecksum;
				String[] cmd = new String[]{"rsync", "--update", "--whole-file", "--times", "--verbose", localBinary, remoteFile};
				Process runCmd = Runtime.getRuntime().exec(cmd);
				int exitCode = runCmd.waitFor();

				if (exitCode != 0) {
					BufferedReader errInput = new BufferedReader(new InputStreamReader(
							runCmd.getErrorStream()));
					String message = errInput.readLine();
					throw new ReplicationException(String.format("Error replicating %s to %s with error code %d and message %s", binaryPath, remoteFile, exitCode, message));
				}
					
				verifyChecksums(originalFileChecksum, remoteFile);
				
			}
		} catch (IOException e) {
			throw new ReplicationException(String.format("Unable to replicate %s", binaryPath), e);
		}
	}
}
