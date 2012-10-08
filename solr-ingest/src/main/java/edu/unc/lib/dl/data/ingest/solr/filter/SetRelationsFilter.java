package edu.unc.lib.dl.data.ingest.solr.filter;

import org.jdom.Attribute;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class SetRelationsFilter extends AbstractIndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(SetRelationsFilter.class);
	
	private XPath defaultWebDataXPath;
	
	public SetRelationsFilter() {
		try {
			defaultWebDataXPath = XPath.newInstance("cdr:defaultWebData/@rdf:resource");
			defaultWebDataXPath.addNamespace(JDOMNamespaceUtil.CDR_NS);
			defaultWebDataXPath.addNamespace(JDOMNamespaceUtil.RDF_NS);
		} catch (JDOMException e) {
			log.error("Failed to initialize queries", e);
		}
	}
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		if (dip.getFoxml() == null)
			throw new IndexingException("Unable to extract relations, no FOXML document was provided for " + dip.getPid().getPid());
		
		try {
			String defaultWebData = this.getDefaultWebData(dip);
		} catch (JDOMException e) {
			throw new IndexingException("Failed to set relations for " + dip.getPid(), e);
		}
	}
	
	private String getDefaultWebData(DocumentIndexingPackage dip) throws JDOMException {
		Attribute defaultWebData = (Attribute)defaultWebDataXPath.selectNodes(dip.getFoxml());
		// If this object does not have a defaultWebData but its defaultWebObject does, then use that instead.
		if (defaultWebData == null && dip.getDefaultWebObject() != null) {
			defaultWebData = (Attribute)defaultWebDataXPath.selectNodes(dip.getDefaultWebObject().getFoxml());
		}
		if (defaultWebData == null)
			return null;
		return ContentModelHelper.CDRProperty.defaultWebData.getPredicate() + "|" + defaultWebData.getValue();
	}
}
