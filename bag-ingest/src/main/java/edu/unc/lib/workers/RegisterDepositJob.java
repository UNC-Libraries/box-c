package edu.unc.lib.workers;

import edu.unc.lib.dl.util.DepositBagInfoTxt;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagInfoTxt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterDepositJob extends AbstractBagJob implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(RegisterDepositJob.class);

	public RegisterDepositJob(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	public RegisterDepositJob() {
	}

	@Override
	public void run() {
		log.debug("registering deposit: {}", this.getBagDirectory().getPath());
		Bag bag = loadBag();
		
		// create deposit status
		BagInfoTxt info = bag.getBagInfoTxt();
		Map<String, String> status = new HashMap<String, String>();
		status.put(DepositField.bagDate.name(), info.getBaggingDate());
		status.put(DepositField.bagDirectory.name(), getBagDirectory().getAbsolutePath());
		status.put(DepositField.contactEmail.name(), info.getContactEmail());
		status.put(DepositField.contactName.name(), info.getContactName());
		status.put(DepositField.containerId.name(), info.get(DepositBagInfoTxt.CONTAINER_ID));
		status.put(DepositField.depositMethod.name(), info.get(DepositBagInfoTxt.DEPOSIT_METHOD));
		status.put(DepositField.extIdentifier.name(), info.getExternalIdentifier());
		status.put(DepositField.intSenderDescription.name(), info.getInternalSenderDescription());
		status.put(DepositField.intSenderIdentifier.name(), info.getInternalSenderIdentifier());
		status.put(DepositField.payLoadOctets.name(), info.getPayloadOxum());
		status.put(DepositField.startTime.name(), String.valueOf(System.currentTimeMillis()));
		status.put(DepositField.status.name(), "");
		status.put(DepositField.uuid.name(), getDepositPID().getUUID());
		Set<String> nulls = new HashSet<String>();
		for(String key : status.keySet()) {
			if(status.get(key) == null) nulls.add(key);
		}
		for(String key : nulls) status.remove(key);
		this.getDepositStatusFactory().save(getDepositPID().getUUID(), status);
	}

}
