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
package edu.unc.lib.dl.util;

/**
 * ErrorURIRegistry
 * @author bbpennel
 *
 */
public class ErrorURIRegistry {
    public static final String INVALID_INGEST_PACKAGE = "http://cdr.lib.unc.edu/sword/error/InvalidIngestPackage";
    public static final String INGEST_EXCEPTION = "http://cdr.lib.unc.edu/sword/error/IngestException";
    public static final String UPDATE_EXCEPTION = "http://cdr.lib.unc.edu/sword/error/UpdateException";
    public static final String RETRIEVAL_EXCEPTION = "http://cdr.lib.unc.edu/sword/error/RetrievalException";
    public static final String UNSUPPORTED_PACKAGE_TYPE = "http://cdr.lib.unc.edu/sword/error/UnsupportedPackageType";
    public static final String INSUFFICIENT_PRIVILEGES = "http://cdr.lib.unc.edu/sword/error/InsufficientPrivileges";
    public static final String RESOURCE_NOT_FOUND = "http://cdr.lib.unc.edu/sword/error/ResourceNotFound";

    private ErrorURIRegistry() {
    }
}
