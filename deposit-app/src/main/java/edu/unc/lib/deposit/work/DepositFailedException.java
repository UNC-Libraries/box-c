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
package edu.unc.lib.deposit.work;

/**
 * Thrown whenever the deposit cannot be completed for a given reason.
 * @author count0
 *
 */
public class DepositFailedException extends Throwable {

    /**
     * 
     */
    private static final long serialVersionUID = -4818301461775253637L;

    public DepositFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DepositFailedException(String message) {
        super(message);
    }

    public DepositFailedException(Throwable cause) {
        super(cause);
    }

}
