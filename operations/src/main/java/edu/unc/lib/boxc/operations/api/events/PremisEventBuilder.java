package edu.unc.lib.boxc.operations.api.events;

import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Interface for builder for creating a PREMIS RDF event.
 * @author bbpennel
 */
public interface PremisEventBuilder {

    /**
     * Add an event detail property to this event
     *
     * @param message
     *            The detail message for this event.
     * @param args
     *            Optional parameters that should be formatted into the message,
     *            using String.format syntax.
     * @return this event builder
     */
    PremisEventBuilder addEventDetail(String message, Object... args);

    /**
     * Add an event outcome property
     *
     * @param success if true, the outcome will be Success, otherwise Fail
     * @return this event builder
     */
    PremisEventBuilder addOutcome(boolean success);

    /**
     * Add a related software agent to this event
     *
     * @param agentPid PID for the agent
     * @return this event builder
     */
    PremisEventBuilder addSoftwareAgent(PID agentPid);

    /**
     * Add a related authorizing agent to this event
     *
     * @param agentPid PID for the agent
     * @return this event builder
     */
    PremisEventBuilder addAuthorizingAgent(PID agentPid);

    /**
     * Add a related implementor agent to this event
     *
     * @param agentPid PID for the agent
     * @return this event builder
     */
    PremisEventBuilder addImplementorAgent(PID agentPid);

    /**
     * Finalize this builder by retrieving the created event resource
     *
     * @return
     */
    Resource create();

    /**
     * Finalize this builder by pushing the built event back to the log
     *
     * @return
     */
    Resource write();

    /**
     * Finalize this builder by pushing the built event back to the log
     * and then closing the logger
     *
     * @return
     */
    Resource writeAndClose();

}