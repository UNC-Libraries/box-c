package edu.unc.lib.bag;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XLINK_NS;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.xml.NamespaceConstants;

public class METSGraphExtractor {
	private static Map<String, URI> containerTypes = new HashMap<String, URI>();
	static {
		containerTypes.put("Folder", ContentModelHelper.Model.CONTAINER.getURI());
		containerTypes.put("Collection", ContentModelHelper.Model.COLLECTION.getURI());
		containerTypes.put("Aggregate Work", ContentModelHelper.Model.AGGREGATE_WORK.getURI());
		containerTypes.put("SWORD Object", ContentModelHelper.Model.AGGREGATE_WORK.getURI());
	}
	
	private Model m;
	private Document mets;
	private PID depositId;
	
	public METSGraphExtractor(Model m, Document mets, PID depositId) {
		this.m=m;
		this.mets=mets;
		this.depositId=depositId;
	}
	
	public Model extractModel() {
		addStructLinkProperties();
		addContainerTriples();
		return m;
	}

	public void addStructLinkProperties() {
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
						.createResource(getPIDURI(getDiv(from)));
				Property role = m.createProperty(CDRProperty.sortOrder.getURI()
						.toString());
				Resource alpha = m
						.createResource("http://cdr.unc.edu/definitions/1.0/base-model.xml#alphabetical");
				m.add(fromR, role, alpha);
			} else {
				Resource fromR = m
						.createResource(getPIDURI(getDiv(from)));
				Resource toR = m.createResource(getPIDURI(getDiv(to)));
				Property role = m.createProperty(arcrole);
				m.add(fromR, role, toR);
			}
		}
	}

	public void addContainerTriples() {		
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

	public Iterator<Element> getDivs() {
		@SuppressWarnings("unchecked")
		Iterator<Element> divs = (Iterator<Element>) mets.getRootElement()
				.getChild("structMap", METS_NS).getDescendants(new MetsDivFilter());
		return divs;
	}

	public static String getPIDURI(Element div) {
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

	public Element getDiv(String id) {
		Element result = null;
		Iterator<Element> divs = getDivs();
		while (divs.hasNext()) {
			Element div = divs.next();
			String idtest = div.getAttributeValue("ID");
			if (id.equals(idtest))
				return div;
		}
		return result;
	}

	public static class MetsDivFilter implements Filter {
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
}
