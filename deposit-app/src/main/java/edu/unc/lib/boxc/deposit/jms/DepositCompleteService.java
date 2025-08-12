package edu.unc.lib.boxc.deposit.jms;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DEPOSIT_RECORD_BASE;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.work.DepositGraphUtils;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for reporting on the completion of a deposit.
 *
 * @author bbpennel
 */
public class DepositCompleteService {
    private static final Logger LOG = LoggerFactory.getLogger(DepositCompleteService.class);
    private DepositModelManager depositModelManager;
    private OperationsMessageSender opsMessageSender;
    private DepositStatusFactory depositStatusFactory;

    /**
     * Send a message indicating that the deposit with the given ID has completed.
     * This will result in an "add" operation being sent for the deposited objects.
     *
     * @param depositId the ID of the deposit that has completed
     */
    public void sendDepositCompleteEvent(String depositId) {
        var depositStatus = depositStatusFactory.get(depositId);
        PID depositPid = PIDs.get(DEPOSIT_RECORD_BASE, depositId);
        try {
            Model model = depositModelManager.getReadModel(depositPid);
            Bag depositBag = model.getBag(depositPid.getRepositoryPath());

            PID destPid = PIDs.get(depositStatus.get(DepositField.containerId.name()));

            List<PID> addedPids = new ArrayList<>();
            NodeIterator childIt = DepositGraphUtils.getChildIterator(depositBag);
            if (childIt == null) {
                LOG.warn("No children nodes found for deposit {} ", depositId);
                return;
            }
            childIt.forEachRemaining(node -> addedPids.add(PIDs.get(node.asResource().getURI())));

            // Send message indicating the deposit has completed
            opsMessageSender.sendAddOperation(depositStatus.get(DepositField.depositorName.name()),
                    List.of(destPid), addedPids, null, depositId);
        } finally {
            depositModelManager.end();
        }
    }

    public void setDepositModelManager(DepositModelManager depositModelManager) {
        this.depositModelManager = depositModelManager;
    }

    public void setOpsMessageSender(OperationsMessageSender opsMessageSender) {
        this.opsMessageSender = opsMessageSender;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }
}
