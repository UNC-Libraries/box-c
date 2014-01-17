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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.apache.abdera.Abdera;
import org.apache.abdera.writer.Writer;
import org.apache.log4j.Logger;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

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

/**
 * Default handler when packaging type is null.
 * May include a file or not.
 * All metadata is inside of the Atom entry.
 * @author count0
 *
 */
public class AtomPubEntryDepositHandler extends AbstractDepositHandler {
	private static Logger log = Logger
			.getLogger(AtomPubEntryDepositHandler.class);

	@Override
	public DepositReceipt doDeposit(PID destination, Deposit deposit,
			PackagingType type, SwordConfiguration config, String depositor,
			String owner) throws SwordError {
		log.debug("Preparing to perform an Atom Pub entry metadata only deposit to "
				+ destination.getPid());

		if (deposit.getSwordEntry() == null
				|| deposit.getSwordEntry().getEntry() == null)
			throw new SwordError(UriRegistry.ERROR_CONTENT, 415,
					"No AtomPub entry was included in the submission");

		if (log.isDebugEnabled()) {
			Abdera abdera = new Abdera();
			Writer writer = abdera.getWriterFactory().getWriter("prettyxml");
			try {
				writer.writeTo(deposit.getSwordEntry().getEntry(), System.out);
			} catch (IOException e) {
				throw new Error(e);
			}
		}

		PID depositPID = null;
		UUID depositUUID = UUID.randomUUID();
		depositPID = new PID("uuid:" + depositUUID.toString());
		File bagDir = getNewBagDirectory(depositPID.getUUID());
		bagDir.mkdir();

		// write SWORD Atom entry to file
		File atomFile = new File(bagDir, "atom.xml");
		Abdera abdera = new Abdera();
		FileOutputStream fos = null;
		try {
			Writer writer = abdera.getWriterFactory().getWriter("prettyxml");
			fos = new FileOutputStream(atomFile);
			writer.writeTo(deposit.getSwordEntry().getEntry(), fos);
		} catch (IOException e) {
			throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400,
					"Unable to unpack your deposit: " + deposit.getFilename(),
					e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ignored) {
				}
			}
		}

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
		prebag.setTagFiles(Collections.singletonList(atomFile));
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
		
		// deposit.getMimeType()

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
		info.setInternalSenderIdentifier(depositor);
		info.addExternalIdentifier(deposit.getFilename());
		info.setBaggingDate(new Date(System.currentTimeMillis()));
		info.put(DEPOSIT_METHOD, DepositMethod.SWORD13.getLabel());
		info.put(DEPOSIT_ID, depositPID.getPid());
		info.put(CONTAINER_ID, destination.getPid());
		info.put(PACKAGING_TYPE, PackagingType.ATOM.getUri());
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

		queueForIngest(bagDir);
		return buildReceipt(depositPID, config);
	}
}
