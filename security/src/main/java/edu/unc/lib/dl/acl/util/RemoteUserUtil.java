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
package edu.unc.lib.dl.acl.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility for retrieving the remote user
 *
 * @author bbpennel
 *
 */
public class RemoteUserUtil {

    public static final String REMOTE_USER = "REMOTE_USER";

    private RemoteUserUtil() {
    }

    /**
     * Retrieve the remote user from the given request.
     *
     * @param request
     * @return remote user or an empty string if none found.
     */
    public static String getRemoteUser(HttpServletRequest request) {
        String user = request.getHeader(REMOTE_USER);
        if (user == null) {
            user = request.getRemoteUser();
        }
        if (user == null) {
            user = "";
        } else {
            user = user.trim();
        }
        return user;
    }
}
