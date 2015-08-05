package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.List;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerListener;
import net.greghaines.jesque.worker.WorkerPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.model.CDREventMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.JMSMessageUtil;

public class SolrUpdateConductor implements MessageConductor, WorkerListener {

	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateConductor.class);
	
	private net.greghaines.jesque.client.Client jesqueClient;
	private String queueName;
	
	public void setJesqueClient(net.greghaines.jesque.client.Client jesqueClient) {
		this.jesqueClient = jesqueClient;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public void offer(String pid, IndexingActionType action) {
		offer(new SolrUpdateRequest(pid, action));
	}

	public void offer(String pid) {
		offer(new SolrUpdateRequest(pid, IndexingActionType.ADD));
	}

	public void offer(SolrUpdateRequest ingestRequest) {
		String pid = ingestRequest.getPid().getPid();
		String action = ingestRequest.getUpdateAction().toString();
		
		List<String> children = null;
		
		if (ingestRequest instanceof ChildSetRequest) {
			children = new ArrayList<>();
			for (PID p : ((ChildSetRequest) ingestRequest).getChildren()) {
				children.add(p.getPid());
			}
		}
		
		Job job = new Job(SolrUpdateJob.class.getName(), pid, action, children);
		jesqueClient.enqueue(queueName, job);
	}
	
	@Override
	public void add(ActionMessage message) {
		LOG.debug("Adding " + message.getTargetID() + " " + message.getClass().getName() + ": "
				+ message.getQualifiedAction());
		if (message instanceof SolrUpdateRequest) {
			this.offer((SolrUpdateRequest) message);
			return;
		}
		String action = message.getQualifiedAction();

		if (action == null)
			return;

		if (message instanceof FedoraEventMessage) {
			String datastream = ((FedoraEventMessage) message).getDatastream();
			if (JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action)) {
				this.offer(message.getTargetID(), IndexingActionType.DELETE_SOLR_TREE);
			} else if (JMSMessageUtil.FedoraActions.INGEST.equals(action)) {
				this.offer(message.getTargetID(), IndexingActionType.ADD);
			} else if (JMSMessageUtil.FedoraActions.MODIFY_OBJECT.equals(action)) {
				if (!((FedoraEventMessage) message).getArgument("fedora-types:label").equals("null")) {
					this.offer(message.getTargetID(), IndexingActionType.UPDATE_DESCRIPTION);
				}
				if (!((FedoraEventMessage) message).getArgument("fedora-types:state").equals("null")) {
					this.offer(message.getTargetID(), IndexingActionType.UPDATE_STATUS);
				}
			} else if (ContentModelHelper.Datastream.MD_DESCRIPTIVE.equals(datastream)
					&& (JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
							|| JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action)
							|| JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.equals(action) || JMSMessageUtil.FedoraActions.ADD_DATASTREAM
								.equals(action))) {
				this.offer(message.getTargetID(), IndexingActionType.UPDATE_DESCRIPTION);
			} else {
				this.offer(message.getTargetID());
			}
		} else if (message instanceof CDREventMessage) {
			CDREventMessage cdrMessage = (CDREventMessage) message;
			if (JMSMessageUtil.CDRActions.MOVE.equals(action)) {
				SolrUpdateRequest request = new ChildSetRequest(cdrMessage.getTargetID(), cdrMessage.getSubjects(),
						IndexingActionType.MOVE);
				this.offer(request);
			} else if (JMSMessageUtil.CDRActions.ADD.equals(action)) {
				SolrUpdateRequest request = new ChildSetRequest(cdrMessage.getTargetID(), cdrMessage.getSubjects(),
						IndexingActionType.ADD_SET_TO_PARENT);
				this.offer(request);
			} else if (JMSMessageUtil.CDRActions.REORDER.equals(action)) {
				// TODO this is a placeholder until a partial update for reorder is worked out
				for (String pidString : cdrMessage.getReordered()) {
					this.offer(pidString, IndexingActionType.ADD);
				}
			} else if (JMSMessageUtil.CDRActions.INDEX.equals(action)) {
				IndexingActionType indexingAction = IndexingActionType.getAction(IndexingActionType.namespace
						+ cdrMessage.getOperation());
				if (indexingAction != null) {
					if (IndexingActionType.SET_DEFAULT_WEB_OBJECT.equals(indexingAction)) {
						SolrUpdateRequest request = new ChildSetRequest(cdrMessage.getTargetID(), cdrMessage.getSubjects(),
								IndexingActionType.SET_DEFAULT_WEB_OBJECT);
						this.offer(request);
					} else {
						for (String pidString : cdrMessage.getSubjects()) {
							this.offer(pidString, indexingAction);
						}
					}
				}
			} else if (JMSMessageUtil.CDRActions.REINDEX.equals(action)) {
				// Determine which kind of reindex to perform based on the mode
				if (cdrMessage.getMode().equals("inplace")) {
					this.offer(cdrMessage.getParent(), IndexingActionType.RECURSIVE_REINDEX);
				} else {
					this.offer(cdrMessage.getParent(), IndexingActionType.CLEAN_REINDEX);
				}
			} else if (JMSMessageUtil.CDRActions.PUBLISH.equals(action)) {
				for (String pidString : cdrMessage.getSubjects()) {
					this.offer(pidString, IndexingActionType.UPDATE_STATUS);
				}
			} else if (JMSMessageUtil.CDRActions.EDIT_TYPE.equals(action)) {
				SolrUpdateRequest request = new ChildSetRequest(cdrMessage.getTargetID(), cdrMessage.getSubjects(),
						IndexingActionType.UPDATE_TYPE);
				this.offer(request);
			}
			
		} else {
			// For all other message types, do a single record update
			this.offer(message.getTargetID());
		}
	}
	
	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue, Job job, Object runner, Object result, Throwable t) {
		if (event == null || event == WorkerEvent.WORKER_POLL) {
			return;
		}
		
		LOG.debug("onEvent event={}, worker={}, queue={}, job={}, runner={}, result={}, t={}", new Object[] { event, worker, queue, job, runner, result, t });
	
		if (event == WorkerEvent.JOB_FAILURE) {
			LOG.error("Job failed: " + job, t);
		}
	}

	public static final String identifier = "JESQUE_SOLR_UPDATE";

	/**
	 * Returns the identifier string for this conductor
	 * @return
	 */
	@Override
	public String getIdentifier() {
		return identifier;
	}
	
}
