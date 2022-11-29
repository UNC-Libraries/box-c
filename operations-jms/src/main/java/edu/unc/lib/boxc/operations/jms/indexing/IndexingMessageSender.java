package edu.unc.lib.boxc.operations.jms.indexing;

import static edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageHelper.makeIndexingOperationBody;

import java.util.Collection;
import java.util.Map;

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.MessageSender;
/**
 * Constructs and sends JMS messages describing CDR operations related to reindexing
 *
 * @author harring
 *
 */
public class IndexingMessageSender extends MessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingMessageSender.class);

    /**
     * Adds message to JMS queue for object(s) to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param targetPid PID of object to be indexed
     * @param actionType type of indexing action to perform
     */
    public void sendIndexingOperation(String userid, PID targetPid, IndexingActionType actionType) {
        sendIndexingOperation(userid, targetPid, null, actionType, null, null);
    }

    /**
     * Adds message to JMS queue for object(s) to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param targetPid PID of object to be indexed
     * @param children pids of other objects to be indexed
     * @param actionType type of indexing action to perform
     */
    public void sendIndexingOperation(String userid, PID targetPid, Collection<PID> children,
            IndexingActionType actionType) {
        sendIndexingOperation(userid, targetPid, children, actionType, null, null);
    }

    /**
     * Adds message to JMS queue for object(s) to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param targetPid PID of object to be indexed
     * @param children pids of other objects to be indexed
     * @param actionType type of indexing action to perform
     * @param parameters map containing additional parameters to include in the
     *            message
     */
    public void sendIndexingOperation(String userid, PID targetPid, Collection<PID> children,
            IndexingActionType actionType, Map<String, String> parameters, IndexingPriority priority) {
        Document msg = makeIndexingOperationBody(userid, targetPid, children, actionType, parameters, priority);

        LOG.debug("sending solr update message for {} of type {}", targetPid, actionType.toString());
        sendMessage(msg);
        LOG.debug("sent indexing operation JMS message using JMS template: {}", this.getJmsTemplate());
    }
}
