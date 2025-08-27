package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.PipelineAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * Coordinates the deposit pipeline by listening for messages that request actions
 *
 * @author bbpennel
 */
public class PipelineCoordinator implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineCoordinator.class);
    private DepositPipelineMessageService pipelineMessageService;
    private DepositPipelineStatusFactory pipelineStatusFactory;
    private DefaultMessageListenerContainer jobListenerContainer;
    private DefaultMessageListenerContainer operationListenerContainer;


    @Override
    public void onMessage(Message message) {
        DepositPipelineMessage pipelineMessage;
        try {
            pipelineMessage = pipelineMessageService.fromJson(message);
            PipelineAction action = pipelineMessage.getAction();
            switch (action) {
            case QUIET:
                quietPipeline();
                break;
            case UNQUIET:
                unquietPipeline();
                break;
            case STOP:
                stopPipeline();
                break;
            }
        } catch (Exception e) {
            LOG.error("Error processing pipeline message", e);
        }
    }

    private void quietPipeline() {
        LOG.info("Quieting the deposit pipeline");
        RedisWorkerConstants.DepositPipelineState state = pipelineStatusFactory.getPipelineState();
        if (DepositPipelineState.active.equals(state)) {
            pipelineStatusFactory.setPipelineState(DepositPipelineState.quieted);
            jobListenerContainer.stop();
            operationListenerContainer.stop();
        } else {
            LOG.warn("Cannot quiet deposit pipeline in state {}", state);
        }
    }

    private void unquietPipeline() {
        LOG.info("Unquieting the deposit pipeline");
        RedisWorkerConstants.DepositPipelineState state = pipelineStatusFactory.getPipelineState();
        if (DepositPipelineState.quieted.equals(state)) {
            pipelineStatusFactory.setPipelineState(DepositPipelineState.active);
            jobListenerContainer.start();
            operationListenerContainer.start();
        } else {
            LOG.warn("Cannot unquiet deposit pipeline in state {}", state);
        }
    }


    private void stopPipeline() {
        LOG.info("Stopping the deposit pipeline");
        RedisWorkerConstants.DepositPipelineState state = pipelineStatusFactory.getPipelineState();
        if (DepositPipelineState.shutdown.equals(state)
                || DepositPipelineState.stopped.equals(state)) {
            LOG.warn("Cannot stop deposit pipeline in state {}", state);
        } else {
            pipelineStatusFactory.setPipelineState(DepositPipelineState.stopped);
            jobListenerContainer.shutdown();
            operationListenerContainer.shutdown();
        }
    }

    public void setPipelineMessageService(DepositPipelineMessageService pipelineMessageService) {
        this.pipelineMessageService = pipelineMessageService;
    }

    public void setPipelineStatusFactory(DepositPipelineStatusFactory pipelineStatusFactory) {
        this.pipelineStatusFactory = pipelineStatusFactory;
    }

    public void setJobListenerContainer(DefaultMessageListenerContainer jobListenerContainer) {
        this.jobListenerContainer = jobListenerContainer;
    }

    public void setOperationListenerContainer(DefaultMessageListenerContainer operationListenerContainer) {
        this.operationListenerContainer = operationListenerContainer;
    }
}
