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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractWizardFormController;
import org.springframework.web.servlet.support.RequestContext;

import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;

/**
 * 
 * 
 */
public abstract class AbstractUserGroupController extends
		AbstractWizardFormController {
	/** Logger for this class and subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	protected UiWebService uiWebService;

	protected String getUsersSetting = Constants.GET_USERS;

	
	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	public int getInitialPage(HttpServletRequest request) {
		return 0;
	}

	protected int getTargetPage(HttpServletRequest request, Object command,
			Errors errors, int currentPage) {
		if (currentPage == 0) {
			return 1;
		}

		return 0;
	}

	protected Map referenceData(HttpServletRequest request, Object command,
			Errors errors, int page) {
		if (page == 1) {
			UserGroupDAO userGroupRequest = (UserGroupDAO) command;

			if(userGroupRequest.getGroupName() != null) 
				logger.debug("Group: "+userGroupRequest.getGroupName());
			else logger.debug("Group name is NULL");
			
			userGroupRequest.setType(getUsersSetting);

			UserGroupDAO userGroupResponse = uiWebService
					.userGroupOperation(userGroupRequest);

			userGroupRequest.getUsers().addAll(userGroupResponse.getUsers());

			Map model = new HashMap();
			return model;
		}

		return null;
	}

	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		UserGroupDAO userGroupRequest = new UserGroupDAO();

		userGroupRequest.setType(Constants.GET_GROUPS);

		UserGroupDAO userGroupResponse = uiWebService
				.userGroupOperation(userGroupRequest);

		userGroupRequest.getGroups().addAll(userGroupResponse.getGroups());

		return userGroupRequest;
	}

	protected boolean notNull(String value) {
		if ((value == null) || (value.equals(""))) {
			return false;
		}

		return true;
	}

	protected String getUserSuccessMessage(RequestContext requestContext,
			String successMessage, String userName, String groupName) {
		StringBuilder temp = new StringBuilder(32);

		temp.append(requestContext.getMessage(successMessage, "")).append(" ");
		temp.append(requestContext.getMessage("um.group", "")).append(" ");
		temp.append(groupName).append(", ");
		temp.append(requestContext.getMessage("um.user", "")).append(" ");
		temp.append(userName);

		return temp.toString();
	}

	protected ModelAndView processCancel(HttpServletRequest request,
            HttpServletResponse response,
            Object command,
            BindException errors)
     throws Exception
     {
		return new ModelAndView("admin");		
     }
}
