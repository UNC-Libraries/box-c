package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

/**
 * Updates the access control of an object and all of its children
 * 
 * @author bbpennel
 *
 */
public class UpdateAccessAction extends UpdateTreeAction {

	public UpdateAccessAction() {
		this.addDocumentMode = false;
	}

	@Override
	public DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) {
		// Create a blank starting entry, the pipeline will provide the necessary data
		DocumentIndexingPackage dip = new DocumentIndexingPackage();
		dip.getDocument().setId(pid.getPid());
		dip.setPid(pid);
		dip.setParentDocument(parent);

		return dip;
	}
}
