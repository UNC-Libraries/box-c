package edu.unc.lib.boxc.deposit.impl.jms;

import edu.unc.lib.boxc.deposit.api.PipelineAction;

/**
 * Message containing details about a deposit pipeline action to be executed
 *
 * @author bbpennel
 */
public class DepositPipelineMessage {
    private PipelineAction action;
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PipelineAction getAction() {
        return action;
    }

    public void setAction(PipelineAction action) {
        this.action = action;
    }
}
