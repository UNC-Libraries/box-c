package edu.unc.lib.boxc.model.api.services;

import edu.unc.lib.boxc.model.api.ids.PID;

import java.util.List;

/**
 * Service for accessing repository object member information
 *
 * @author bbpennel
 */
public interface MembershipService {
    /**
     * List the members of the specified object
     * @param parentPid
     * @return List of member PIDs
     */
    List<PID> listMembers(PID parentPid);
}
