package edu.unc.lib.deposit.fcrepo4;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.DepositGraphUtils;
import edu.unc.lib.dl.fcrepo4.DirectPCDMObject;
import edu.unc.lib.dl.fcrepo4.PCDMObject;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Rdfs;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.ResourceType;

/**
 * Transforms objects from the deposit model into properties to be ingested into the repository,
 * serializing properties for each object into a separate file
 * 
 * @author bbpennel
 *
 */
public class MakePCDMAIPsJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(MakePCDMAIPsJob.class);
	
	private Repository repository;

	@Override
	public void runJob() {
		getSubdir(DepositConstants.AIPS_DIR).mkdir();
		
		Model m = getReadOnlyModel();
		
		Map<String, String> status = getDepositStatus();
		
		// TODO the old pipeline was applying the unpublish flag to every object. 
		// This does not seem ideal for workflow, and is only used currently by deposit collectors.
		// Consider only applying at the top level objects, or requiring normalization to set per object
		// String publishObjectsValue = status.get(DepositField.publishObjects.name());
		// boolean publishObjects = !"false".equals(publishObjectsValue);
		// TODO Apply top level access restrictions that may have come along with the deposit
		
		// establish task size
		List<Resource> topDownObjects = DepositGraphUtils.getObjectsBreadthFirst(m, getDepositPID());
		setTotalClicks(topDownObjects.size()); // + (excludeDepositRecord ? 0 : 1));

		Resource deposit = m.getResource(getDepositPID().getURI());
		
		// Build the deposit record object
		Model depositRecordModel = makeDepositRecordModel(deposit, status);
		serializeObjectModel(getDepositPID(), depositRecordModel);
		String depositRecordUri = repository.getDepositRecordPath(getDepositPID().getUUID());
		Resource depositRecordResc = createResource(depositRecordUri);

		for(Resource dObjResc : topDownObjects) {
			// Create model for this objects AIP
			Resource aipObjResc = makeObjectAIP(dObjResc);
			Model aipModel = aipObjResc.getModel();
			
			// Add originalDeposit link
			aipModel.add(aipObjResc, Cdr.originalDeposit, depositRecordResc);
			
			// Serialize the AIP model to file
			serializeObjectModel(new PID(aipObjResc.getURI()), aipModel);
		}
	}
	
	private Model makeDepositRecordModel(Resource deposit, Map<String, String> status) {
		Model aipModel = ModelFactory.createDefaultModel();
		
		Resource aipObjResc = aipModel.createResource(deposit.getURI());
		
		String filename = status.get(DepositField.fileName);
		String title = "Deposit record" + (filename == null? "" : " for " + filename);
		
		aipModel.add(aipObjResc, Rdfs.type, Cdr.DepositRecord);
		
		aipModel.add(aipObjResc, DcElements.title, title);
		String method = status.get(DepositField.depositMethod.name());
		if (method != null) {
			aipModel.add(aipObjResc, Cdr.depositMethod, method);
		}
		String onBehalfOf = status.get(DepositField.depositorName.name());
		if (onBehalfOf != null) {
			aipModel.add(aipObjResc, Cdr.depositedOnBehalfOf, onBehalfOf);
		}
		String depositPackageType = status.get(DepositField.packagingType.name());
		if (depositPackageType != null) {
			aipModel.add(aipObjResc, Cdr.depositPackageType, depositPackageType);
		}
		String depositPackageProfile = status.get(DepositField.packageProfile.name());
		if (depositPackageProfile != null) {
			aipModel.add(aipObjResc, Cdr.depositPackageProfile, depositPackageProfile);
		}
		
		return aipModel;
	}
	
	private Resource makeObjectAIP(Resource dObjResc) {
		// Model for properties with this object as the subject
		Model aipModel = ModelFactory.createDefaultModel();
		
		PID objPid = new PID(dObjResc.getURI());
		Resource aipObjResc = aipModel.createResource(objPid.getURI());
		
		Statement typeStmt = dObjResc.getProperty(CdrDeposit.objectType);
		Resource objectTypeResc = typeStmt.getResource();
		ResourceType resourceType = ResourceType.getResourceTypeByUri(objectTypeResc.getURI());
		aipModel.add(aipObjResc, Rdfs.type, objectTypeResc);
		
		// Add the label as a dc:title
		Statement labelStmt = dObjResc.getProperty(CdrDeposit.label);
		if (labelStmt != null) {
			aipObjResc.addProperty(DcElements.title, labelStmt.getString());
		}
		
		if (dObjResc.hasProperty(Cdr.invalidTerm)) {
			StmtIterator props = dObjResc.listProperties(Cdr.invalidTerm);

			while (props.hasNext()) {
				Statement prop = props.next();
				aipObjResc.addProperty(Cdr.invalidTerm, prop.getLiteral());
			}
		}
		
		switch(resourceType) {
		case Aggregate:
			Statement primaryStmt = dObjResc.getProperty(Cdr.primaryObject);
			if (primaryStmt != null) {
				PCDMObject pcdm = new DirectPCDMObject(objPid.getUUID());
				PID primaryPid = new PID(primaryStmt.getResource().getURI());
				pcdm.getMemberPath(primaryPid.getUUID());
			}
			break;
		default:
			break;
		}
		
		// Add in access control properties
		addAccessControlProperties(dObjResc, aipObjResc, resourceType);
		
		return aipObjResc;
	}
	
	private void addAccessControlProperties(Resource dObjResc, Resource aipObjResc, ResourceType resourceType) {
		// TODO add those access properties
	}
	
	private void serializeObjectModel(PID pid, Model objModel) {
		File propertiesFile = new File(getSubdir(DepositConstants.AIPS_DIR), pid.getUUID() + ".ttl");
		
		try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
			RDFDataMgr.write(fos, objModel, RDFFormat.TURTLE_PRETTY);
		} catch (IOException e) {
			failJob(e, "Failed to serialize properties for object {} to {}",
					pid, propertiesFile.getAbsolutePath());
		}
	}
}
