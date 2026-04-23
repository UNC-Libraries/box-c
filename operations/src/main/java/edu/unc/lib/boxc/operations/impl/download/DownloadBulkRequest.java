package edu.unc.lib.boxc.operations.impl.download;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;

public class DownloadBulkRequest {
    private String workPidString;
    private AccessGroupSet principals;

    public DownloadBulkRequest(String workPidString, AccessGroupSet principals) {
        this.workPidString = workPidString;
        this.principals = principals;
    }

    public String getWorkPidString() {
        return workPidString;
    }

    public void setWorkPidString(String workPidString) {
        this.workPidString = workPidString;
    }

    public AccessGroupSet getPrincipals() {
        return principals;
    }

    public void setPrincipals(AccessGroupSet principals) {
        this.principals = principals;
    }
}
