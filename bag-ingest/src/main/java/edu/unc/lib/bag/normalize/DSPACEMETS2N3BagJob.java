package edu.unc.lib.bag.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;

import java.io.File;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.METSProfile;

public class DSPACEMETS2N3BagJob extends AbstractMETS2N3BagJob {
	private static final Logger log = LoggerFactory.getLogger(DSPACEMETS2N3BagJob.class);

	public DSPACEMETS2N3BagJob(String bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}
	
	@Override
	public void run() {
		validateMETS();
		validateProfile(METSProfile.CDR_SIMPLE);
		Document mets = loadMETS();
		assignPIDs(mets); // assign any missing PIDs
		saveMETS(mets); // manifest updated to have record of all PIDs
		
		Model model = ModelFactory.createDefaultModel();
		METSHelper helper = new METSHelper(mets);
		
		// add aggregate work bag
		Element aggregateEl = helper.mets.getRootElement().getChild("structMap", METS_NS).getChild("div", METS_NS);
		Bag aggregate = model.createBag(METSHelper.getPIDURI(aggregateEl));
		Property hasModel = model.createProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
		model.add(aggregate, hasModel, model.createResource(ContentModelHelper.Model.CONTAINER.getURI().toString()));
		model.add(aggregate, hasModel, model.createResource(ContentModelHelper.Model.AGGREGATE_WORK.getURI().toString()));
		Resource simple = model.createResource(ContentModelHelper.Model.SIMPLE.getURI().toString());
		@SuppressWarnings("unchecked")
		List<Element> topchildren = (List<Element>) aggregateEl.getChildren(
				"div", METS_NS);
		for (Element childEl : topchildren) {
			Resource child = model.createResource(METSHelper.getPIDURI(childEl));
			aggregate.add(child);
			model.add(child, hasModel, simple);
		}
		helper.addFileAssociations(model, true);
		
		final File modsFolder = new File(getBagDirectory(), "description");
		modsFolder.mkdir();
		
		// TODO extract EPDCX from mets
		
		saveModel(model, "everything.n3");
		// addN3PackagingType();
		recordEvent(Type.NORMALIZATION, "Converted METS {1} to N3 form", METSProfile.CDR_SIMPLE.getName());
		
		// TODO enqueue for additional BIOMED job if applicable
	}

}
