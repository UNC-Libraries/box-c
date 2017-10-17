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
package edu.unc.lib.dl.ui.access;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;

/**
 * Stores cached details about a user's highest level of access
 *
 * @author bbpennel
 *
 */
public class AccessLevel {
    private String username;
    private UserRole highestRole;
    private boolean viewAdmin;
    private long cacheAge;

    public AccessLevel(String username) {
        cacheAge = System.currentTimeMillis();
        viewAdmin = false;
        this.username = username;
        this.highestRole = null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getHighestRole() {
        return highestRole;
    }

    public void setHighestRole(UserRole highestRole) {
        this.highestRole = highestRole;
        if (this.highestRole != null && this.highestRole.getPermissions().contains(Permission.viewHidden)) {
            this.viewAdmin = true;
        }
    }

    public boolean isViewAdmin() {
        return viewAdmin;
    }

    public long getCacheAge() {
        return cacheAge;
    }

    public void setCacheAge(long cacheAge) {
        this.cacheAge = cacheAge;
    }
}
