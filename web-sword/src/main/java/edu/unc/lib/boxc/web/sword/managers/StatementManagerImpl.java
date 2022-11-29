package edu.unc.lib.boxc.web.sword.managers;

import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Statement;
import org.swordapp.server.StatementManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

/**
 *
 * @author bbpennel
 *
 */
public class StatementManagerImpl extends AbstractFedoraManager implements StatementManager {

    @Override
    public Statement getStatement(String iri, Map<String, String> accept,
            AuthCredentials auth, SwordConfiguration config)
                    throws SwordServerException, SwordError, SwordAuthException {

        throw new NotImplementedException("Operation not supported");
    }
}
