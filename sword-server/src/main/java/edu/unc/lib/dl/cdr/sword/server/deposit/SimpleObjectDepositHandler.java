/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.sword.server.deposit;

import static edu.unc.lib.dl.util.DepositBagInfo.CONTAINER_ID;
import static edu.unc.lib.dl.util.DepositBagInfo.DEPOSIT_ID;
import static edu.unc.lib.dl.util.DepositBagInfo.DEPOSIT_METHOD;
import static edu.unc.lib.dl.util.DepositBagInfo.DEPOSIT_PERMISSION_GROUP;
import static edu.unc.lib.dl.util.DepositBagInfo.PACKAGING_TYPE;
import static edu.unc.lib.dl.util.DepositBagInfo.SWORD_SLUG;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.ErrorURIRegistry;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.PackagingType;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagInfoTxt;
import gov.loc.repository.bagit.Manifest;
import gov.loc.repository.bagit.Manifest.Algorithm;
import gov.loc.repository.bagit.transformer.impl.UpdateCompleter;
import gov.loc.repository.bagit.writer.impl.FileSystemWriter;
import gov.loc.repository.bagit.PreBag;

public class SimpleObjectDepositHandler extends AbstractDepositHandler {
	private static Logger log = Logger
			.getLogger(SimpleObjectDepositHandler.class);

	@Override
	public DepositReceipt doDeposit(PID destination, Deposit deposit,
			PackagingType type, SwordConfiguration config, String depositor,
			String owner) throws Exception {
		log.debug("Preparing to perform a Simple Object deposit to "
				+ destination.getPid());

		PID depositPID = null;
		UUID depositUUID = UUID.randomUUID();
		depositPID = new PID("uuid:" + depositUUID.toString());
		File bagDir = getNewBagDirectory(depositPID.getUUID());
		bagDir.mkdir();

		// write deposit file to data directory
		if (deposit.getFile() != null) {
			File dataDir = new File(bagDir, "data");
			File depositFile = new File(dataDir, deposit.getFilename());
			depositFile.mkdirs();
			try {
				FileUtils.renameOrMoveTo(deposit.getFile(), depositFile);
			} catch (IOException e) {
				throw new Error(e);
			}
		}

		// make bag
		BagFactory factory = new BagFactory();
		PreBag prebag = factory.createPreBag(bagDir);
		Bag bag = prebag.makeBagInPlace(BagFactory.LATEST, false);

		// verify checksum for payload file
		if (deposit.getMd5() != null) {
			Manifest mani = bag.getPayloadManifest(Algorithm.MD5);
			String bagitMD5 = mani.get("data/" + deposit.getFilename());
			if (bagitMD5 == null || !bagitMD5.equals(deposit.getMd5())) {
				throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400,
						"The supplied checksum of " + deposit.getMd5()
								+ " does not match " + bagitMD5
								+ " (calculated)");
			}
		}

		// add metadata from SWORD/Atom
		BagInfoTxt info = bag.getBagInfoTxt();
		if(info == null) info = bag.getBagPartFactory().createBagInfoTxt();
		bag.putBagFile(info);
		String email = SwordConfigurationImpl.getUserEmailAddress();
		if (email != null) {
			info.addContactEmail(email);
			info.addContactName(depositor);
		} else {
			info.addContactEmail(owner + "@email.unc.edu");
			info.addContactName(owner);
		}
		info.setInternalSenderDescription("Added via SWORD");
		info.setInternalSenderIdentifier(depositor);
		info.addExternalIdentifier(deposit.getFilename());
		// info.setBaggingDate( ?? );
		info.put(DEPOSIT_METHOD, DepositMethod.SWORD13.getLabel());
		info.put(DEPOSIT_ID, depositPID.getPid());
		info.put(CONTAINER_ID, destination.getPid());
		info.put(PACKAGING_TYPE, type.getUri());
		info.put(SWORD_SLUG, deposit.getSlug());

		// depositor groups (forwarded for permissions)
		if (this.getOverridePermissionGroups() != null) {
			info.putList(DEPOSIT_PERMISSION_GROUP,
					this.getOverridePermissionGroups());
		} else {
			info.putList(DEPOSIT_PERMISSION_GROUP,
					GroupsThreadStore.getGroups());
		}
		info.generateBagSize(bag);
		bag = bag.makeComplete(new UpdateCompleter(factory));
		try {
			FileSystemWriter writer = new FileSystemWriter(factory);
			writer.setTagFilesOnly(true);
			writer.write(bag, bagDir);
			bag.close();
		} catch (IOException e) {
			throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500, "Unable to write to deposit bag: "+depositPID.getPid());
		}

		queueForIngest(bagDir, depositPID);
		return buildReceipt(depositPID, config);
	}
}
