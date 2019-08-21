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

/**
 * Object representing the assignment of a single role to a single principal
 *
 * @author bbpennel
 *
 */
public class RoleAssignment {
    private String principal;
    private UserRole role;

    public RoleAssignment() {
    }

    public RoleAssignment(String principal, String role) {
        this(principal, UserRole.valueOf(role));
    }

    public RoleAssignment(String principal, UserRole role) {
        setPrincipal(principal);
        setRole(role);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserRole)) {
            return false;
        }
        RoleAssignment other = (RoleAssignment) obj;
        return other.principal.equals(principal) && other.role.equals(role);
    }

    /**
     * @return the principal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * @param principal the principal to set
     */
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * @return the role
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * @param role the role to set
     */
    public void setRole(UserRole role) {
        this.role = role;
    }
}
