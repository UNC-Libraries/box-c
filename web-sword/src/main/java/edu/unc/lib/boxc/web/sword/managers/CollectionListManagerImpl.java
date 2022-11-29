package edu.unc.lib.boxc.web.sword.managers;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionListManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

/**
 *
 * @author bbpennel
 *
 */
public class CollectionListManagerImpl extends AbstractFedoraManager implements CollectionListManager {

    private int pageSize = 10;

    @Override
    public Feed listCollectionContents(IRI collectionIRI, AuthCredentials auth, SwordConfiguration config)
            throws SwordServerException, SwordAuthException, SwordError {

        throw new SwordServerException("Method not supported");
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}