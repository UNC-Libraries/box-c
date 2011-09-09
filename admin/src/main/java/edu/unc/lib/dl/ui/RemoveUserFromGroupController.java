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
/**
 * 
 */
package edu.unc.lib.dl.ui;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.util.Constants;

/**
 * 
 * 
 */
public class RemoveUserFromGroupController extends AbstractUserGroupController {

	public RemoveUserFromGroupController() {
		setCommandName("userGroupDAO");
		setPages(new String[] { "removeuserfromgroup0", "removeuserfromgroup1",
				"removeuserfromgroup2" });

		getUsersSetting = Constants.GET_USERS_IN_GROUP;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractWizardFormController#processFinish(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView processFinish(HttpServletRequest arg0,
			HttpServletResponse arg1, Object arg2, BindException arg3)
			throws Exception {

		
		UserGroupDAO userGroupRequest = (UserGroupDAO) arg2;

		if (notNull(userGroupRequest.getPid())
				&& notNull(userGroupRequest.getGroupPid())) {

			userGroupRequest.setType(Constants.REMOVE_USER_FROM_GROUP);
			userGroupRequest.setAdminName(arg0.getRemoteUser());

			UserGroupDAO userGroupResponse = uiWebService
					.userGroupOperation(userGroupRequest);

			if ((notNull(userGroupResponse.getMessage()))
					&& (Constants.SUCCESS
							.equals(userGroupResponse.getMessage()))) {
				RequestContext requestContext = new RequestContext(arg0);

				String message = getUserSuccessMessage(requestContext,
						"um.user.removed", userGroupResponse.getUserName(),
						userGroupResponse.getGroupName());

				userGroupRequest.setMessage(message);

				return new ModelAndView("removeuserfromgroup2", "userGroupDAO",
						userGroupRequest);

			} else { // something went wrong
				// TODO: handle errors/results

				logger.debug("Remove user from group failure");
			}
		}

		return new ModelAndView("removeuserfromgroup2", "userGroupDAO",
				new UserGroupDAO());
	}
}
