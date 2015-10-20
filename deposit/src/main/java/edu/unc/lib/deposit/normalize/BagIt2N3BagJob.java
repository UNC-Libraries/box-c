package edu.unc.lib.deposit.normalize;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.Manifest;

public class BagIt2N3BagJob extends AbstractDepositJob {
	
	private static final Logger LOG = LoggerFactory.getLogger(BagIt2N3BagJob.class);
	
	public BagIt2N3BagJob() {
		super();
	}

	public BagIt2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public void runJob() {
		
		Model model = getWritableModel();
		com.hp.hpl.jena.rdf.model.Bag top = model.createBag(getDepositPID().getURI().toString());
		
		Map<String, String> status = getDepositStatus();
		String sourcePath = status.get(DepositField.sourcePath.name());
		
		BagFactory bagFactory = new BagFactory();
		
		File sourceFile = new File(sourcePath);
		Bag bag = bagFactory.createBag(sourceFile);
		
		Collection<BagFile> payload = bag.getPayload();
		
		for (BagFile file : payload) {
			
			Map<Manifest.Algorithm, String> checksums = bag.getChecksums(file.getFilepath());
			
			Resource fileResource = getFileResource(top, sourcePath, file.getFilepath());
			
			// TODO: add checksum, size, label
			
		}
		
	}
	
	private Resource getFileResource(com.hp.hpl.jena.rdf.model.Bag top, String basepath, String filepath) {
		
		Resource folderResource = getFolderResource(top, basepath, filepath);
		
		UUID uuid = UUID.randomUUID();
		PID pid = new PID("uuid:" + uuid.toString());
		
		Resource fileResource = top.getModel().createResource(pid.getURI());
		
		// TODO: add to folder resource
		
		return fileResource;
		
	}
	
	private Resource getFolderResource(com.hp.hpl.jena.rdf.model.Bag top, String basepath, String filepath) {
		
		// find or create a folder resource for the filepath
		
		return null;
		
	}
	
}
