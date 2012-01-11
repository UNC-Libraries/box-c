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
package edu.unc.lib.dl.cdr.services.sword.managers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.CollectionListManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.PreIngestEventLogger;
import edu.unc.lib.dl.services.DigitalObjectManager;

/**
 * 
 * @author bbpennel
 * 
 */
public class CollectionDepositManagerImpl extends AbstractFedoraManager implements CollectionDepositManager {
	private static Logger LOG = Logger.getLogger(CollectionDepositManagerImpl.class);

	private DigitalObjectManager digitalObjectManager;
	private AgentManager agentManager;

	@Override
	public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {

		try {
			if (deposit.isBinaryOnly()){
				return this.doBinaryDeposit(collectionURI, deposit, auth, config);
			} else if (deposit.isMultipart()){
				
			} else if (deposit.isEntryOnly()){
				
			}
		} catch (Exception e) {
			LOG.error("Exception while attempting to deposit", e);
			System.out.println("Exception while attempting to deposit ");
			e.printStackTrace();
		}
		return null;
	}
	
	private DepositReceipt doBinaryDeposit(String collectionURI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws Exception {
		
		LOG.debug("Preparing to perform a binary deposit");
		
		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");
		
		Agent agent = agentManager.findPersonByOnyen("bbpennel", true);

		PID pid = new PID(collectionURI);
		String containerPath = this.tripleStoreQueryService.lookupRepositoryPath(pid);
		
		METSPackageSIP sip = new METSPackageSIP("/Collections/", deposit.getInputStream(), agent, isZip);
		// PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

		digitalObjectManager.add(sip, agent, "Added through SWORD", false);

		DepositReceipt receipt = new DepositReceipt();
		IRI editMediaIRI = new IRI("https://localhost/cdr-services/sword/collection/");

		receipt.addEditMediaIRI(editMediaIRI);
		receipt.setSwordEditIRI(editMediaIRI);
		receipt.setEditMediaIRI(editMediaIRI);

		LOG.info("Returning receipt " + receipt);
		return receipt;
	}
	
	private void outputStreamToFile(Deposit deposit) throws Exception {
		File metsFile = File.createTempFile("input-", ".zip");
		OutputStream os = new BufferedOutputStream(new FileOutputStream(metsFile));
		try {
			byte[] buf = new byte[4096];
			int len;
			while ((len = deposit.getInputStream().read(buf)) != -1) {
				os.write(buf, 0, len);
			}
		} finally {
			deposit.getInputStream().close();
			os.flush();
			os.close();
			deposit.setInputStream(new FileInputStream(metsFile));
		}
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public AgentManager getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentManager agentManager) {
		this.agentManager = agentManager;
	}
}
