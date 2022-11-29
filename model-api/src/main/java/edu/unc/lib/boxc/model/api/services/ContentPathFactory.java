package edu.unc.lib.boxc.model.api.services;

import java.util.List;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Factory for retrieving path information for content objects
 * @author bbpennel
 */
public interface ContentPathFactory {

    /**
     * Returns the list of PIDs for content objects which are parents of the provided
     * PID, ordered from the base of the hierarchy to the immediate parent of the PID.
     *
     * @param pid
     * @return
     */
    List<PID> getAncestorPids(PID pid);

    /**
     * Invalidates cached data for the provided pid
     * @param pid
     */
    void invalidate(PID pid);
}