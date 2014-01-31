package edu.unc.lib.bag.normalize;

import static edu.unc.lib.dl.util.DepositBagInfo.PACKAGING_TYPE;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.EPDCX_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.FileUtils;
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

	public DSPACEMETS2N3BagJob(String bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}
	
	@Override
	public void run() {
		gov.loc.repository.bagit.Bag bag = loadBag();
		// copy DSPACE METS into tag space.
		try {
			File payloadMets = new File(getBagDirectory(), "data/mets.xml");
			FileUtils.copyFile(payloadMets, new File(getBagDirectory(), "mets.xml"));
			bag.addFileAsTag(getMETSFile());
		} catch (IOException e) {
			throw new Error(e);
		}
		validateMETS();
		validateProfile(METSProfile.DSPACE_SIP);
		Document mets = loadMETS();
		assignPIDs(mets); // assign any missing PIDs
		saveMETS(mets); // manifest updated to have record of all PIDs
		
		Model model = ModelFactory.createDefaultModel();
		METSHelper helper = new METSHelper(mets);
		
		// deposit bag
		Bag top = model.createBag(getDepositPID().getURI().toString());
		// add aggregate work bag
		Element aggregateEl = helper.mets.getRootElement().getChild("structMap", METS_NS).getChild("div", METS_NS);
		Bag aggregate = model.createBag(METSHelper.getPIDURI(aggregateEl));
		top.add(aggregate);
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
		saveModel(model, MODEL_FILE);
		bag.addFileAsTag(new File(getBagDirectory(), MODEL_FILE));
		
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
			bag.addFileAsTag(modsFolder);
		} catch(NullPointerException ignored) {
			log.debug("NPE", ignored);
			// no embedded metadata
		} catch (TransformerException e) {
			failDeposit(e, Type.NORMALIZATION, "Failed during transform of EPDCX to MODS");
		} catch (FileNotFoundException e) {
			failDeposit(e, Type.NORMALIZATION, "Failed during transform of EPDCX to MODS");
		} catch (IOException e) {
			failDeposit(e, Type.NORMALIZATION, "Failed during transform of EPDCX to MODS");
		}

		List<String> packagings = bag.getBagInfoTxt().getList(PACKAGING_TYPE);
		bag.getBagInfoTxt().putList(PACKAGING_TYPE, PackagingType.BAG_WITH_N3.getUri());
		recordDepositEvent(Type.NORMALIZATION, "Normalized deposit package from {0} to {1}", packagings, PackagingType.BAG_WITH_N3.getUri());
		
		saveBag(bag);
		
		// enqueue for additional BIOMED job if applicable
		if("BioMed Central".equals(bag.getBagInfoTxt().getInternalSenderDescription())) {
			enqueueNextJob(BioMedCentralExtrasJob.class.getName());
		} else {
			enqueueDefaultNextJob();
		}
	}

}
