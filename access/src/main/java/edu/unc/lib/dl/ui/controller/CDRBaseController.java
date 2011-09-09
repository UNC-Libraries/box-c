/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.unc.lib.dl.security.access.AccessGroupConstants;
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.security.access.UserSecurityProfile;

/**
 * Common base controller for use in the CDR UI, offers basic security functionality.
 * @author bbpennel
 * $Id: CDRBaseController.java 2736 2011-08-08 20:04:52Z count0 $
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/access/src/main/java/edu/unc/lib/dl/ui/controller/CDRBaseController.java $
 */
public abstract class CDRBaseController {
	protected AccessGroupSet getUserAccessGroups(HttpServletRequest request){
		HttpSession session = request.getSession();
		UserSecurityProfile user = (UserSecurityProfile)session.getAttribute("user");
		if (user == null){
			//If we couldn't get the user, set it to public
			return new AccessGroupSet(AccessGroupConstants.PUBLIC_GROUP);
		}
		return user.getAccessGroups();
	}
	
	protected UserSecurityProfile getUserProfile(HttpServletRequest request){
		HttpSession session = request.getSession();
		UserSecurityProfile user = (UserSecurityProfile)session.getAttribute("user");
		return user;
	}
	
}
