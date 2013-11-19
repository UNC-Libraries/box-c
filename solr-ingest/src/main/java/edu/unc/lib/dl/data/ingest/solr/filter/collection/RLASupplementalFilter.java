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
	
	private static final String CONTEXT_1_LABEL = "Context1";
	private static final String CONTEXT_1_FIELD = "rla_context_1_d";
	
	private static final String CONTEXT_2_LABEL = "Context2";
	private static final String CONTEXT_2_FIELD = "rla_context_2_d";
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();
		Element mods = dip.getMods();
		
		idb.setKeyword(new ArrayList<String>());
		if (mods != null) {
			if (idb.getDynamicFields() == null)
				idb.setDynamicFields(new HashMap<String, Object>());
			try {
				extractValues(mods, idb);
			} catch (JDOMException e) {
				log.error("Failed to set RLA fields", e);
			}
		}
	}
	
	private void extractValues(Element mods, IndexDocumentBean idb) throws JDOMException {
		List<?> elements = mods.getChildren();
		
		if (elements == null)
			return;
		
		for (Object elementObject : elements) {
			Element element = (Element) elementObject;
			
			if ("identifier".equals(element.getName())) {
				if (SPECIMEN_LABEL.equalsIgnoreCase(element.getAttributeValue("displayLabel"))) {
					idb.getDynamicFields().put(SPECIMEN_FIELD, element.getTextTrim());
				} else if (CATALOG_LABEL.equalsIgnoreCase(element.getAttributeValue("displayLabel"))) {
					idb.getDynamicFields().put(CATALOG_FIELD, element.getTextTrim());
				}
			} else if ("subject".equals(element.getName())) {
				if (SITE_CODE_LABEL.equalsIgnoreCase(element.getAttributeValue("displayLabel"))) {
					Element geographicEl = element.getChild("geographic", JDOMNamespaceUtil.MODS_V3_NS);
					idb.getDynamicFields().put(SITE_CODE_FIELD, geographicEl.getTextTrim());
				}
			} else if ("note".equals(element.getName())) {
				if (CONTEXT_1_LABEL.equalsIgnoreCase(element.getAttributeValue("displayLabel"))) {
					idb.getDynamicFields().put(CONTEXT_1_FIELD, element.getTextTrim());
				} else if (CONTEXT_2_LABEL.equalsIgnoreCase(element.getAttributeValue("displayLabel"))) {
					idb.getDynamicFields().put(CONTEXT_2_FIELD, element.getTextTrim());
				}
			}
		}
	}
}
