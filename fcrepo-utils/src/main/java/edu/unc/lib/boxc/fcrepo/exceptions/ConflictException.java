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
package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * Request failed due to a conflict
 *
 * @author bbpennel
 */
public class ConflictException extends FedoraException {

    /**
     * @param e
     */
    public ConflictException(Exception e) {
        super(e);
    }

    /**
     * @param message
     * @param e
     */
    public ConflictException(String message, Exception e) {
        super(message, e);
    }

    /**
     * @param message
     */
    public ConflictException(String message) {
        super(message);
    }

}
