package edu.unc.lib.boxc.deposit.impl.jms;

import edu.unc.lib.boxc.deposit.api.DepositOperation;

import java.util.Map;
import java.util.Objects;

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
    private Map<String, String> additionalInfo;

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

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DepositOperationMessage message = (DepositOperationMessage) o;
        return action == message.action
                && Objects.equals(depositId, message.depositId)
                && Objects.equals(username, message.username)
                && Objects.equals(jobId, message.jobId)
                && Objects.equals(exceptionClassName, message.exceptionClassName)
                && Objects.equals(exceptionMessage, message.exceptionMessage)
                && Objects.equals(exceptionStackTrace, message.exceptionStackTrace)
                && Objects.equals(additionalInfo, message.additionalInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, depositId, username, jobId, exceptionClassName, exceptionMessage, exceptionStackTrace, additionalInfo);
    }
}
