package edu.unc.lib.dl.data.ingest.solr;

/**
 * Message which blocks until its parent message has finished
 * @author bbpennel
 *
 */
public class BlockUntilTargetCompleteRequest extends SolrUpdateRequest {
	private static final long serialVersionUID = 1L;
	private UpdateNodeRequest targetRequest;
	
	public BlockUntilTargetCompleteRequest(String pid, SolrUpdateAction action, String messageID,
			UpdateNodeRequest parent, UpdateNodeRequest target) {
		super(pid, action, messageID, parent);
		this.targetRequest = target;
	}

	@Override
	public boolean isBlocked() {
		return !this.targetRequest.getStatus().equals(ProcessingStatus.FINISHED)
				&& !this.targetRequest.getStatus().equals(ProcessingStatus.FAILED);
	}
}
