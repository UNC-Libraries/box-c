/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.cdr.headers;

/**
 * Constants for Apache Camel object enhancement services
 * @author lfarrell
 *
 */
public class CdrFcrepoHeaders {
    private CdrFcrepoHeaders() {
    }

    public static final String CdrBinaryMimeType = "MimeType";

    public static final String CdrBinaryPath = "BinaryPath";

    public static final String CdrBinaryChecksum = "Checksum";

    public static final String CdrBinaryUri = "CdrBinaryUri";

}
