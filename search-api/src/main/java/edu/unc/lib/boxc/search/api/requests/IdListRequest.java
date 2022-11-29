package edu.unc.lib.boxc.search.api.requests;

import java.util.List;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Request object for a a list of IDs
 *
 * @author bbpennel
 *
 */
public class IdListRequest extends SimpleIdRequest {
    private List<String> ids;

    public IdListRequest(List<String> ids, List<String> resultFields, AccessGroupSet accessGroups) {
        super((PID) null, resultFields, accessGroups);
        this.ids = ids;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
