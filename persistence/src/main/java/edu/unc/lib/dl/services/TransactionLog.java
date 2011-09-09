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
/**
 *
 */
package edu.unc.lib.dl.services;

import org.jdom.Document;

import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.PremisEventLogger;

/**
 * This is a log interface for CDR transactions.  Each transaction consists of an API call
 * to the CDR Digital Object Manager.
 * @author Gregory Jansen
 *
 */
public interface TransactionLog {
    /**
     * Adds a successful transaction to the log.
     * @param methodName name of the method called
     * @param logger the logger of the transaction
     */
    public void log(String methodName, PremisEventLogger logger);
    /**
     * Adds a failed transaction to the log.
     * @param exception the exception
     */
    public void log(String methodName, IngestException exception);
    /**
     * Retrieves transaction logs from the cache.
     * @param maximum number of transactions to retrieve
     * @return a JDOM Document of the last <code>number</code> transactions
     */
    public Document getRecent(int max);
    /**
     * Cleans up and persists the cache, closes all resources.
     */
    public void close();
}
