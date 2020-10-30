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
package edu.unc.lib.dl.persist.api.transfer;

import java.net.URI;

/**
 * Information describing the outcome of a binary transfer operation
 *
 * @author bbpennel
 */
public interface BinaryTransferOutcome {

    /**
     * @return URI where the binary is stored after the transfer
     */
    URI getDestinationUri();

    /**
     * @return SHA1 calculated of the binary during transfer
     */
    String getSha1();
}
