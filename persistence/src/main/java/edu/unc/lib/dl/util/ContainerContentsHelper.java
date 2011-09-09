/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.util;

import java.io.IOException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.aip.AIPException;
import edu.unc.lib.dl.ingest.aip.AIPImpl.RepositoryPlacement;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * A set of functions that are useful for reading, interpreting and writing the
 * container contents XML stream. (MD_CONTENTS)
 *
 * @author count0
 *
 */
public class ContainerContentsHelper {
    private static final Log log = LogFactory.getLog(ContainerContentsHelper.class);
    private static Collator stringCompare = Collator.getInstance();
    private static Comparator<String[]> stringSorter = new Comparator<String[]>() {
	@Override
	public int compare(String[] arg0, String[] arg1) {
	    try {
		CollationKey arg0Key = stringCompare.getCollationKey(arg0[1]);
		CollationKey arg1Key = stringCompare.getCollationKey(arg1[1]);
		return arg0Key.compareTo(arg1Key);
	    } catch (IllegalArgumentException e) {
		log.error(arg0[1] + " compared to " + arg1[1], e);
		throw e;
	    }
	}
    };
    private static Pattern nonsortCharacters = Pattern.compile("[^a-zA-Z0-9]+");
    private TripleStoreQueryService tripleStoreQueryService = null;

    public TripleStoreQueryService getTripleStoreQueryService() {
	return tripleStoreQueryService;
    }

    public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
	this.tripleStoreQueryService = tripleStoreQueryService;
    }

    /**
     * Adds new children to the content index. If there is an order specified
     * for a new child, then it will insert the child at the specified position.
     * Any existing children after the specified position will be shifted if
     * neccessary.
     * @param reordered
     *
     * @param oldContents
     *            bytes for the old XML CONTENTS stream
     * @param topPid
     *            new child pid
     * @param containerOrder
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public Document addChildContentAIPInCustomOrder(Document result, String containerPath,
		    ArchivalInformationPackage aip, List<PID> reordered) {
	log.debug("adding child content to MD_CONTENTS XML doc");

	// first build a list of existing pid order in the container
	Element parentDiv = result.getRootElement().getChild("div", JDOMNamespaceUtil.METS_NS);
	List<Element> childDivs = parentDiv.getChildren();
	int maxExistingOrder = 5;
	if (childDivs.size() > 0) {
	    maxExistingOrder = Integer.parseInt(childDivs.get(childDivs.size() - 1).getAttributeValue("ORDER"));
	}
	ArrayList<PID> order = new ArrayList<PID>(maxExistingOrder+1);
	try {
	    for (Element child : childDivs) {
		int ord = Integer.parseInt(child.getAttributeValue("ORDER"));
		PID pid = new PID(child.getAttributeValue("ID"));
		if(ord >= order.size()) {
		    while (ord > order.size()) { // insert nulls
			order.add(null);
		    }
		    order.add(pid);
		} else {
		    order.add(ord, pid);
		}
	    }
	} catch (NullPointerException e) {
	    throw new IllegalRepositoryStateException("Invalid container contents XML (MD_CONTENTS) on: ", e);
	}

	// FIXME: put all the old children in reordered pile, for now
	for(PID p : order) {
	    if(p != null) {
		reordered.add(p);
	    }
	}

	// clear out the current children
	parentDiv.removeContent();

	// build a list of things with designated order and things with only sip
	// order
	List<RepositoryPlacement> designatedOrder = new ArrayList<RepositoryPlacement>();
	List<RepositoryPlacement> sipOrdered = new ArrayList<RepositoryPlacement>();
	List<RepositoryPlacement> unordered = new ArrayList<RepositoryPlacement>();
	for (PID pid : aip.getTopPIDs()) {
	    RepositoryPlacement place = aip.getTopPIDPlacement(pid);
	    if (containerPath.equals(place.parentPath)) { // only place those
		// objects that go
		// with this container
		if (place.designatedOrder != null) {
		    designatedOrder.add(place);
		} else if(place.sipOrder != null) {
		    sipOrdered.add(place);
		} else {
		    unordered.add(place);
		}
	    }
	}
	// order.ensureCapacity(order.size() + designatedOrder.size() +
	// sipOrdered.size());

	// sort designated ordered stuff by that order
	Comparator<RepositoryPlacement> designatedSort = new Comparator<RepositoryPlacement>() {
	    @Override
	    public int compare(RepositoryPlacement o1, RepositoryPlacement o2) {
		if (o1.designatedOrder > o2.designatedOrder) {
		    return 1;
		} else if (o1.designatedOrder < o2.designatedOrder) {
		    return -1;
		}
		return 0;
	    }
	};
	// ensure capacity for max of designated and existing, plus count of
	// sipOrdered and a buffer for collisions
	int maxDesignatedOrder = 0;
	if (designatedOrder.size() > 0) {
	    Collections.sort(designatedOrder, designatedSort);
	    maxDesignatedOrder = designatedOrder.get(designatedOrder.size() - 1).designatedOrder.intValue();
	}
	int capacityEstimate = Math.max(maxDesignatedOrder, maxExistingOrder) + sipOrdered.size() + 10;
	order.ensureCapacity(capacityEstimate);
	// insert the objects with designated order
	for (RepositoryPlacement place : designatedOrder) {
	    int pos = place.designatedOrder.intValue();
	    if(pos >= order.size()) {
		while (pos > order.size()) { // index out of bounds, insert nulls
		    order.add(null);
		}
		order.add(place.pid);
	    } else {
		order.add(pos, place.pid);
	    }
	}

	// append the objects with sip sibling order
	// sort sip ordered stuff by that order
	Comparator<RepositoryPlacement> sipSort = new Comparator<RepositoryPlacement>() {
	    @Override
	    public int compare(RepositoryPlacement o1, RepositoryPlacement o2) {
		if (o2.sipOrder == null || o1.sipOrder > o2.sipOrder) {
		    return 1;
		} else if (o1.sipOrder == null || o1.sipOrder < o2.sipOrder) {
		    return -1;
		}
		return 0;
	    }
	};
	Collections.sort(sipOrdered, sipSort);
	// add SIP ordered
	for (RepositoryPlacement place : sipOrdered) {
	    order.add(place.pid);
	}
	// add unordered
	for(RepositoryPlacement place : unordered) {
	    order.add(place.pid);
	}

	for(ListIterator<PID> li = order.listIterator(); li.hasNext();) {
	    int ord = li.nextIndex();
	    PID pid = li.next();
	    if (pid != null) {
		Element el = new Element("div", parentDiv.getNamespace()).setAttribute("ID", pid.getPid())
				.setAttribute("ORDER", String.valueOf(ord));
		parentDiv.addContent(el);
	    }
	}
	return result;
    }

    public Document remove(Document oldXML, PID pid) {
	Element parentDiv = oldXML.getRootElement().getChild("div", JDOMNamespaceUtil.METS_NS);
	List<Element> childDivs = parentDiv.getChildren();
	Element remove = null;
	for (Element child : childDivs) {
	    String p = child.getAttributeValue("ID");
	    if (pid.getPid().equals(p)) {
		remove = child;
		break;
	    }
	}
	parentDiv.removeContent(remove);
	return oldXML;
    }

    public Document addChildContentAIPAndAlphabetize(Document result, String containerPath, PID container,
		    ArchivalInformationPackage aip, List<PID> reordered) {
	SortedSet<String[]> sortedRecords = this.getAlphabetizedExistingChildren(container);

	// FIXME: put all the old children in reordered pile, for now
	for(String[] existing : sortedRecords) {
	    if(existing[0] != null) {
		reordered.add(new PID(existing[0]));
	    }
	}

	RDFAwareAIPImpl rdfaip = null;
	if (aip instanceof RDFAwareAIPImpl) {
	    rdfaip = (RDFAwareAIPImpl) aip;
	} else {
	    try {
		rdfaip = new RDFAwareAIPImpl(aip);
	    } catch (AIPException e) {
		throw new Error("That was unexpected", e);
	    }
	}
	for (PID p : aip.getTopPIDs()) {
	    RepositoryPlacement place = aip.getTopPIDPlacement(p);
	    if (containerPath.equals(place.parentPath)) {
		// get label and add this one
		String label = JRDFGraphUtil.getRelatedLiteralObject(rdfaip.getGraph(), p,
				ContentModelHelper.FedoraProperty.label.getURI());
		label = nonsortCharacters.matcher(label).replaceAll("");
		sortedRecords.add(new String[] { p.getPid(), label });
	    }
	}
	this.replaceContent(result, sortedRecords);
	return result;
    }

    private void replaceContent(Document result, SortedSet<String[]> records) {
	Element parentDiv = result.getRootElement().getChild("div", JDOMNamespaceUtil.METS_NS);
	parentDiv.removeContent();
	int ord = 1;
	for (String[] record : records) {
	    Element el = new Element("div", parentDiv.getNamespace()).setAttribute("ID", record[0]).setAttribute(
			    "ORDER", String.valueOf(ord));
	    ord++;
	    parentDiv.addContent(el);
	}
    }

    private SortedSet<String[]> getAlphabetizedExistingChildren(PID pid) {
	List<List<String>> existingLabels = this
			.getTripleStoreQueryService()
			.queryResourceIndex(
					String
							.format(
									"select $pid $label from <%1$s> where <%2$s> <%3$s> $pid and $pid <%4$s> $label;",
									this.getTripleStoreQueryService()
											.getResourceIndexModelUri(),
									pid.getURI(),
									ContentModelHelper.Relationship.contains
											.getURI(),
									ContentModelHelper.FedoraProperty.label
											.getURI()));
	SortedSet<String[]> pid2label = new TreeSet<String[]>(stringSorter);
	for (List<String> record : existingLabels) {
	    String label = nonsortCharacters.matcher(record.get(1)).replaceAll("");
	    pid2label.add(new String[] { new PID(record.get(0)).getPid(), label });
	}
	return pid2label;
    }

    /**
     * First detects the type of Container order required and then process the
     * structure map.
     *
     * @param oldXML
     * @param containerPath
     * @param aip
     * @param reordered
     * @param tripleStoreQueryService
     * @return
     */
    public Document addChildContentAIP(Document oldXML, String containerPath, ArchivalInformationPackage aip, List<PID> reordered) {
	PID container = this.getTripleStoreQueryService().fetchByRepositoryPath(containerPath);
	String sortOrder = this.getSortOrder(container);
	if ("alphabetical".equals(sortOrder)) {
	    return addChildContentAIPAndAlphabetize(oldXML, containerPath, container, aip, reordered);
	} else {
	    return addChildContentAIPInCustomOrder(oldXML, containerPath, aip, reordered);
	}
    }

    private String getSortOrder(PID pid) {
	String result = null;
	List<List<String>> results = this.getTripleStoreQueryService().queryResourceIndex(
			String.format("select $sort from <%1$s> where <%2$s> <%3$s> $sort;", this
					.getTripleStoreQueryService().getResourceIndexModelUri(), pid.getURI(),
					ContentModelHelper.CDRProperty.sortOrder.getURI()));
	if (results.size() > 0 && results.get(0).size() > 0) {
	    result = results.get(0).get(0);
	}
	return result;
    }

    /**
     * First detects the type of Container order required and then process the
     * structure map.
     *
     * @param oldXML
     * @param containerPath
     * @param aip
     * @param tripleStoreQueryService
     * @return
     */
    public Document insertChildContentList(Document oldXML, String containerPath, List<PID> children, Collection<PID> reorderedPids) {
	PID container = this.getTripleStoreQueryService().fetchByRepositoryPath(containerPath);
	String sortOrder = this.getSortOrder(container);
	if ("alphabetical".equals(sortOrder)) {
	    return addChildContentListAndAlphabetize(oldXML, container, children, reorderedPids);
	} else {
	    return addChildContentListInCustomOrder(oldXML, container, children, reorderedPids);
	}
    }

    /**
     * @param oldXML
     * @param container
     * @param children
     * @return
     */
    private Document addChildContentListInCustomOrder(Document oldXML, PID container, List<PID> children, Collection<PID> reorderedPids) {
	log.debug("HERE incoming children:");
	for(int i = 0; i < children.size(); i++) {
	    log.debug(i+" => "+children.get(i));
	}

	// first build a list of existing pid order in the container
	Element parentDiv = oldXML.getRootElement().getChild("div", JDOMNamespaceUtil.METS_NS);
	List<Element> childDivs = parentDiv.getChildren();
	int maxExistingOrder = 5;
	if (childDivs.size() > 0) {
	    maxExistingOrder = Integer.parseInt(childDivs.get(childDivs.size() - 1).getAttributeValue("ORDER"));
	}
	ArrayList<PID> order = new ArrayList<PID>(maxExistingOrder);
	try {
	    for (Element child : childDivs) {
		int ord = Integer.parseInt(child.getAttributeValue("ORDER"));
		PID pid = new PID(child.getAttributeValue("ID"));
		if(ord >= order.size()) {
		    while (ord > order.size()) { // insert nulls
			order.add(null);
		    }
		    order.add(pid);
		} else {
		    order.add(ord, pid);
		}
	    }
	} catch (NullPointerException e) {
	    throw new IllegalRepositoryStateException("Invalid container contents XML (MD_CONTENTS) on: ", e);
	}

	log.debug("HERE order before merge:");
	for(int i = 0; i < order.size(); i++) {
	    log.debug(i+" => "+order.get(i));
	}

	PID[] originalOrder = order.toArray(new PID[0]);

	// clear out the current children
	parentDiv.removeContent();

	int maxIncomingOrder = 0;
	if (children.size() > 0) {
	    maxIncomingOrder = children.size() - 1;
	}
	int capacityEstimate = Math.max(maxIncomingOrder, maxExistingOrder) + 10;
	order.ensureCapacity(capacityEstimate);

	for (ListIterator<PID> foo = children.listIterator(); foo.hasNext();) {
	    int ord = foo.nextIndex();
	    PID child = foo.next();
	    if(ord >= order.size()) {
		while (ord > order.size()) { // insert nulls
		    order.add(null);
		}
		order.add(child);
	    } else {
		order.add(ord, child);
	    }
	}

	log.debug("HERE order after merge:");
	for(int i = 0; i < order.size(); i++) {
	    log.debug(i+" => "+order.get(i));
	}

	for(int i = 0; i < originalOrder.length; i++) {
	    PID orig = originalOrder[i];
	    if(orig != null) {
		if(!orig.equals(order.get(i))) {
		    reorderedPids.add(orig);
		}
	    }
	}

	for (ListIterator<PID> li = order.listIterator(); li.hasNext();) {
	    int ord = li.nextIndex();
	    PID pid = li.next();
	    if (pid != null) {
		Element el = new Element("div", parentDiv.getNamespace()).setAttribute("ID", pid.getPid())
				.setAttribute("ORDER", String.valueOf(ord));
		parentDiv.addContent(el);
	    }
	}

	return oldXML;
    }

    /**
     * @param oldXML
     * @param container
     * @param children
     * @return
     */
    private Document addChildContentListAndAlphabetize(Document oldXML, PID container, List<PID> children, Collection<PID> reorderedPids) {
	SortedSet<String[]> sortedRecords = this.getAlphabetizedExistingChildren(container);
	String[][] originalOrder = sortedRecords.toArray(new String[][]{ new String[1] });
	for (PID child : children) {
	    String label = this.getTripleStoreQueryService().lookupLabel(child);
	    label = nonsortCharacters.matcher(label).replaceAll("");
	    sortedRecords.add(new String[] { child.getPid(), label });
	}

	// find things that moved, everything after first diff
	boolean pastNewStuff = false;
	String[][] mergedStuff = sortedRecords.toArray(new String[][]{new String[1]});
	for(int i = 0; i < originalOrder.length; i++) {
	    if(!pastNewStuff) {
		if(originalOrder[i][0].equals(mergedStuff[i][0])) {
		    continue;
		} else {
		    pastNewStuff = true;
		}
	    }
	    reorderedPids.add(new PID(originalOrder[i][0]));
	}

	this.replaceContent(oldXML, sortedRecords);
	return oldXML;
    }
}
