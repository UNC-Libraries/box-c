package edu.unc.lib.bag.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XLINK_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_ACL_NS;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.xml.NamespaceConstants;

public class METSGraphExtractor {
	public static interface FilePathFunction {
		public String getPath(String piduri);
	}

	private static Map<String, URI> containerTypes = new HashMap<String, URI>();
	static {
		containerTypes.put("Folder", ContentModelHelper.Model.CONTAINER.getURI());
		containerTypes.put("Collection", ContentModelHelper.Model.COLLECTION.getURI());
		containerTypes.put("Aggregate Work", ContentModelHelper.Model.AGGREGATE_WORK.getURI());
		containerTypes.put("SWORD Object", ContentModelHelper.Model.AGGREGATE_WORK.getURI());
	}
	
	private Document mets;
	private PID depositId;
	
	private Map<String, Element> elementsById = null;
	
	public METSGraphExtractor(Document mets, PID depositId) {
		this.mets=mets;
		this.depositId=depositId;
		initIdMap();
	}
	
	private void initIdMap() {
		elementsById = new HashMap<String, Element>();
		@SuppressWarnings("unchecked")
		Iterator<Element> els = (Iterator<Element>) mets.getRootElement()
				.getDescendants(new Filter() {
					private static final long serialVersionUID = 1L;
					@Override
					public boolean matches(Object obj) {
						return Element.class.isInstance(obj);
					}});
		while(els.hasNext()) {
			Element el = els.next();
			String id = el.getAttributeValue("ID");
			if(id != null) elementsById.put(id, el);
		}
	}

	public void addArrangement(Model m) {
		addStructLinkProperties(m);
		addContainerTriples(m);
	}

	private void addStructLinkProperties(Model m) {
		for (Object e : mets.getRootElement().getChild("structLink", METS_NS)
				.getChildren()) {
			if (!(e instanceof Element))
				continue;
			Element link = (Element) e;
			String from = link.getAttributeValue("from", XLINK_NS);
			String arcrole = link.getAttributeValue("arcrole", XLINK_NS);
			String to = link.getAttributeValue("to", XLINK_NS);
			if ("http://cdr.unc.edu/definitions/1.0/base-model.xml#hasAlphabeticalOrder"
					.equals(arcrole)) {
				Resource fromR = m
						.createResource(getPIDURI(from));
				Property role = m.createProperty(CDRProperty.sortOrder.getURI()
						.toString());
				Resource alpha = m
						.createResource("http://cdr.unc.edu/definitions/1.0/base-model.xml#alphabetical");
				m.add(fromR, role, alpha);
			} else {
				Resource fromR = m
						.createResource(getPIDURI(from));
				Resource toR = m.createResource(getPIDURI(to));
				Property role = m.createProperty(arcrole);
				m.add(fromR, role, toR);
			}
		}
	}
	
	public void saveDescriptions(FilePathFunction f) {
		Iterator<Element> divs = getDivs();
		while(divs.hasNext()) {
			Element div = divs.next();
			String dmdid = div.getAttributeValue("DMDID");
			if(dmdid == null) continue;
			Element dmdSecEl = elementsById.get(dmdid);
			if(dmdid == null) continue;
			Element modsEl = dmdSecEl.getChild("mdWrap", METS_NS).getChild("xmlData", METS_NS)
					.getChild("mods", MODS_V3_NS);
			String pid = getPIDURI(div);
			String path = f.getPath(pid);
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(path);
				Document mods = new Document();
				mods.setRootElement((Element)modsEl.detach());
				new XMLOutputter(Format.getPrettyFormat()).output(mods, fos);
			} catch (IOException e) {
				throw new Error("unexpected exception", e);
			} finally {
				try {
					fos.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	private void addContainerTriples(Model m) {		
		// add deposit-level parent (represented as structMap or bag div)
		Element topContainer = (Element) mets.getRootElement().getChild(
				"structMap", METS_NS);
		Element firstdiv = topContainer.getChild("div", METS_NS);
		if (firstdiv != null
				&& "bag".equals(firstdiv.getAttributeValue("TYPE")
						.toLowerCase())) {
			topContainer = firstdiv;
		}
		Bag top = m.createBag(depositId.getURI());
		@SuppressWarnings("unchecked")
		List<Element> topchildren = (List<Element>) topContainer.getChildren(
				"div", METS_NS);
		for (Element childEl : topchildren) {
			Resource child = m.createResource(getPIDURI(childEl));
			top.add(child);
		}

		Iterator<Element> divs = getDivs();
		while (divs.hasNext()) {
			// FIXME detect Baq or Seq from model
			Element div = divs.next();
			String type = div.getAttributeValue("TYPE");
			if (type != null && "bag".equals(type.toLowerCase())) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Element> children = (List<Element>) div.getChildren("div",
					METS_NS);
			
			if(type == null && children.size() > 0) {
				type = "Folder";
			} else {
				type = "File";
			}

			if (containerTypes.keySet().contains(type)) {
				// add any children
				Bag parent = m.createBag(getPIDURI(div));
				for (Element childEl : children) {
					Resource child = m.createResource(getPIDURI(childEl));
					parent.add(child);
				}
				// set container content model(s)
				Property hasModel = m.createProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
				m.add(parent, hasModel, m.createResource(ContentModelHelper.Model.CONTAINER.getURI().toString()));
				if(!"Folder".equals(type)) {
					m.add(parent, hasModel, m.createResource(containerTypes.get(type).toString()));
				}
			}
			
		}
	}

	private Iterator<Element> getDivs() {
		@SuppressWarnings("unchecked")
		Iterator<Element> divs = (Iterator<Element>) mets.getRootElement()
				.getChild("structMap", METS_NS).getDescendants(new MetsDivFilter());
		return divs;
	}
	
	private Iterator<Element> getFptrs() {
		@SuppressWarnings("unchecked")
		Iterator<Element> fptrs = (Iterator<Element>) mets.getRootElement()
		.getChild("structMap", METS_NS).getDescendants(new MetsFptrFilter());
		return fptrs;
	}

	private static String getPIDURI(Element div) {
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
	
	private String getPIDURI(String id) {
		return getPIDURI(elementsById.get(id));
	}

	private static class MetsDivFilter implements Filter {
		private static final long serialVersionUID = 7056520458827673597L;
		@Override
		public boolean matches(Object obj) {
			if (!Element.class.isInstance(obj))
				return false;
			Element e = (Element) obj;
			return (NamespaceConstants.METS_URI.equals(e
					.getNamespaceURI()) && "div".equals(e.getName()));
		}
	}
	
	private static class MetsFptrFilter implements Filter {
		private static final long serialVersionUID = 1964347591122579007L;

		@Override
		public boolean matches(Object obj) {
			if (!Element.class.isInstance(obj))
				return false;
			Element e = (Element) obj;
			return (NamespaceConstants.METS_URI.equals(e
					.getNamespaceURI()) && "fptr".equals(e.getName()));
		}
	}

	public void addFileAssociations(Model m) {
		// for every fptr
		Iterator<Element> fptrs = getFptrs();
		while(fptrs.hasNext()) {
			Element fptr = fptrs.next();
			String fileId = fptr.getAttributeValue("FILEID");
			Element div = fptr.getParentElement();
			String pid = getPIDURI(div);
			Element fileEl = elementsById.get(fileId);
			String use = fileEl.getAttributeValue("USE"); // may be null
			Element flocat = fileEl.getChild("FLocat", METS_NS);
			String href = flocat.getAttributeValue("href", XLINK_NS);
			Resource object = m.createResource(pid);
			
			// record object source data file
			// only supporting one USE in fileSec, i.e. source data
			Property hasSourceData = m.createProperty(CDRProperty.sourceData.getURI()
					.toString());
			Resource file = m.createResource(); // blank node represents file
			m.add(object, hasSourceData, file); // associate object with file
			
			// record staging location
			Property hasStagingLocation = m.createProperty(ContentModelHelper.CDRProperty.hasStagingLocation.getURI().toString());
			m.add(file, hasStagingLocation, href);
			
			// record mimetype
			if(fileEl.getAttributeValue("MIMETYPE") != null) {
				Property hasMimetype = m.createProperty(CDRProperty.hasSourceMimeType.getURI().toString());
				m.add(file, hasMimetype, fileEl.getAttributeValue("MIMETYPE"));
			}
			
			// record File checksum if supplied, we only support MD5 in Simple profile
			if(fileEl.getAttributeValue("CHECKSUM") != null) {
				Property hasChecksum = m.createProperty(CDRProperty.hasChecksum.getURI().toString());
				m.add(file, hasChecksum, fileEl.getAttributeValue("CHECKSUM"));
			}
			
			// record SIZE (bytes/octets)
			if(fileEl.getAttributeValue("SIZE") != null) {
				Property hasSize = m.createProperty(CDRProperty.hasSourceFileSize.getURI().toString());
				m.add(file, hasSize, fileEl.getAttributeValue("SIZE"));
			}
			
			// record CREATED (iso8601)
			if(fileEl.getAttributeValue("CREATED") != null) {
				Property hasChecksum = m.createProperty(CDRProperty.hasCreatedDate.getURI().toString());
				m.add(file, hasChecksum, fileEl.getAttributeValue("CREATED"), XSDDatatype.XSDdateTime);
			}
				
		}
	}

	public void addAccessControls(Model m) {
		Iterator<Element> divs = getDivs();
		while(divs.hasNext()) {
			Element div = divs.next();
			Resource object = m.createResource(getPIDURI(div));
			if(div.getAttributeValue("ADMID") != null) {
				Element rightsMdEl = elementsById.get(div.getAttributeValue("ADMID"));
				Element aclEl = rightsMdEl.getChild("mdWrap", METS_NS).getChild("xmlData", METS_NS).getChild("accessControl", CDR_ACL_NS);
				
				// set allowIndexing, record "no" when discoverable is false
				String discoverableVal = aclEl.getAttributeValue("discoverable", CDR_ACL_NS);
				if("false".equals(discoverableVal)) {
					Property allowIndexing = m.createProperty(ContentModelHelper.CDRProperty.allowIndexing.getURI().toString());
					m.add(object, allowIndexing, "no");
				}
				
				// isPublished, when "false" record "no"
				String publishedVal = aclEl.getAttributeValue("published", CDR_ACL_NS);
				if("false".equals(publishedVal)) {
					Property published = m.createProperty(ContentModelHelper.CDRProperty.isPublished.getURI().toString());
					m.add(object, published, "no");
				}
				
				// embargo, converts date to dateTime
				String embargoUntilVal = aclEl.getAttributeValue("embargo-until", CDR_ACL_NS);
				if(embargoUntilVal != null) {
					Property embargoUntil = m.createProperty(ContentModelHelper.CDRProperty.embargoUntil.getURI().toString());
					m.add(object, embargoUntil, embargoUntilVal+"T00:00:00", XSDDatatype.XSDdateTime);
				}
				
				// inherit, default is true, literal
				String inheritVal = aclEl.getAttributeValue("inherit", CDR_ACL_NS);
				if("false".equals(inheritVal)) {
					Property inheritPermissions = m.createProperty(ContentModelHelper.CDRProperty.inheritPermissions.getURI().toString());
					m.add(object, inheritPermissions, "false");
				}
				
				// TODO add grants
				
			}
		}
	}
}
