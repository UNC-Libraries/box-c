package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
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

public class ReplicationProcessor implements Processor {
	private static final Logger log = LoggerFactory.getLogger(ReplicationProcessor.class);
	
	private final String replicationLocations;
	private final int maxRetries;
	private final long retryDelay;
	
	public ReplicationProcessor(String replicationLocations, int maxRetries, long retryDelay) {
		this.replicationLocations = replicationLocations;
		this.maxRetries = maxRetries;
		this.retryDelay = retryDelay;
		
		checkReplicationLocations(splitReplicationLocations(replicationLocations));
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		final Message in = exchange.getIn();
		
		String binaryPath = (String) in.getHeader(CdrBinaryPath);
		String binaryChecksum = (String) in.getHeader(CdrBinaryChecksum);
		String[] replicationPaths = splitReplicationLocations(replicationLocations);
		
		int retryAttempt = 0;
		
		while (true) {
			try {
				replicate(binaryPath, binaryChecksum, replicationPaths);
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
				throw new ReplicationDestinationUnavailableException("Unable to find replication destination {}", replicationPath);
			}
		}
		
		return true;
	}
	
	private String createFilePath(String basePath, String originalFileChecksum) {
		String[] binaryFcrepoChecksumSplit = originalFileChecksum.split(":");
		String checksum = binaryFcrepoChecksumSplit[2];
		
		String[] tokens = Iterables.toArray
				(Splitter.fixedLength(2).split(checksum), 
						String.class);
		
		String remotePath = new StringJoiner("/")
			.add(basePath)
			.add(tokens[0])
			.add(tokens[1])
			.add(tokens[2])
			.add(checksum)
			.toString();
		
		return remotePath;
	}
	
	private String createRemoteSubDirectory(String baseDirectory, String binaryChecksum) {
		String replicationPath = createFilePath(baseDirectory, binaryChecksum);
		
		if (!Files.exists(Paths.get(replicationPath))) {
			new File(replicationPath).mkdirs();
		}

		return replicationPath;
	}
	
	private String getFileChecksum(String filePath) {
		String checksum = null;
		try {
			checksum = DigestUtils.sha1Hex(new FileInputStream(filePath));
		} catch (FileNotFoundException e) {
			log.error("Unable to compute checksum for {}", filePath);
		} catch (IOException e) {
			log.error("Unable to compute checksum for {}", filePath);
		}
		
		return checksum;
	}
	
	private String getFilename(String binaryPath) {
		String[] parts = binaryPath.split("/");
		return parts[parts.length - 1];
	}
	
	private boolean verifyChecksums(String originalFileChecksum, String replicatedFilePath) {
		String remoteChecksum = getFileChecksum(replicatedFilePath);
			
		if (originalFileChecksum.equals(remoteChecksum)) {
			log.info("Local and remote checksums match for {}", replicatedFilePath);
			return true;
		} else {
			throw new ReplicationException("Local and remote checksums did not match {} {}", originalFileChecksum, remoteChecksum);
		}
	}
	
	private void replicate(String binaryPath, String originalFileChecksum, String[] replicationLocations) {
		try {
			for (String location : replicationLocations) {
				String fullPath = Paths.get(createRemoteSubDirectory(location, originalFileChecksum), getFilename(binaryPath)).toString();
				String[] cmd = new String[]{"rsync", "--update", "--whole-file", "--times", "--verbose", binaryPath, fullPath};
				Process returnCode = Runtime.getRuntime().exec(cmd);
				
				if (returnCode.exitValue() != 0) {
					throw new ReplicationException("Error replicating {} to {}", binaryPath, fullPath);
				}
				
				verifyChecksums(originalFileChecksum, fullPath);
			}
		} catch (IOException e) {
			log.error("Unable to find file to replicate {}", e);
		}
	}
}
