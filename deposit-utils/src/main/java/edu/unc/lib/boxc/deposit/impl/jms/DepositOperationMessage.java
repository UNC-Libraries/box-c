package edu.unc.lib.boxc.deposit.impl.jms;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositAction;

/**
 * Message containing details about a deposit operation
 *
 * @author bbpennel
 */
public class DepositOperationMessage {
    private DepositOperation action;
    private String depositId;
    private String username;
    private String jobId;
    private String body;

    public DepositOperationMessage() {
    }

    public DepositOperation getAction() {
        return action;
    }

    public void setAction(DepositOperation action) {
        this.action = action;
    }

    public String getDepositId() {
        return depositId;
    }

    public void setDepositId(String depositId) {
        this.depositId = depositId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
