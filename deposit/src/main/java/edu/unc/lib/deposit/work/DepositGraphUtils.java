package edu.unc.lib.deposit.work;

import java.util.ArrayList;
import java.util.List;

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
	
	public static Resource cmodel(Model m, edu.unc.lib.dl.util.ContentModelHelper.Model model) {
		return m.getResource(model.getURI().toString());
	}

	private static void addChildren(Resource c, List<Resource> result) {
		NodeIterator iterator = null;
		if (c.hasProperty(RDF.type, RDF.Bag)) {
			iterator = c.getModel().getBag(c).iterator();
		} else if(c.hasProperty(RDF.type, RDF.Seq)){
			iterator = c.getModel().getSeq(c).iterator();
		}
		List<Resource> containers = new ArrayList<Resource>();
		while(iterator.hasNext()) {
			Resource n = (Resource)iterator.next();
			result.add(n);
			if (n.hasProperty(RDF.type, RDF.Bag) || n.hasProperty(RDF.type, RDF.Seq)) {
				containers.add(n);
			}
		}
		iterator.close();
		for(Resource r : containers) {
			addChildren(r, result);
		}
	}

	public static List<Resource> getObjectsBreadthFirst(Model m, PID depositPID) {
		List<Resource> result = new ArrayList<Resource>();
		Resource top = m.getResource(depositPID.getURI());
		addChildren(top, result);
		return result;
	}
}
