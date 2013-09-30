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

import org.apache.abdera.Abdera;
import org.apache.abdera.writer.Writer;
import org.apache.log4j.Logger;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.AtomPubEntrySIP;
import edu.unc.lib.dl.services.IngestResult;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.PackagingType;

public class AtomPubEntryDepositHandler extends AbstractDepositHandler {
	private static Logger log = Logger.getLogger(AtomPubEntryDepositHandler.class);

	@Override
	public DepositReceipt doDeposit(PID destination, Deposit deposit, PackagingType type, SwordConfiguration config,
			String depositor, String owner) throws Exception {
		log.debug("Preparing to perform an Atom Pub entry metadata only deposit to " + destination.getPid());

		if (deposit.getSwordEntry() == null || deposit.getSwordEntry().getEntry() == null)
			throw new SwordError(UriRegistry.ERROR_CONTENT, 415, "No AtomPub entry was included in the submission");

		AtomPubEntrySIP sip = new AtomPubEntrySIP(destination, deposit.getSwordEntry().getEntry());
		if (log.isDebugEnabled()) {
			Abdera abdera = new Abdera();
			Writer writer = abdera.getWriterFactory().getWriter("prettyxml");
			writer.writeTo(deposit.getSwordEntry().getEntry(), System.out);
		}

		if (deposit.getFile() == null) {
			sip = new AtomPubEntrySIP(destination, deposit.getSwordEntry().getEntry());
		} else {
			sip = new AtomPubEntrySIP(destination, deposit.getSwordEntry().getEntry(), deposit.getFile(),
					deposit.getMimeType(), deposit.getFilename(), deposit.getMd5());
		}
		sip.setInProgress(deposit.isInProgress());
		sip.setSuggestedSlug(deposit.getSlug());

		DepositRecord record = new DepositRecord(depositor, owner, DepositMethod.SWORD13);
		record.setDepositorEmail(SwordConfigurationImpl.getUserEmailAddress());
		record.setMessage("Added through SWORD");
		IngestResult ingestResult = digitalObjectManager.addToIngestQueue(sip, record);

		return buildReceipt(ingestResult, config);
	}
}
