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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * A set of functions that are useful for reading, interpreting and writing the container contents XML stream.
 * (MD_CONTENTS)
 *
 * @author count0
 *
 */
public abstract class ContainerContentsHelper {
    private static final Log log = LogFactory.getLog(ContainerContentsHelper.class);

    /**
     * Adds new children to the content index. If there is an order specified
     * for a new child, then it will insert the child at the specified position.
     * Any existing children after the specified position will be shifted if
     * neccessary.
     *
     * @param reordered
     *
     * @param oldContents
     *            bytes for the old XML CONTENTS stream
     * @param topPid
     *            new child pid
     * @param containerOrder
     * @return
     */
    public static Document addChildContentAIPInCustomOrder(Document result, PID containerPID,
            Collection<ContainerPlacement> placements, List<PID> reordered) {
        log.debug("adding child content to MD_CONTENTS XML doc");

        // first build a list of existing pid order in the container
        Element parentDiv = result.getRootElement().getChild("div", JDOMNamespaceUtil.METS_NS);
        List<Element> childDivs = parentDiv.getContent(Filters.element());
        int maxExistingOrder = 5;
        if (childDivs.size() > 0) {
            maxExistingOrder = Integer.parseInt(childDivs.get(childDivs.size() - 1).getAttributeValue("ORDER"));
        }
        ArrayList<PID> order = new ArrayList<PID>(maxExistingOrder + 1);
        try {
            for (Element child : childDivs) {
                int ord = Integer.parseInt(child.getAttributeValue("ORDER"));
                PID pid = new PID(child.getAttributeValue("ID"));
                if (ord >= order.size()) {
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
        for (PID p : order) {
            if (p != null) {
                reordered.add(p);
            }
        }

        // clear out the current children
        parentDiv.removeContent();

        // build a list of things with designated order and things with only sip
        // order
        List<ContainerPlacement> designatedOrder = new ArrayList<ContainerPlacement>();
        List<ContainerPlacement> sipOrdered = new ArrayList<ContainerPlacement>();
        List<ContainerPlacement> unordered = new ArrayList<ContainerPlacement>();
        for (ContainerPlacement place : placements) {
            if (containerPID.equals(place.parentPID)) { // only place those objects that go in this container
                if (place.designatedOrder != null) {
                    designatedOrder.add(place);
                } else if (place.sipOrder != null) {
                    sipOrdered.add(place);
                } else {
                    unordered.add(place);
                }
            }
        }
        // order.ensureCapacity(order.size() + designatedOrder.size() +
        // sipOrdered.size());

        // sort designated ordered stuff by that order
        Comparator<ContainerPlacement> designatedSort = new Comparator<ContainerPlacement>() {
            @Override
            public int compare(ContainerPlacement o1, ContainerPlacement o2) {
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
        for (ContainerPlacement place : designatedOrder) {
            int pos = place.designatedOrder.intValue();
            if (pos >= order.size()) {
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
        Comparator<ContainerPlacement> sipSort = new Comparator<ContainerPlacement>() {
            @Override
            public int compare(ContainerPlacement o1, ContainerPlacement o2) {
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
        for (ContainerPlacement place : sipOrdered) {
            order.add(place.pid);
        }
        // add unordered
        for (ContainerPlacement place : unordered) {
            order.add(place.pid);
        }

        for (ListIterator<PID> li = order.listIterator(); li.hasNext();) {
            int ord = li.nextIndex();
            PID pid = li.next();
            if (pid != null) {
                Element el = new Element("div", parentDiv.getNamespace()).setAttribute("ID", pid.getPid()).setAttribute(
                        "ORDER", String.valueOf(ord));
                parentDiv.addContent(el);
            }
        }
        return result;
    }

    public static Document remove(Document oldXML, PID pid) {
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

    public static Document remove(Document oldXML, Collection<PID> children) {
        Element parentDiv = oldXML.getRootElement().getChild("div", JDOMNamespaceUtil.METS_NS);
        List<Element> childDivs = parentDiv.getChildren();
        Iterator<Element> childIt = childDivs.iterator();

        while (childIt.hasNext()) {
            Element child = childIt.next();
            PID childPID = new PID(child.getAttributeValue("ID"));

            if (children.contains(childPID)) {
                childIt.remove();
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
    public static Document addChildContentListInCustomOrder(Document oldXML, PID container, List<PID> children,
            Collection<PID> reorderedPids) {
        log.debug("HERE incoming children:");
        for (int i = 0; i < children.size(); i++) {
            log.debug(i + " => " + children.get(i));
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
                if (ord >= order.size()) {
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
        for (int i = 0; i < order.size(); i++) {
            log.debug(i + " => " + order.get(i));
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
            if (ord >= order.size()) {
                while (ord > order.size()) { // insert nulls
                    order.add(null);
                }
                order.add(child);
            } else {
                order.add(ord, child);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("HERE order after merge:");
            for (int i = 0; i < order.size(); i++) {
                log.debug(i + " => " + order.get(i));
            }
        }

        // Record changes to order if desired
        if (reorderedPids != null) {
            for (int i = 0; i < originalOrder.length; i++) {
                PID orig = originalOrder[i];
                if (orig != null) {
                    if (!orig.equals(order.get(i))) {
                        reorderedPids.add(orig);
                    }
                }
            }
        }

        for (ListIterator<PID> li = order.listIterator(); li.hasNext();) {
            int ord = li.nextIndex();
            PID pid = li.next();
            if (pid != null) {
                Element el = new Element("div", parentDiv.getNamespace()).setAttribute("ID", pid.getPid()).setAttribute(
                        "ORDER", String.valueOf(ord));
                parentDiv.addContent(el);
            }
        }

        return oldXML;
    }
}
