package edu.unc.lib.boxc.deposit.impl.jms;

import java.util.UUID;

/**
 * Message containing details about a deposit job to be executed
 *
 * @author bbpennel
 */
public class DepositJobMessage {
    private String jobClassName;
    private String jobId = UUID.randomUUID().toString();
    private String depositId;
    private String username;

    public DepositJobMessage() {
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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
}
