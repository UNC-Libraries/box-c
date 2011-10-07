package edu.unc.lib.dl.sword.server.managers;

import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.CollectionListManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class CollectionDepositManagerImpl extends AbstractFedoraManager implements CollectionDepositManager {

	@Override
	public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

}
