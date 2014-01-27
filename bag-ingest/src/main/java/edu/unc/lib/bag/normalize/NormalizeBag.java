package edu.unc.lib.bag.normalize;

import static edu.unc.lib.dl.util.DepositBagInfo.PACKAGING_TYPE;

import java.text.MessageFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.bag.AbstractBagJob;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;

/**
 * Taking in a deposit directory in BagIt form, with a variety of manifest types,
 * this job adds required elements to make a normal CDR BagIt RDF deposit.
 * 
 * All specialized package-specific transforms are handled here.
 * @author count0
 *
 */
public class NormalizeBag extends AbstractBagJob {
	private static final Logger log = LoggerFactory.getLogger(NormalizeBag.class);

	BagFactory bagFactory = new BagFactory();
	
	public NormalizeBag(String bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}

	@Override
	public void run() {
		log.debug("starting NormalizeBag job: {}", this.getBagDirectory().getPath());
		Bag bag = bagFactory.createBag(getBagDirectory());
		
		// pack the bag in N3 CDR style
		List<String> packagings = bag.getBagInfoTxt().getList(PACKAGING_TYPE);
		if(!packagings.contains(PackagingType.BAG_WITH_N3.getUri())) {
			// we need to add N3 packaging to this bag
			String convertJob = null;
			if(packagings.contains(PackagingType.METS_CDR.getUri())) {
				convertJob = CDRMETS2N3BagJob.class.getName();
			} else if(packagings.contains(PackagingType.METS_DSPACE_SIP_1.getUri())
					|| packagings.contains(PackagingType.METS_DSPACE_SIP_2.getUri())) {
				convertJob = "DSPACEMETS2N3BagJob";
			} else if(packagings.contains(PackagingType.SIMPLE_OBJECT.getUri())) {
				convertJob = "SIMPLE2N3BagJob";
			} else if(packagings.contains(PackagingType.ATOM.getUri())) {
				convertJob = "Atom2N3BagJob";
			}
			if(convertJob == null) {
				String msg = MessageFormat.format("Cannot convert deposit package to N3 BagIt. No converter for this packaging type(s): {}", packagings.toArray());
				failDeposit(Type.NORMALIZATION, "Cannot convert deposit to N3 BagIt package.", msg);
			} else {
				enqueueNextJob(convertJob);
			}
		}

		// BagIt validation
		
		// 1st set of parallel metadata creation and file operations:
		//   Biomed metadata extraction
		//   Atom metadata extraction
		//   Virus scan
		
		// Parallel 
		// Container check
		// Fix path conflicts
		
	}

}
