package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_ACL_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XLINK_NS;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
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

public class CDRMETSGraphExtractor {
	private static Map<String, URI> containerTypes = new HashMap<String, URI>();
	static {
		containerTypes.put("Folder",
				ContentModelHelper.Model.CONTAINER.getURI());
		containerTypes.put("Collection",
				ContentModelHelper.Model.COLLECTION.getURI());
		containerTypes.put("Aggregate Work",
				ContentModelHelper.Model.AGGREGATE_WORK.getURI());
		containerTypes.put("SWORD Object",
				ContentModelHelper.Model.AGGREGATE_WORK.getURI());
	}

	private PID depositId = null;
	METSHelper helper = null;
	private Document mets = null;

	public CDRMETSGraphExtractor(Document mets, PID depositId) {
		this.depositId = depositId;
		this.mets = mets;
		this.helper = new METSHelper(mets);
	}

	public void addArrangement(Model m) {
		addStructLinkProperties(m);
		addContainerTriples(m);
	}

	private void addStructLinkProperties(Model m) {
		if (mets.getRootElement().getChild("structLink", METS_NS) == null)
			return;
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
				Resource fromR = m.createResource(helper.getPIDURI(from));
				Property role = m.createProperty(CDRProperty.sortOrder.getURI()
						.toString());
				Resource alpha = m
						.createResource("http://cdr.unc.edu/definitions/1.0/base-model.xml#alphabetical");
				m.add(fromR, role, alpha);
			} else {
				Resource fromR = m.createResource(helper.getPIDURI(from));
				Resource toR = m.createResource(helper.getPIDURI(to));
				Property role = m.createProperty(arcrole);
				m.add(fromR, role, toR);
			}
		}
	}

	public void saveDescriptions(FilePathFunction f) {
		Iterator<Element> divs = helper.getDivs();
		while (divs.hasNext()) {
			Element div = divs.next();
			String dmdid = div.getAttributeValue("DMDID");
			if (dmdid == null)
				continue;
			Element dmdSecEl = helper.getElement(dmdid);
			if (dmdSecEl == null)
				continue;
			Element modsEl = dmdSecEl.getChild("mdWrap", METS_NS)
					.getChild("xmlData", METS_NS).getChild("mods", MODS_V3_NS);
			String pid = METSHelper.getPIDURI(div);
			String path = f.getPath(pid);
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(path);
				Document mods = new Document();
				mods.setRootElement((Element) modsEl.detach());
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
			Resource child = m.createResource(METSHelper.getPIDURI(childEl));
			top.add(child);
		}

		Iterator<Element> divs = helper.getDivs();
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

			if (type == null) {
				if (children.size() > 0) {
					type = "Folder";
				} else {
					type = "File";
				}
			}

			if (containerTypes.keySet().contains(type)) {
				// add any children
				Bag parent = m.createBag(METSHelper.getPIDURI(div));
				for (Element childEl : children) {
					Resource child = m.createResource(METSHelper
							.getPIDURI(childEl));
					parent.add(child);
				}
				// set container content model(s)
				Property hasModel = m
						.createProperty(ContentModelHelper.FedoraProperty.hasModel
								.getURI().toString());
				m.add(parent, hasModel, m
						.createResource(ContentModelHelper.Model.CONTAINER
								.getURI().toString()));
				if (!"Folder".equals(type)) {
					m.add(parent, hasModel, m.createResource(containerTypes
							.get(type).toString()));
				}
			}

		}
	}

	public void addAccessControls(Model m) {
		Iterator<Element> divs = helper.getDivs();
		while (divs.hasNext()) {
			Element div = divs.next();
			Resource object = m.createResource(METSHelper.getPIDURI(div));
			if (div.getAttributeValue("ADMID") != null) {
				Element rightsMdEl = helper.getElement(div
						.getAttributeValue("ADMID"));
				Element aclEl = rightsMdEl.getChild("mdWrap", METS_NS)
						.getChild("xmlData", METS_NS)
						.getChild("accessControl", CDR_ACL_NS);

				// set allowIndexing, record "no" when discoverable is false
				String discoverableVal = aclEl.getAttributeValue(
						"discoverable", CDR_ACL_NS);
				if ("false".equals(discoverableVal)) {
					Property allowIndexing = m
							.createProperty(ContentModelHelper.CDRProperty.allowIndexing
									.getURI().toString());
					m.add(object, allowIndexing, "no");
				}

				// isPublished, when "false" record "no"
				String publishedVal = aclEl.getAttributeValue("published",
						CDR_ACL_NS);
				if ("false".equals(publishedVal)) {
					Property published = m
							.createProperty(ContentModelHelper.CDRProperty.isPublished
									.getURI().toString());
					m.add(object, published, "no");
				}

				// embargo, converts date to dateTime
				String embargoUntilVal = aclEl.getAttributeValue(
						"embargo-until", CDR_ACL_NS);
				if (embargoUntilVal != null) {
					Property embargoUntil = m
							.createProperty(ContentModelHelper.CDRProperty.embargoUntil
									.getURI().toString());
					m.add(object, embargoUntil, embargoUntilVal + "T00:00:00",
							XSDDatatype.XSDdateTime);
				}

				// inherit, default is true, literal
				String inheritVal = aclEl.getAttributeValue("inherit",
						CDR_ACL_NS);
				if ("false".equals(inheritVal)) {
					Property inheritPermissions = m
							.createProperty(ContentModelHelper.CDRProperty.inheritPermissions
									.getURI().toString());
					m.add(object, inheritPermissions, "false");
				}

				// add grants to groups
				for (Object o : aclEl.getChildren("grant", CDR_ACL_NS)) {
					Element grant = (Element) o;
					String role = grant.getAttributeValue("role", CDR_ACL_NS);
					String group = grant.getAttributeValue("group", CDR_ACL_NS);
					String roleURI = NamespaceConstants.CDR_ROLE_NS_URI + role;
					Property roleProp = m.createProperty(roleURI);
					m.add(object, roleProp, group);
				}

			}
		}
	}
}
