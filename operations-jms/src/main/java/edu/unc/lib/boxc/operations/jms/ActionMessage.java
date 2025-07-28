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
    String getMessageID();

    /**
     * Returns the target of this message.
     *
     * @return
     */
    String getTargetID();

    /**
     * Returns the label for the target of this message
     *
     * @return
     */
    String getTargetLabel();

    void setTargetLabel(String targetLabel);

    /**
     * Returns the unqualified action to be performed on the target
     *
     * @return
     */
    String getAction();

    /**
     * Returns the namespace of the action to be performed on the target
     *
     * @return
     */
    String getNamespace();

    /**
     * Returns the action name qualified by its namespace
     *
     * @return
     */
    String getQualifiedAction();

    /**
     * Returns the time at which this message was created.
     *
     * @return
     */
    long getTimeCreated();

    /**
     * Returns the identifier of the user requesting this action
     *
     * @return
     */
    String getUserID();
}
