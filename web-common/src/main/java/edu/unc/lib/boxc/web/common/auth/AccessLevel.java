package edu.unc.lib.boxc.web.common.auth;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;

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
