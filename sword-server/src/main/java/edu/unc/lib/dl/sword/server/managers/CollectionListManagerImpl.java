package edu.unc.lib.dl.sword.server.managers;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionListManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class CollectionListManagerImpl extends AbstractFedoraManager implements CollectionListManager {

	@Override
	public Feed listCollectionContents(IRI collectionIRI, AuthCredentials auth, SwordConfiguration config)
			throws SwordServerException, SwordAuthException, SwordError {
		// TODO Auto-generated method stub
		return null;
	}

}
