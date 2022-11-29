package edu.unc.lib.boxc.auth.api.services;

import java.util.Set;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Interface for services retrieving access control information for objects.
 *
 * @author count0
 *
 */
public interface AccessControlService {
    /**
     * Returns true if the given groups have the specified permission on the
     * object pid
     *
     * @param pid
     *            PID identifying the object to evaluate the access of
     * @param principals
     *            Access control principals which identify the agent being
     *            evaluated
     * @param permission
     *            The permission being tested
     * @return
     */
    public boolean hasAccess(PID pid, Set<String> principals, Permission permission);

    /**
     * Throws an AccessRestrictionException if the given groups do not have the
     * specified permission on the object pid
     *
     * @param pid
     *            PID identifying the object to make this assertion against
     * @param principals
     *            Access control principals which identify the agent being
     *            asserted
     * @param permission
     *            The permission being tested
     *
     * @throws AccessRestrictionException
     */
    public void assertHasAccess(PID pid, Set<String> principals, Permission permission)
            throws AccessRestrictionException;

    /**
     * Throws an AccessRestrictionException if the given groups do not have the
     * specified permission on the object pid
     *
     * @param message
     *            The identifying message for the thrown
     *            AccessRestrictionException
     * @param pid
     *            PID identifying the object to make this assertion against
     * @param principals
     *            Access control principals which identify the agent being
     *            asserted
     * @param permission
     *            The permission being tested
     *
     * @throws AccessRestrictionException
     */
    public void assertHasAccess(String message, PID pid, Set<String> principals, Permission permission)
            throws AccessRestrictionException;
}
