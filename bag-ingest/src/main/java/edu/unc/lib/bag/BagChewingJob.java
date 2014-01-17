package edu.unc.lib.bag;

import static edu.unc.lib.dl.util.DepositBagInfo.PACKAGING_TYPE;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;

public class BagChewingJob extends AbstractBagJob {
	Client jesqueClient = null;
	public Client getJesqueClient() {
		return jesqueClient;
	}

	public void setJesqueClient(Client jesqueClient) {
		this.jesqueClient = jesqueClient;
	}

	BagFactory bagFactory = new BagFactory();
	
	public BagChewingJob(File bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}

	@Override
	public void run() {
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
			}
			Job job = new Job(convertJob, getBagDirectory().getAbsolutePath());
			jesqueClient.enqueue("INGEST_QUEUE", job);
			return;
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
