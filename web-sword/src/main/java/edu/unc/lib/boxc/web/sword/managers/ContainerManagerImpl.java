package edu.unc.lib.boxc.web.sword.managers;

import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ContainerManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

/**
 *
 * @author bbpennel
 *
 */
public class ContainerManagerImpl extends AbstractFedoraManager implements ContainerManager {

    @Override
    public DepositReceipt replaceMetadata(String editIRI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not yet supported");
    }

    @Override
    public DepositReceipt replaceMetadataAndMediaResource(String editIRI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not yet supported");
    }

    @Override
    public DepositReceipt addMetadataAndResources(String editIRI, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not yet supported");
    }

    @Override
    public DepositReceipt addMetadata(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not yet supported");
    }

    @Override
    public DepositReceipt addResources(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {
        // This happens in the MediaResourceManager. This method isn't referenced
        return null;
    }

    /**
     * Deletes the object identified in editIRI if the submitted auth credentials have delete rights to the targeted
     * object.
     */
    @Override
    public void deleteContainer(String editIRI, AuthCredentials auth, SwordConfiguration config) throws SwordError,
    SwordServerException, SwordAuthException {
        throw new NotImplementedException("Operation not supported");
    }

    /**
     * Empty body request with headers. Allows for declaring an item to no longer be in-progress
     */
    @Override
    public DepositReceipt useHeaders(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {
        throw new NotImplementedException("Operation not supported");
    }

    /**
     * Determines if the request is a statement request instead of a deposit receipt request. Does not return a
     * statement.
     */
    @Override
    public boolean isStatementRequest(String editIRI, Map<String, String> accept, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * After-the-fact deposit receipt retrieval method. From the EDIT-IRI
     */
    @Override
    public DepositReceipt getEntry(String editIRIString, Map<String, String> accept, AuthCredentials auth,
            SwordConfiguration configBase) throws SwordServerException, SwordError, SwordAuthException {
        throw new NotImplementedException("Operation not supported");
    }
}
