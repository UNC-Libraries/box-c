package edu.unc.lib.dl.acl.fcrepo4;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.fedora.PID;

public interface AclFactory {

	/**
	 * Returns an aggregated map of principals to sets of granted roles
	 * 
	 * @param pid
	 * @return
	 */
	public Map<String, Set<String>> getPrincipalRoles(PID pid);
	
	/**
	 * Returns the patron access setting for this object if specified, otherwise
	 * inherit from parent is returned
	 * 
	 * @param pid
	 * @return PatronAccess enumeration value for this object's access setting
	 *         if specified, otherwise parent.
	 */
	public PatronAccess getPatronAccess(PID pid);
	
	/**
	 * Returns the expiration date of an embargo imposed on the object, or null if no embargo is specified
	 * 
	 * @param pid
	 * @return
	 */
	public Date getEmbargoUntil(PID pid);
	
	/**
	 * Returns true if the object specified is marked for deletion.
	 * 
	 * @param pid
	 * @return
	 */
	public boolean isMarkedForDeletion(PID pid);
}
