package edu.unc.lib.boxc.operations.impl.acl;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import edu.unc.lib.boxc.auth.api.models.RoleAssignment;

/**
 * Contains details which affect patron access to a resource.
 *
 * @author bbpennel
 *
 */
public class PatronAccessDetails {

    private List<RoleAssignment> roles;
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC", pattern = "yyyy-MM-dd")
    private Date embargo;
    private boolean deleted;

    public List<RoleAssignment> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleAssignment> roles) {
        this.roles = roles;
    }

    public Date getEmbargo() {
        return embargo;
    }

    public void setEmbargo(Date embargo) {
        this.embargo = embargo;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
