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
package edu.unc.lib.dl.cdr.sword.server.managers;

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
