package edu.unc.lib.dl.cdr.sword.server.managers;

import java.util.List;
import java.util.Map;

import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ContainerManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.services.DigitalObjectManager;

public class ContainerManagerImpl extends AbstractFedoraManager implements ContainerManager {

	private DigitalObjectManager digitalObjectManager;
	private AgentFactory agentFactory;

	@Override
	public DepositReceipt replaceMetadata(String editIRI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DepositReceipt replaceMetadataAndMediaResource(String editIRI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DepositReceipt addMetadataAndResources(String editIRI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DepositReceipt addMetadata(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
			throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DepositReceipt addResources(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
			throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Deletes the object identified in editIRI if the submitted auth credentials have delete rights to the targeted object.
	 */
	@Override
	public void deleteContainer(String editIRI, AuthCredentials auth, SwordConfiguration config) throws SwordError,
			SwordServerException, SwordAuthException {

		Agent user = agentFactory.findPersonByOnyen(auth.getUsername(), false);
		if (user == null){
			throw new SwordAuthException("Unable to find a user matching the submitted username credentials, " + auth.getUsername());
		}
		//Ignoring on-behalf-of for the moment
		
		SwordConfigurationImpl configImpl = (SwordConfigurationImpl)config;
		
		PID targetPID = extractPID(editIRI, SwordConfigurationImpl.EDIT_PATH + "/");
		
		List<String> groupList = this.getGroups(auth, configImpl);
		
		if (!accessControlUtils.hasAccess(targetPID, groupList, "http://cdr.unc.edu/definitions/roles#curator")){
			throw new SwordAuthException("Insufficient privileges to delete object " + targetPID.getPid());
		}
		
		try {
			this.digitalObjectManager.delete(targetPID, user, "Deleted by " + user.getName());
		} catch (NotFoundException e) {
			throw new SwordError("Unable to delete the object " + targetPID.getPid() +".  The object was not found in the repository.");
		} catch (IngestException e) {
			throw new SwordServerException("Failed to delete object " + targetPID.getPid(), e);
		}
	}

	@Override
	public DepositReceipt useHeaders(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
			throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isStatementRequest(String editIRI, Map<String, String> accept, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DepositReceipt getEntry(String editIRI, Map<String, String> accept, AuthCredentials auth,
			SwordConfiguration config) throws SwordServerException, SwordError, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}

	
}
