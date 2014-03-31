package edu.unc.lib.deposit.work;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 *
 * @author bbpennel
 * @date Mar 24, 2014
 */
public abstract class GraphUtils {

	/**
	 * Walk the children of the given bag in depth first order, storing children in the given pids collection
	 *
	 * @param bag
	 *           bag to retrieve children from
	 * @param pids
	 *           collection to store bag children into, in depth first ordering
	 * @param recursive
	 *           if false, then only the first tier of children will be retrieved
	 */
	public static void walkChildrenDepthFirst(Bag bag, Collection<String> pids, boolean recursive) {

		NodeIterator childIt = bag.iterator();
		while (childIt.hasNext()) {
			Resource childResource = (Resource) childIt.next();

			pids.add(childResource.getURI());

			if (recursive) {
				Bag childBag = childResource.getModel().getBag(childResource);
				walkChildrenDepthFirst(childBag, pids, recursive);
			}
		}
	}

}
