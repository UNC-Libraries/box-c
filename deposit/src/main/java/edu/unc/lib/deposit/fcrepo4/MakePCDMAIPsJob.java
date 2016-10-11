package edu.unc.lib.deposit.fcrepo4;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.DepositGraphUtils;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.Rdfs;
import edu.unc.lib.dl.util.DepositConstants;
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
		log.debug("Creating content AIPS for deposit {}", getDepositPID());
		getSubdir(DepositConstants.AIPS_DIR).mkdir();
		
		Model m = getReadOnlyModel();
		
		// TODO Flag for making something private by default.  Perhaps this should move to normalization 
		
		// establish task size
		List<Resource> topDownObjects = DepositGraphUtils.getObjectsBreadthFirst(m, getDepositPID());
		setTotalClicks(topDownObjects.size()); // + (excludeDepositRecord ? 0 : 1));
		
		// Build the deposit record object
		DepositRecord depositRecord = repository.getDepositRecord(getDepositPID());
		//Resource depositRecordResc = createResource(depositRecord);

		for (Resource dObjResc : topDownObjects) {
			// Create model for this objects AIP
			Resource aipObjResc = makeObjectAIP(dObjResc);
			Model aipModel = aipObjResc.getModel();
			
			// Add originalDeposit link
			aipModel.add(aipObjResc, Cdr.originalDeposit, (RDFNode) depositRecord);
			
			// Serialize the AIP model to file
			serializeObjectModel(new PID(aipObjResc.getURI()), aipModel);
			
			// Generate datastream objects
			StmtIterator stmtIt = dObjResc.listProperties(CdrDeposit.hasDatastream);
			while (stmtIt.hasNext()) {
				Statement hasDSStmt = stmtIt.next();
				Resource dDSObj = hasDSStmt.getResource();
				
				Model dsModel = makeDatastreamAIP(dDSObj);
				serializeObjectModel(new PID(dDSObj.getURI()), dsModel);
			}
		}
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
		
//		switch(resourceType) {
//		case Aggregate:
//			Statement primaryStmt = dObjResc.getProperty(Cdr.primaryObject);
//			if (primaryStmt != null) {
//				ContentObject aggrObject = repository.getContentObject(objPid);
//				
//				PID primaryPid = new PID(primaryStmt.getResource().getURI());
//				String primaryPath = aggrObject.getChildPath(primaryPid);
//				//aipModel.createResource
//				aipObjResc.addProperty(Cdr.primaryObject, createResource(primaryPath));
//			}
//			break;
//		default:
//			break;
//		}
		
		// addNamespaceProperties(dObjResc, aipObjResc, Cdr.getURI());
		
		// Add in access control properties
		addAccessControlProperties(dObjResc, aipObjResc, resourceType);
		
		return aipObjResc;
	}
	
	private Model makeDatastreamAIP(Resource dDSObj) {
		Model aipModel = ModelFactory.createDefaultModel();
		
		PID objPid = new PID(dDSObj.getURI());
		Resource aipDSResc = aipModel.createResource(objPid.getURI());
		
		// Add all existing types
		StmtIterator typeIt = dDSObj.listProperties(RDF.type);
		while (typeIt.hasNext()) {
			aipDSResc.addProperty(RDF.type, typeIt.next().getObject());
		}
		// Add pcdm:File type
		aipDSResc.addProperty(RDF.type, PcdmModels.File);
		
		addNamespaceProperties(dDSObj, aipDSResc, Cdr.getURI());
		
		return aipModel;
	}
	
	private void addNamespaceProperties(Resource dObjResc, Resource aipObjResc, String namespace) {
		StmtIterator stmtIt = dObjResc.listProperties();
		while (stmtIt.hasNext()) {
			Statement stmt = stmtIt.next();
			Property property = stmt.getPredicate();
			if (property.getNameSpace().startsWith(namespace)) {
				aipObjResc.addProperty(property, stmt.getObject());
			}
		}
	}
	
	private void addAccessControlProperties(Resource dObjResc, Resource aipObjResc, ResourceType resourceType) {
		// TODO add those access properties
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}
}
