package edu.unc.lib.deposit.work;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;

public class DepositGraphUtils {

	public DepositGraphUtils() {
		// TODO Auto-generated constructor stub
	}

	public static Property dprop(Model m, DepositRelationship r) {
		return m.getProperty(r.getURI().toString());
	}

	public static Property cdrprop(Model m, CDRProperty r) {
		return m.getProperty(r.getURI().toString());
	}

	public static Property cdrprop(Model m, Relationship r) {
		return m.getProperty(r.getURI().toString());
	}

	public static Property fprop(Model m, FedoraProperty r) {
		return m.getProperty(r.getURI().toString());
	}

	public static Resource cmodel(Model m,
			edu.unc.lib.dl.util.ContentModelHelper.Model model) {
		return m.getResource(model.getURI().toString());
	}

	private static void addChildren(Resource c, List<Resource> result) {
		NodeIterator iterator = null;
		if (c.hasProperty(RDF.type, RDF.Bag)) {
			iterator = c.getModel().getBag(c).iterator();
		} else if (c.hasProperty(RDF.type, RDF.Seq)) {
			iterator = c.getModel().getSeq(c).iterator();
		}
		List<Resource> containers = new ArrayList<Resource>();
		if(iterator != null) {
			while (iterator.hasNext()) {
				Resource n = (Resource) iterator.next();
				result.add(n);
				if (n.hasProperty(RDF.type, RDF.Bag)
						|| n.hasProperty(RDF.type, RDF.Seq)) {
					containers.add(n);
				}
			}
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
		while (childIt.hasNext()) {
			Resource childResource = (Resource) childIt.next();

			if (!children.contains(childResource)) {
				children.add(childResource);
			}

			Bag childBag = childResource.getModel().getBag(childResource);
			walkObjectsDepthFirst(childBag, children);
		}
	}
}
