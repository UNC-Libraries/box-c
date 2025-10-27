package edu.unc.lib.boxc.deposit.impl.jms;

import edu.unc.lib.boxc.deposit.api.DepositOperation;

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
    private String exceptionClassName;
    private String exceptionMessage;
    private String exceptionStackTrace;

    public DepositOperationMessage() {
    }

    public DepositOperationMessage(DepositOperation action, String depositId, String username) {
        this.action = action;
        this.depositId = depositId;
        this.username = username;
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

    public String getExceptionClassName() {
        return exceptionClassName;
    }

    public void setExceptionClassName(String exceptionClassName) {
        this.exceptionClassName = exceptionClassName;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionStackTrace() {
        return exceptionStackTrace;
    }

    public void setExceptionStackTrace(String exceptionStackTrace) {
        this.exceptionStackTrace = exceptionStackTrace;
    }
}
