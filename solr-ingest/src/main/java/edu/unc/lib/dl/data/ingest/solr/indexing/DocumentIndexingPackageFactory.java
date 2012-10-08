package edu.unc.lib.dl.data.ingest.solr.indexing;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;

public class DocumentIndexingPackageFactory {
	private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPackageFactory.class);
	
	private ManagementClient managementClient = null;
	
	public DocumentIndexingPackage createDocumentIndexingPackage(PID pid) {
		try {
			Document foxml = managementClient.getObjectXML(pid);
			if (foxml == null)
				throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid());
			return new DocumentIndexingPackage(pid, foxml);
		} catch (FedoraException e) {
			throw new IndexingException("Failed to retrieve FOXML for " + pid.getPid(), e);
		}
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}
}
