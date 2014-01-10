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
import static edu.unc.lib.dl.util.DepositBagInfo.METS_PROFILE;
import static edu.unc.lib.dl.util.DepositBagInfo.METS_TYPE;
import static edu.unc.lib.dl.util.DepositBagInfo.PACKAGING_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
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
import edu.unc.lib.dl.util.MetsHeaderScanner;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.ZipFileUtil;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagInfoTxt;
import gov.loc.repository.bagit.transformer.impl.UpdateCompleter;
import gov.loc.repository.bagit.writer.impl.FileSystemWriter;

public class METSDepositHandler extends AbstractDepositHandler {
	private static Logger log = Logger.getLogger(METSDepositHandler.class);

	@Override
	public DepositReceipt doDeposit(PID destination, Deposit deposit, PackagingType type, SwordConfiguration config,
			String depositor, String owner) throws SwordError {
		if (log.isDebugEnabled()) {
			log.debug("Preparing to perform a CDR METS deposit to " + destination.getPid());
			log.debug("Working with temporary file: "+ deposit.getFile().getAbsolutePath());
		}
		
		// extract info from METS header
		MetsHeaderScanner scanner = new MetsHeaderScanner();
		try {
			scanner.scan(deposit.getFile());
		} catch (IOException e1) {
			throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400, "Unable to unpack your deposit: "+deposit.getFilename());
		}
		
		PID depositPID = scanner.getObjID();
		if(depositPID == null) {
			UUID depositUUID = UUID.randomUUID();
			depositPID = new PID("uuid:"+depositUUID.toString());
		}
		File bagDir = getNewBagDirectory(depositPID.getUUID());
		
		// create or unzip bag directory
		String name = deposit.getFilename();
		BagFactory factory = new BagFactory();
		Bag bag = null;
		File metsFile = new File(bagDir, "METS.xml");
		if(name.endsWith(".zip")) {
			try {
				ZipFileUtil.unzipToDir(deposit.getFile(), bagDir);
				bag = factory.createBag(bagDir);
			} catch (IOException e) {
				throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400, "Unable to unpack your deposit: "+depositPID.getPid());
			}
		} else {
			try {
				FileUtils.renameOrMoveTo(deposit.getFile(), metsFile);
				bag = factory.createBagByPayloadFiles(bagDir, BagFactory.LATEST, Collections.<String> emptyList());
			} catch (IOException e) {
				throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500, "Unable to create your deposit bag: "+depositPID.getPid(), e);
			}
		}

		// add metadata from METS
		BagInfoTxt info = bag.getBagInfoTxt();
		if(info == null) info = bag.getBagPartFactory().createBagInfoTxt();
		bag.putBagFile(info);
		String email = SwordConfigurationImpl.getUserEmailAddress();
		if(email != null) {
			info.addContactEmail(email);
			info.addContactName(depositor);
		} else {
			info.addContactEmail(owner+"@email.unc.edu");
			info.addContactName(owner);
		}
		info.setInternalSenderDescription("Added via SWORD");
		info.setInternalSenderIdentifier(depositor);
		info.addExternalIdentifier(deposit.getFilename());
		info.setBaggingDate(scanner.getCreateDate());
		info.put(DEPOSIT_METHOD, DepositMethod.SWORD13.getLabel());
		info.put(DEPOSIT_ID, depositPID.getPid());
		info.put(CONTAINER_ID, destination.getPid());
		info.put(PACKAGING_TYPE, type.getUri());
		info.put(METS_PROFILE, scanner.getProfile());
		info.put(METS_TYPE, scanner.getType());
		
		// depositor groups (forwarded for permissions)
		if (this.getOverridePermissionGroups() != null) {
			info.putList(DEPOSIT_PERMISSION_GROUP, this.getOverridePermissionGroups());
		} else {
			info.putList(DEPOSIT_PERMISSION_GROUP, GroupsThreadStore.getGroups());
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
		queueForIngest(bagDir);
		return buildReceipt(depositPID, config);
	}
}