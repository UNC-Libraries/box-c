package edu.unc.lib.boxc.indexing.solr;

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class ChildSetRequest extends SolrUpdateRequest {
    private static final long serialVersionUID = 1L;
    private List<PID> children;

    public ChildSetRequest(String newParent, List<String> children, IndexingActionType action, String userID) {
        super(newParent, action, null, userID);
        this.children = new ArrayList<>(children.size());
        for (String childString : children) {
            this.children.add(PIDs.get(childString));
        }
    }

    public List<PID> getChildren() {
        return children;
    }

    public void setChlidren(List<PID> children) {
        this.children = children;
    }
}
