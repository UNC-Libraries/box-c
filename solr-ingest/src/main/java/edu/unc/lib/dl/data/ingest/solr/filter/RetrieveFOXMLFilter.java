package edu.unc.lib.dl.data.ingest.solr.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.FedoraException;

public class RetrieveFOXMLFilter implements IndexDocumentFilter {
	protected static final Logger log = LoggerFactory.getLogger(RetrieveFOXMLFilter.class);
	
	private edu.unc.lib.dl.fedora.ManagementClient managementClient = null;
	
	public RetrieveFOXMLFilter() {
	}
	
	public void setManagementClient(edu.unc.lib.dl.fedora.ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		try {
			dip.setFoxml(managementClient.getObjectXML(dip.getPid()));
		} catch (FedoraException e) {
			throw new IndexingException("Failed to retrieve FOXML for " + dip.getPid(), e);
		}
	}
}
