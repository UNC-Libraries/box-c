package edu.unc.lib.dl.data.ingest.solr.filter.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class RLASupplementalFilter extends CollectionSupplementalInformationFilter {
	private static final Logger log = LoggerFactory.getLogger(RLASupplementalFilter.class);
	
	private static final String SPECIMEN_LABEL = "RLA Specimen Number";
	private static final String SPECIMEN_FIELD = "rla_specimen_number_d";
	
	private static final String CATALOG_LABEL = "RLA Catalog Number";
	private static final String CATALOG_FIELD = "rla_catalog_number_d";
	
	private static final String SITE_CODE_LABEL = "RLA Site Code";
	private static final String SITE_CODE_FIELD = "rla_site_code_d";
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();
		Element mods = dip.getMods();
		
		idb.setKeyword(new ArrayList<String>());
		if (mods != null) {
			if (idb.getDynamicFields() == null)
				idb.setDynamicFields(new HashMap<String, Object>());
			try {
				extractIdentifiers(mods, idb);
				extractSite(mods, idb);
			} catch (JDOMException e) {
				log.error("Failed to set RLA fields", e);
			}
		}
	}
	
	private void extractIdentifiers(Element mods, IndexDocumentBean idb) throws JDOMException {
		List<?> elements = mods.getChildren("identifier", JDOMNamespaceUtil.MODS_V3_NS);
		if (elements == null)
			return;
		
		for (Object element : elements) {
			Element identifierEl = (Element) element;
			if (SPECIMEN_LABEL.equalsIgnoreCase(identifierEl.getAttributeValue("displayLabel"))) {
				idb.getDynamicFields().put(SPECIMEN_FIELD, identifierEl.getTextTrim());
			} else if (CATALOG_LABEL.equalsIgnoreCase(identifierEl.getAttributeValue("displayLabel"))) {
				idb.getDynamicFields().put(CATALOG_FIELD, identifierEl.getTextTrim());
			}
		}
	}
	
	private void extractSite(Element mods, IndexDocumentBean idb) throws JDOMException {
		List<?> elements = mods.getChildren("subject", JDOMNamespaceUtil.MODS_V3_NS);
		if (elements == null)
			return;
		
		for (Object element : elements) {
			Element subjectEl = (Element) element;
			if (SITE_CODE_LABEL.equalsIgnoreCase(subjectEl.getAttributeValue("displayLabel"))) {
				Element geographicEl = subjectEl.getChild("geographic", JDOMNamespaceUtil.MODS_V3_NS);
				idb.getDynamicFields().put(SITE_CODE_FIELD, geographicEl.getTextTrim());
				return;
			}
		}
	}
}
