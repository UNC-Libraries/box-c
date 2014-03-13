package edu.unc.lib.bag.normalize;

import static edu.unc.lib.dl.util.BagInfoTxtExtensions.PACKAGING_TYPE;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Callable;

import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.workers.AbstractBagJob;
import gov.loc.repository.bagit.Bag;

/**
 * Taking in a deposit directory in BagIt form, with a variety of manifest types,
 * this job adds required elements to make a normal CDR BagIt RDF deposit.
 * 
 * All specialized package-specific transforms are handled here.
 * @author count0
 *
 */
public class NormalizeBag extends AbstractBagJob implements Callable<String> {

	public NormalizeBag(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	public String call() {
		
		Bag bag = getBag();
		// pack the bag in N3 CDR style
		List<String> packagings = bag.getBagInfoTxt().getList(PACKAGING_TYPE);
		
		if(!packagings.contains(PackagingType.BAG_WITH_N3.getUri())) {
			// we need to add N3 packaging to this bag
			Class convertJob = null;
			if(packagings.contains(PackagingType.METS_CDR.getUri())) {
				convertJob = CDRMETS2N3BagJob.class;
			} else if(packagings.contains(PackagingType.METS_DSPACE_SIP_1.getUri())
					|| packagings.contains(PackagingType.METS_DSPACE_SIP_2.getUri())) {
				convertJob = DSPACEMETS2N3BagJob.class;
			} /*else if(packagings.contains(PackagingType.SIMPLE_OBJECT.getUri())) {
				convertJob = SIMPLE2N3BagJob.class;
			} else if(packagings.contains(PackagingType.ATOM.getUri())) {
				convertJob = Atom2N3BagJob.class;
			}*/
			if(convertJob == null) {
				String msg = MessageFormat.format("Cannot convert deposit package to N3 BagIt. No converter for this packaging type(s): {}", packagings.toArray());
				failJob(Type.NORMALIZATION, "Cannot convert deposit to N3 BagIt package.", msg);
			} else {
				return convertJob.getName();
			}
		}
		return null;
	}

}
