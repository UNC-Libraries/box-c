package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public abstract class AbstractIndexDocumentFilter implements IndexDocumentFilter {
	protected TripleStoreQueryService tsqs;
	protected DocumentIndexingPackageFactory dipFactory;
	
	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(this.getClass().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		inStreamReader.close();
		inStream.close();
		return fileData.toString();
	}
	
	protected DocumentIndexingPackage retrieveParentDIP(DocumentIndexingPackage dip) {
		IndexDocumentBean idb = dip.getDocument();
		PID parentPID = null;
		// Try to get the parent pid from the items ancestors if available.
		if (idb.getAncestorPath() != null && idb.getAncestorPath().size() > 0) {
			String ancestor = idb.getAncestorPath().get(idb.getAncestorPath().size() - 1);
			int index = ancestor.indexOf(',');
			ancestor = ancestor.substring(index + 1);
			index = ancestor.indexOf(',');
			ancestor = ancestor.substring(0, index);
			parentPID = new PID(ancestor);
		} else {
			parentPID = new PID(tsqs.fetchFirstBySubjectAndPredicate(dip.getPid(), ContentModelHelper.Relationship.contains.toString()));
		}
		DocumentIndexingPackage parentDIP = dipFactory.createDocumentIndexingPackage(parentPID);
		dip.setParentDocument(parentDIP);
		return parentDIP;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}

	public void setDocumentIndexingPackageFactory(DocumentIndexingPackageFactory dipFactory) {
		this.dipFactory = dipFactory;
	}
}
