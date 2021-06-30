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

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Object representing the assignment of a single role to a single principal
 *
 * @author bbpennel
 *
 */
public class RoleAssignment {
    private String principal;
    private UserRole role;
    private String assignedTo;

    public RoleAssignment() {
    }

    public RoleAssignment(String principal, UserRole role) {
        setPrincipal(principal);
        setRole(role);
    }

    public RoleAssignment(String principal, UserRole role, PID pid) {
        this(principal, role);
        setAssignedTo(pid.getId());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assignedTo == null) ? 0 : assignedTo.hashCode());
        result = prime * result + ((principal == null) ? 0 : principal.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RoleAssignment other = (RoleAssignment) obj;
        if (assignedTo == null) {
            if (other.assignedTo != null) {
                return false;
            }
        } else if (!assignedTo.equals(other.assignedTo)) {
            return false;
        }
        if (principal == null) {
            if (other.principal != null) {
                return false;
            }
        } else if (!principal.equals(other.principal)) {
            return false;
        }
        return role == other.role;
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

    /**
     * @return get the id of the object this role is assigned to
     */
    public String getAssignedTo() {
        return assignedTo;
    }

    /**
     * @param assignedTo the id of the object this role is assigned to
     */
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    @Override
    public String toString() {
        return "RoleAssignment [principal=" + principal + ", role=" + role + ", assignedTo=" + assignedTo + "]";
    }
}
