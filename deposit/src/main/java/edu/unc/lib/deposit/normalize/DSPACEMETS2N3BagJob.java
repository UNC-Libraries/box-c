package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.EPDCX_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.METSProfile;

public class DSPACEMETS2N3BagJob extends AbstractMETS2N3BagJob {

	private static final Logger log = LoggerFactory.getLogger(DSPACEMETS2N3BagJob.class);

	private Transformer epdcx2modsTransformer = null;

	public Transformer getEpdcx2modsTransformer() {
		return epdcx2modsTransformer;
	}

	public void setEpdcx2modsTransformer(Transformer epdcx2modsTransformer) {
		this.epdcx2modsTransformer = epdcx2modsTransformer;
	}

	public DSPACEMETS2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		validateMETS();

		// Store a reference to the manifest file
		addManifestURI();

		validateProfile(METSProfile.DSPACE_SIP);
		Document mets = loadMETS();
		assignPIDs(mets); // assign any missing PIDs
		saveMETS(mets); // manifest updated to have record of all PIDs

		Model model = getWritableModel();
		METSHelper helper = new METSHelper(mets);

		// deposit RDF bag
		Bag top = model.createBag(getDepositPID().getURI().toString());
		// add aggregate work bag
		Element aggregateEl = helper.mets.getRootElement().getChild("structMap", METS_NS).getChild("div", METS_NS);
		Bag aggregate = model.createBag(METSHelper.getPIDURI(aggregateEl));
		top.add(aggregate);
		Property hasModel = model.createProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
		model.add(aggregate, hasModel, model.createResource(ContentModelHelper.Model.CONTAINER.getURI().toString()));
		model.add(aggregate, hasModel, model.createResource(ContentModelHelper.Model.AGGREGATE_WORK.getURI().toString()));
		List<Element> topchildren = aggregateEl.getChildren(
				"div", METS_NS);
		for (Element childEl : topchildren) {
			Resource child = model.createResource(METSHelper.getPIDURI(childEl));
			aggregate.add(child);
		}
		helper.addFileAssociations(model, true);

		// extract EPDCX from mets
		FileOutputStream fos = null;
		try {
			Element epdcxEl = mets.getRootElement().getChild("dmdSec", METS_NS).getChild("mdWrap", METS_NS).getChild("xmlData", METS_NS).getChild("descriptionSet", EPDCX_NS);
			JDOMResult mods = new JDOMResult();
			epdcx2modsTransformer.transform(new JDOMSource(epdcxEl), mods);
			final File modsFolder = getDescriptionDir();
			modsFolder.mkdir();
			File modsFile = new File(modsFolder, new PID(aggregate.getURI()).getUUID()+".xml");
			fos = new FileOutputStream(modsFile);
			new XMLOutputter(Format.getPrettyFormat()).output(mods.getDocument(), fos);
		} catch(NullPointerException ignored) {
			log.debug("NPE", ignored);
			// no embedded metadata
		} catch (TransformerException e) {
			failJob(e, "Failed during transform of EPDCX to MODS.");
		} catch (FileNotFoundException e) {
			failJob(e, "Failed during transform of EPDCX to MODS.");
		} catch (IOException e) {
			failJob(e, "Failed during transform of EPDCX to MODS.");
		}

		recordDepositEvent(Type.NORMALIZATION, "Normalized deposit package from {0} to {1}", PackagingType.METS_DSPACE_SIP_1.getUri(), PackagingType.BAG_WITH_N3.getUri());
	}

}
