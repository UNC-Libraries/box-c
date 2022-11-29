package edu.unc.lib.boxc.search.api.requests;

import java.util.List;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Request object for a single ID along with access restrictions and requested result data.
 * @author bbpennel
 */
public class SimpleIdRequest {
    protected final PID pid;
    protected List<String> resultFields;
    protected final AccessGroupSet accessGroups;

    public SimpleIdRequest(PID pid, AccessGroupSet accessGroups) {
        this(pid, null, accessGroups);
    }

    public SimpleIdRequest(PID pid, List<String> resultFields, AccessGroupSet accessGroups) {
        this.pid = pid;
        this.accessGroups = accessGroups;
        this.resultFields = resultFields;
    }

    /**
     * @return the pid
     */
    public PID getPid() {
        return pid;
    }

    public String getId() {
        return pid.getId();
    }

    public AccessGroupSet getAccessGroups() {
        return accessGroups;
    }

    public List<String> getResultFields() {
        return resultFields;
    }

    public void setResultFields(List<String> resultFields) {
        this.resultFields = resultFields;
    }
}
