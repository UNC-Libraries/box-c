package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.xml.NamespaceConstants;

public class SetDatastreamsFilter extends AbstractIndexDocumentFilter {
	protected static final Logger log = LoggerFactory.getLogger(SetDatastreamsFilter.class);
	
	private XPath datastreamVersionXPath;
	
	public SetDatastreamsFilter() {
		try {
			datastreamVersionXPath = XPath.newInstance("/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion");
			datastreamVersionXPath.addNamespace(Namespace.getNamespace("foxml", NamespaceConstants.FOXML_URI));
		} catch (JDOMException e) {
			log.error("Failed to initialize queries", e);
		}
	}
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		Document foxml = dip.getFoxml();
		if (foxml == null)
			throw new IndexingException("FOXML was not found or set for " + dip.getPid());
		
		long totalSize = 0;
		try {
			List<?> datastreamVersions = this.datastreamVersionXPath.selectNodes(foxml);
			for (Object datastreamVersionObj: datastreamVersions) {
				Element datastreamVersion = (Element) datastreamVersionObj;
				String dsID = datastreamVersion.getAttributeValue("ID");
				int index = dsID.lastIndexOf(".");
				//String 
			}
		} catch (JDOMException e) {
			throw new IndexingException("Could not extract datastream versions for " + dip.getPid());
		}
	}
	
	private static class Datastream {
		int lowestVersion;
		String dsID;
		long filesize;
		String mimetype;
		String category;
		String ownerPID;
		
	}

}
