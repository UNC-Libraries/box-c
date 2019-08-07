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

import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.MediaResource;
import org.swordapp.server.MediaResourceManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

/**
 *
 * @author bbpennel
 *
 */
public class MediaResourceManagerImpl extends AbstractFedoraManager implements MediaResourceManager {

    @Override
    public MediaResource getMediaResourceRepresentation(String uri, Map<String, String> accept, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not supported");
    }

    @Override
    public DepositReceipt replaceMediaResource(String uri, Deposit deposit, AuthCredentials auth,
            SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not supported");
    }

    @Override
    public void deleteMediaResource(String uri, AuthCredentials auth, SwordConfiguration config) throws SwordError,
    SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not supported");
    }

    @Override
    public DepositReceipt addResource(String uri, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {
        throw new SwordServerException("Method not supported");
    }
}
