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
package edu.unc.lib.boxc.web.common.auth;

/**
 * Constants for headers related to HTTP authentication/authorization.
 *
 * @author count0
 */
public class HttpAuthHeaders {
    public static final String SHIBBOLETH_GROUPS_HEADER = "isMemberOf";
    public static final String FORWARDED_GROUPS_HEADER = "forwardedGroups";
    public static final String FORWARDED_MAIL_HEADER = "forwardedMail";

    private HttpAuthHeaders() {
    }
}
