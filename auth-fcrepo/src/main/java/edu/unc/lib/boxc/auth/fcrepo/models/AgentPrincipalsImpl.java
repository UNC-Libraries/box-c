package edu.unc.lib.boxc.auth.fcrepo.models;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.USER_NAMESPACE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;

/**
 * Stores the authentication principals for an agent.
 *
 * @author bbpennel
 *
 */
public class AgentPrincipalsImpl implements AgentPrincipals {

    private String username;
    private AccessGroupSet principals;

    public AgentPrincipalsImpl() {
    }

    /**
     * Constructs an AgentPrincipals object
     *
     * @param username
     * @param principals
     */
    public AgentPrincipalsImpl(String username, AccessGroupSet principals) {
        this.username = username;
        if (principals == null) {
            this.principals = new AccessGroupSetImpl();
        } else {
            this.principals = new AccessGroupSetImpl(principals);
        }
        if (!isBlank(username)) {
            this.principals.add(getUsernameUri());
        }
    }

    /**
     * Construct an AgentPrincipals object from credentials stored in the
     * current thread.
     *
     * @return new AgentPrincipals object
     */
    public static AgentPrincipals createFromThread() {
        return GroupsThreadStore.getAgentPrincipals();
    }

    /**
     * @return the username
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * @return the namespaced username
     */
    @Override
    @JsonIgnore
    public String getUsernameUri() {
        return USER_NAMESPACE + username;
    }

    /**
     * @return set of all principals for this agent
     */
    @Override
    public AccessGroupSet getPrincipals() {
        return principals;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPrincipals(Collection<String> principals) {
        this.principals = new AccessGroupSetImpl(principals);
    }
}
