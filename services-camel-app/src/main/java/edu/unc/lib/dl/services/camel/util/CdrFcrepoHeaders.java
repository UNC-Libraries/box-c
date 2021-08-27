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
package edu.unc.lib.dl.services.camel.util;

/**
 * Constants for Apache Camel object enhancement services
 *
 * @author lfarrell
 *
 */
public abstract class CdrFcrepoHeaders {

    public static final String CdrBinaryMimeType = "CdrMimeType";

    public static final String CdrBinaryChecksum = "CdrChecksum";

    public static final String CdrBinaryPidId = "CdrBinaryPidId";

    public static final String CdrObjectType = "CdrObjectType";

    // URI identifying the location of content for a binary
    public static final String CdrBinaryPath = "CdrBinaryPath";

    // URI identifying the location
    public static final String CdrImagePath = "CdrImagePath";

    // File path for a temp file
    public static final String CdrTempPath = "CdrTempPath";

    public static final String CdrUpdateAction = "CdrUpdateAction";

    public static final String CdrEnhancementSet = "CdrEnhancementSet";

    public static final String CdrSolrUpdateAction = "CdrSolrUpdateAction";

    public static final String CdrSolrIndexingPriority = "CdrSolrIndexingPriority";
}
