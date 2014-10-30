package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XLINK_NS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class METSHelper {

	protected Document mets;

	public METSHelper(Document mets) {
		this.mets = mets;
		initIdMap();
	}

	protected Map<String, Element> elementsById = null;

	protected static String getPIDURI(Element div) {
		String result = null;
		try {
			String cids = div.getAttributeValue("CONTENTIDS");
			for (String s : cids.split("\\s")) {
				if (s.startsWith("info:fedora/")) {
					result = s;
					break;
				}
			}
		} catch (Exception ignored) {
		}
		return result;
	}
	
	protected static String getOriginalURI(Element div) {
		String result = null;
		try {
			String cids = div.getAttributeValue("CONTENTIDS");
			for (String s : cids.split("\\s")) {
				if (!s.startsWith("info:fedora/")) {
					result = s;
					break;
				}
			}
		} catch (Exception ignored) {
		}
		return result;
	}

	protected void initIdMap() {
		elementsById = new HashMap<String, Element>();
		Iterator<Element> els = (Iterator<Element>) mets.getRootElement()
				.getDescendants(new ElementFilter());
		while (els.hasNext()) {
			Element el = els.next();
			String id = el.getAttributeValue("ID");
			if (id != null)
				elementsById.put(id, el);
		}
	}
	
	protected Element getElement(String id) {
		return elementsById.get(id);
	}

	protected Iterator<Element> getDivs() {
		// add deposit-level parent (represented as structMap or bag div)
		Element topContainer = (Element) mets.getRootElement().getChild(
				"structMap", METS_NS);
		Element firstdiv = topContainer.getChild("div", METS_NS);
		if (firstdiv != null
				&& "Bag".equals(firstdiv.getAttributeValue("TYPE"))) {
			topContainer = firstdiv;
		}
		Iterator<Element> divs = (Iterator<Element>) topContainer
				.getDescendants(new ElementFilter("div", JDOMNamespaceUtil.METS_NS));
		return divs;
	}

	protected Iterator<Element> getFptrs() {
		Iterator<Element> fptrs = (Iterator<Element>) mets.getRootElement()
				.getChild("structMap", METS_NS)
				.getDescendants(new ElementFilter("fptr", JDOMNamespaceUtil.METS_NS));
		return fptrs;
	}

	protected String getPIDURI(String id) {
		return getPIDURI(elementsById.get(id));
	}

	public void addFileAssociations(Model m, boolean prependDataPath) {
		// for every fptr
		Iterator<Element> fptrs = getFptrs();
		while(fptrs.hasNext()) {
			Element fptr = fptrs.next();
			String fileId = fptr.getAttributeValue("FILEID");
			Element div = fptr.getParentElement();
			String pid = METSHelper.getPIDURI(div);
			Element fileEl = getElement(fileId);
			@SuppressWarnings("unused") // only 1 USE supported
			String use = fileEl.getAttributeValue("USE"); // may be null
			Element flocat = fileEl.getChild("FLocat", METS_NS);
			String href = flocat.getAttributeValue("href", XLINK_NS);
			if(prependDataPath && !href.contains(":")) {
				href = "data/"+href;
			}
			Resource object = m.createResource(pid);
			
			// record object source data file
			// only supporting one USE in fileSec, i.e. source data
			
			// record file location
			Property fileLocation = m.createProperty(ContentModelHelper.DepositRelationship.stagingLocation.toString());
			m.add(object, fileLocation, href);
			
			// record mimetype
			if(fileEl.getAttributeValue("MIMETYPE") != null) {
				Property hasMimetype = m.createProperty(ContentModelHelper.DepositRelationship.mimetype.toString());
				m.add(object, hasMimetype, fileEl.getAttributeValue("MIMETYPE"));
			}
			
			// record File checksum if supplied, we only support MD5 in Simple profile
			if(fileEl.getAttributeValue("CHECKSUM") != null) {
				Property hasChecksum = m.createProperty(ContentModelHelper.DepositRelationship.md5sum.toString());
				m.add(object, hasChecksum, fileEl.getAttributeValue("CHECKSUM"));
			}
			
			// record SIZE (bytes/octets)
			if(fileEl.getAttributeValue("SIZE") != null) {
				Property hasSize = m.createProperty(ContentModelHelper.DepositRelationship.size.toString());
				m.add(object, hasSize, fileEl.getAttributeValue("SIZE"));
			}
			
			// record CREATED (iso8601)
			if(fileEl.getAttributeValue("CREATED") != null) {
				Property hasCreated = m.createProperty(ContentModelHelper.DepositRelationship.createTime.toString());
				m.add(object, hasCreated, fileEl.getAttributeValue("CREATED"), XSDDatatype.XSDdateTime);
			}
				
		}
	}

	public String getPIDURIForDIVID(String ref) {
		if(ref.startsWith("#")) {
			ref = ref.substring(1);
		}
		Iterator<Element> i = getDivs();
		while(i.hasNext()) {
			Element div = i.next();
			if(ref.equals(div.getAttributeValue("ID"))) {
				return getPIDURI(div);
			}
		}
		return null;
	}

}