package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.Bag.Format;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.BagHelper;
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
		
		if (BagHelper.getVersion(new File(sourcePath)) == null) {
			failJob("Can't find BagIt bag", "A BagIt bag could not be found at the source path.");
		}
		
		BagFactory bagFactory = new BagFactory();
		
		File sourceFile = new File(sourcePath);
		Bag bag = bagFactory.createBag(sourceFile);
		
		if (bag.getFormat() != Format.FILESYSTEM) {
			failJob("Unsupported BagIt bag format", "Only filesystem bags are supported.");
		}
		
		Collection<BagFile> payload = bag.getPayload();
		
		for (BagFile bagFile : payload) {
			
			String bagFilepath = bagFile.getFilepath();
			
			if (bagFilepath.indexOf("/") == -1) {
				failJob("Unsupported path separators in BagIt bag payload", "The payload must use \"/\" as the path separator.");
			}
			
			List<String> path = Arrays.asList(bagFilepath.split("/"));
			List<String> folderPath = path.subList(1, path.size() - 1);
			com.hp.hpl.jena.rdf.model.Bag folder = getFolder(top, folderPath);
			
			Resource file = getFile(model, bag, bagFile);
			
			folder.add(file);
			
		}
		
	}
	
	private com.hp.hpl.jena.rdf.model.Bag getFolder(com.hp.hpl.jena.rdf.model.Bag top, List<String> path) {
		
		Model model = top.getModel();
		
		if (path.size() == 0) {
			
			return top;
			
		} else {

			String name = path.get(0);
			
			com.hp.hpl.jena.rdf.model.Bag folder = null;
			
			NodeIterator iterator = top.iterator();
			
			while (iterator.hasNext()) {
				Resource child = (Resource) iterator.next();
				
				if (child.getProperty(dprop(model, DepositRelationship.label)).getString().equals(name)) {
					folder = model.getBag(child);
					break;
				}
			}

			iterator.close();
			
			if (folder == null) {
				UUID uuid = UUID.randomUUID();
				PID pid = new PID("uuid:" + uuid.toString());
				folder = model.createBag(pid.getURI());
				
				model.add(folder, dprop(model, DepositRelationship.label), name);
				model.add(folder, fprop(model, FedoraProperty.hasModel), ContentModelHelper.Model.CONTAINER.toString());
				
				top.add(folder);
			}
			
			return getFolder(folder, path.subList(1, path.size()));
			
		}
		
	}
	
	private Resource getFile(Model model, Bag bag, BagFile bagFile) {
		
		UUID uuid = UUID.randomUUID();
		PID pid = new PID("uuid:" + uuid.toString());
		Resource file = model.createResource(pid.getURI());

		model.add(file, fprop(model, FedoraProperty.hasModel), ContentModelHelper.Model.SIMPLE.toString());
		
		List<String> path = Arrays.asList(bagFile.getFilepath().split("/"));
		model.add(file, dprop(model, DepositRelationship.label), path.get(path.size() - 1));
		
		Map<Manifest.Algorithm, String> checksums = bag.getChecksums(bagFile.getFilepath());
		
		if (checksums.containsKey(Manifest.Algorithm.MD5)) {
			model.add(file, dprop(model, DepositRelationship.md5sum), checksums.get(Manifest.Algorithm.MD5));
		} else {
			failJob("MD5 checksum not provided", "An MD5 checksum was not provided for the file: " + bagFile.getFilepath());
		}
		
		return file;
		
	}
	
}
