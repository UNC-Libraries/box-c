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
package edu.unc.lib.boxc.fcrepo.utils;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;

/**
 * Starts, commits, keeps alive, and rolls back transactions in Fedora.
 *
 * @author harring
 *
 */
public class TransactionManager {

    private static final String CREATE_TX = "fcr:tx";
    private static final String COMMIT_TX = "fcr:tx/fcr:commit";
    private static final String ROLLBACK_TX = "fcr:tx/fcr:rollback";
    private FcrepoClient client;
    private BinaryTransferService binaryTransferService;

    public FedoraTransaction startTransaction() throws FedoraException {
        if (FedoraTransaction.isStillAlive()) {
            FedoraTransaction rootTx = FedoraTransaction.rootTxThread.get();
            return new FedoraTransaction(rootTx.getTxUri(), this);
        }
        URI repoBase = URI.create(FcrepoPaths.getBaseUri());
        // appends suffix for creating transaction
        URI createTxUri = URI.create(URIUtil.join(repoBase, CREATE_TX));
        URI txUri = null;
        // attempts to create a transaction by making request to Fedora
        try (FcrepoResponse response = getClient().post(createTxUri).perform()) {
            // gets the full transaction uri from response header
            txUri = response.getLocation();
        } catch (IOException | FcrepoOperationFailedException e) {
            throw new FedoraException("Unable to create transaction", e);
        }

        return new FedoraTransaction(txUri, this);
    }

    protected void commitTransaction(URI txUri) {
        URI commitTxUri = URI.create(URIUtil.join(txUri, COMMIT_TX));
        // attempts to commit/save a transaction by making request to Fedora
        try (FcrepoResponse response = getClient().post(commitTxUri).perform()) {
            // gets the full transaction uri from response header
            int statusCode = response.getStatusCode();
            if (statusCode != HttpStatus.SC_NO_CONTENT) {
                throw new FcrepoOperationFailedException(txUri, statusCode,
                        response.getHeaderValues("Status").toString());
            }
        } catch (Exception e) {
            binaryTransferService.rollbackTransaction(txUri);
            throw new FedoraException("Unable to commit transaction", e);
        }
        binaryTransferService.commitTransaction(txUri);
    }

    protected void keepTransactionAlive(URI txUri) {
        URI txUriAlive = URI.create(URIUtil.join(txUri, CREATE_TX));
        // attempts to commit/save a transaction by making request to Fedora
        try (FcrepoResponse response = getClient().post(txUriAlive).perform()) {
            int statusCode = response.getStatusCode();
            if (statusCode != HttpStatus.SC_NO_CONTENT) {
                throw new FcrepoOperationFailedException(txUri, statusCode,
                        response.getHeaderValues("Status").toString());
            }
        } catch (IOException | FcrepoOperationFailedException e) {
            throw new FedoraException("Unable to keep transaction alive", e);
        }
    }

    protected void cancelTransaction(URI txUri) {
        URI txUriCancel = URI.create(URIUtil.join(txUri, ROLLBACK_TX));
        // attempts to commit/save a transaction by making request to Fedora
        try (FcrepoResponse response = getClient().post(txUriCancel).perform()) {
            int statusCode = response.getStatusCode();
            if (statusCode != HttpStatus.SC_NO_CONTENT) {
                throw new FcrepoOperationFailedException(txUri, statusCode,
                        response.getHeaderValues("Status").toString());
            }
        } catch (IOException | FcrepoOperationFailedException e) {
            throw new FedoraException("Unable to cancel transaction", e);
        } finally {
            binaryTransferService.rollbackTransaction(txUri);
        }
    }

    public void setClient(FcrepoClient client) {
        this.client = client;
    }

    public FcrepoClient getClient() {
        return client;
    }

    public void setBinaryTransferService(BinaryTransferService binaryTransferService) {
        this.binaryTransferService = binaryTransferService;
    }
}
