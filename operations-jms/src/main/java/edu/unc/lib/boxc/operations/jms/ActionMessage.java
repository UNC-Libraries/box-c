package edu.unc.lib.boxc.operations.jms;

import java.io.Serializable;

/**
 * Action Messages
 * @author bbpennel
 *
 */
public interface ActionMessage extends Serializable {
    /**
     * Returns the identifier for this message
     *
     * @return
     */
    public String getMessageID();

    /**
     * Returns the target of this message.
     *
     * @return
     */
    public String getTargetID();

    /**
     * Returns the label for the target of this message
     *
     * @return
     */
    public String getTargetLabel();

    public void setTargetLabel(String targetLabel);

    /**
     * Returns the unqualified action to be performed on the target
     *
     * @return
     */
    public String getAction();

    /**
     * Returns the namespace of the action to be performed on the target
     *
     * @return
     */
    public String getNamespace();

    /**
     * Returns the action name qualified by its namespace
     *
     * @return
     */
    public String getQualifiedAction();

    /**
     * Returns the time at which this message was created.
     *
     * @return
     */
    public long getTimeCreated();

    /**
     * Returns the identifier of the user requesting this action
     *
     * @return
     */
    public String getUserID();
}
