package edu.unc.lib.boxc.auth.api.models;

/**
 * Stores the authentication principals for an agent.
 * @author bbpennel
 */
public interface AgentPrincipals {

    /**
     * @return the username
     */
    String getUsername();

    /**
     * @return the namespaced username
     */
    String getUsernameUri();

    /**
     * @return set of all principals for this agent
     */
    AccessGroupSet getPrincipals();

}