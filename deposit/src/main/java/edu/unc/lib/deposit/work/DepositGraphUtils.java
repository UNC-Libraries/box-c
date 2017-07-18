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
package edu.unc.lib.deposit.work;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author count0
 *
 */
public class DepositGraphUtils {
    private DepositGraphUtils() {
    }

    private static void addChildren(Resource c, List<Resource> result) {
        NodeIterator iterator = null;
        if (c.hasProperty(RDF.type, RDF.Bag)) {
            iterator = c.getModel().getBag(c).iterator();
        } else if (c.hasProperty(RDF.type, RDF.Seq)) {
            iterator = c.getModel().getSeq(c).iterator();
        } else {
            return;
        }

        List<Resource> containers = new ArrayList<Resource>();
        try {
            while (iterator.hasNext()) {
                Resource n = (Resource) iterator.next();
                result.add(n);
                if (n.hasProperty(RDF.type, RDF.Bag)
                        || n.hasProperty(RDF.type, RDF.Seq)) {
                    containers.add(n);
                }
            }
        } finally {
            iterator.close();
        }
        for (Resource r : containers) {
            addChildren(r, result);
        }
    }

    public static List<Resource> getObjectsBreadthFirst(Model m, PID depositPID) {
        List<Resource> result = new ArrayList<Resource>();
        Resource top = m.getResource(depositPID.getURI());
        addChildren(top, result);
        return result;
    }

    /**
     *
     * Walk the children of the given bag in depth first order, storing children
     * in the given pids collection. Duplicate PIDs are ignored.
     *
     * @param bag bag to retrieve children from
     * @param pids collection to store bag children into, in depth first ordering
     * @param recursive if false, then only the first tier of children will be
     *        retrieved
     */
    public static void walkChildrenDepthFirst(Bag bag, Collection<String> pids,
            boolean recursive) {
        NodeIterator childIt = bag.iterator();
        try {
            while (childIt.hasNext()) {
                Resource childResource = (Resource) childIt.next();

                if (!pids.contains(childResource.getURI())) {
                    pids.add(childResource.getURI());
                }

                if (recursive) {
                    Bag childBag = childResource.getModel().getBag(childResource);
                    walkChildrenDepthFirst(childBag, pids, recursive);
                }
            }
        } finally {
            childIt.close();
        }
    }

    /**
     * Walk the children in depth first order, returning each as a resource.
     * Duplicate resources are ignored.
     *
     * @param bag
     * @param children
     */
    public static void walkObjectsDepthFirst(Bag bag, Collection<Resource> children) {
        NodeIterator childIt = bag.iterator();
        try {
            while (childIt.hasNext()) {
                Resource childResource = (Resource) childIt.next();

                if (!children.contains(childResource)) {
                    children.add(childResource);
                }

                Bag childBag = childResource.getModel().getBag(childResource);
                walkObjectsDepthFirst(childBag, children);
            }
        } finally {
            childIt.close();
        }
    }
}
