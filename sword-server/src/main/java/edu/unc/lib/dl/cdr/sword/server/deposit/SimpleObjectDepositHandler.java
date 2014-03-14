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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.PackagingType;

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
		File dir = makeNewDepositDirectory(depositPID.getUUID());
		dir.mkdir();

		// write deposit file to data directory
		if (deposit.getFile() != null) {
			File dataDir = new File(dir, "data");
			File depositFile = new File(dataDir, deposit.getFilename());
			depositFile.mkdirs();
			try {
				FileUtils.renameOrMoveTo(deposit.getFile(), depositFile);
			} catch (IOException e) {
				throw new Error(e);
			}
		}

		// TODO MOVE verify checksum for payload file
//		if (deposit.getMd5() != null) {
//			Manifest mani = bag.getPayloadManifest(Algorithm.MD5);
//			String bagitMD5 = mani.get("data/" + deposit.getFilename());
//			if (bagitMD5 == null || !bagitMD5.equals(deposit.getMd5())) {
//				throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400,
//						"The supplied checksum of " + deposit.getMd5()
//								+ " does not match " + bagitMD5
//								+ " (calculated)");
//			}
//		}

		registerDeposit(depositPID, destination, deposit,
				type, depositor, owner, Collections.<String, String> emptyMap());
		return buildReceipt(depositPID, config);
	}
}
