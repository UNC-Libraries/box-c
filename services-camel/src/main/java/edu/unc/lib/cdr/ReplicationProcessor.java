package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.dl.rdf.Ebucore.hasMimeType;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.rdf.Fcrepo4Repository;

public class ReplicationProcessor implements Processor {
	private static final Logger log = LoggerFactory.getLogger(AddDerivativeProcessor.class);
	
	private final String replicationLocations;
	private final int maxRetries;
	private final long retryDelay;
	
	public ReplicationProcessor(String replicationLocations, int maxRetries, long retryDelay) {
		this.replicationLocations = replicationLocations;
		this.maxRetries = maxRetries;
		this.retryDelay = retryDelay;
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		String[] replicationPaths = replicationLocations.split(";");
		boolean pathsExist = checkReplicationLocations(replicationPaths);
		
		if (!pathsExist) {
			throw new Exception();
		}

		final Message in = exchange.getIn();
		
		String binaryUri = (String) in.getHeader(FCREPO_URI);
		String binaryPath = (String) in.getHeader(CdrBinaryPath);
		String binaryChecksum = (String) in.getHeader(CdrBinaryChecksum);
	/*	final Model model = createDefaultModel();

		Model values = model.read(in.getBody(InputStream.class), null, "Turtle");
		ResIterator resources = values.listResourcesWithProperty(RDF.type, Fcrepo4Repository.Binary);
		
		try {
			if (resources.hasNext()) {
				Resource resource = resources.next();
				String binaryFilePath = resource.getProperty(hasMimeType).getObject().toString();
				binaryFilePaths.add(binaryFilePath);
			}
		} finally {
			resources.close();
		} */
		
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
	
	private boolean checkReplicationLocations(String[] replicationLocations) {
		for (String path : replicationLocations) {
			if (Files.exists(Paths.get(path))) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean verifyChecksums(String originalFileChecksum, String replicatedFilePath) {
		try {
			String remoteChecksum = DigestUtils.sha1Hex(new FileInputStream(replicatedFilePath));
			
			if (originalFileChecksum.equals(remoteChecksum)) {
				log.info("Local and remote checksums match for {}", replicatedFilePath);
				return true;
			} else {
				throw new Exception("Local and remote checksums did not match");
			}
		} catch (Exception e) {
			log.error("Unable to compute checksum for {}", replicatedFilePath);
		}
		
		return false;
	}
	
	private void replicate(String binaryPath, String originalFileChecksum, String[] replicationLocations) {
		try {
			for (String location : replicationLocations) {
				String[] cmd = new String[]{"rsync", "--update", "--whole-file", "--times", "--verbose", binaryPath, location};
				Runtime.getRuntime().exec(cmd);
				
				verifyChecksums(originalFileChecksum, location);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
