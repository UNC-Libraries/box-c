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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.support.RequestContext;

import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;

@Controller
public class AdminController extends MultiActionController {
	/** Logger for this class and subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private UserGroupDAO userGroupRequest;

	private UiWebService uiWebService;


	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	private boolean checkForCancel(HttpServletRequest request) {

		String temp = request.getParameter("_cancel");

		if("Cancel".equals(temp)){
			return true;
		}

		return false;
	}

	@RequestMapping("/admin/")
	public ModelAndView login(HttpServletRequest request,
			HttpServletResponse response) throws Exception {



		Map myModel = new HashMap();
		myModel.put("entries", "");

		return new ModelAndView("admin", "model", myModel);
	}

	@RequestMapping("/admin/status")
	public ModelAndView status(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Map myModel = new HashMap();
		//myModel.put(", "");
		return new ModelAndView("status", "model", myModel);
	}

	@RequestMapping("/admin/createuser")
	public ModelAndView createUser(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest)
			throws Exception {

		if(checkForCancel(request)) return login(request, response);

		return createUserInternal(request, response, userGroupRequest);
	}

	private ModelAndView createUserInternal(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest) {

		if ((notNull(userGroupRequest.getUserName()))
				&& (notNull(userGroupRequest.getOnyen()))) {
			userGroupRequest.setType(Constants.CREATE_USER);
			userGroupRequest.setAdminName(request.getRemoteUser());

			UserGroupDAO userGroupResponse = uiWebService
					.userGroupOperation(userGroupRequest);

			userGroupRequest.setMessage(chooseMessage(request,
					userGroupResponse));
		} else { // no user name
			String msg = new RequestContext(request).getMessage(
					"um.user.noname", "");
			userGroupRequest.setMessage(msg);
		}

		return new ModelAndView("createuser", "userGroupDAO", userGroupRequest);
	}

	@RequestMapping("/admin/creategroup")
	public ModelAndView createGroup(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest)
			throws Exception {


		if(checkForCancel(request)) return login(request, response);

		return createGroupInternal(request, response, userGroupRequest);
	}

	private ModelAndView createGroupInternal(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest) {

		if (notNull(userGroupRequest.getGroupName())) {
			userGroupRequest.setType(Constants.CREATE_GROUP);

			userGroupRequest.setAdminName(request.getRemoteUser());

			UserGroupDAO userGroupResponse = uiWebService
					.userGroupOperation(userGroupRequest);

			userGroupRequest.setMessage(chooseMessage(request,
					userGroupResponse));
		} else { // no group name
			String msg = new RequestContext(request).getMessage(
					"um.group.noname", "");
			userGroupRequest.setMessage(msg);
		}

		// TODO Need to handle duplicate group, null group

		return new ModelAndView("creategroup", "userGroupDAO", userGroupRequest);

	}

	@RequestMapping("/admin/deleteuser")
	public ModelAndView deleteUser(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest)
		throws Exception {



		if(checkForCancel(request)) return login(request, response);

		return deleteUserInternal(request, response, userGroupRequest);
	}

	private ModelAndView deleteUserInternal(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest) {

		if (notNull(userGroupRequest.getPid())) {

			userGroupRequest.setType(Constants.DELETE_USER);
			userGroupRequest.setAdminName(request.getRemoteUser());

			UserGroupDAO userGroupResponse = uiWebService
					.userGroupOperation(userGroupRequest);

			userGroupRequest.setMessage(chooseMessage(request,
					userGroupResponse));
		} else {
			userGroupRequest.setMessage("");
			userGroupRequest.setPid("");
		}

		userGroupRequest.setType(Constants.GET_USERS);

		userGroupRequest.setUserName("");

		UserGroupDAO userGroupResponse = uiWebService
				.userGroupOperation(userGroupRequest);

		userGroupRequest.getUsers().clear();
		userGroupRequest.getUsers().addAll(userGroupResponse.getUsers());

		return new ModelAndView("deleteuser", "userGroupDAO", userGroupRequest);
	}

	@RequestMapping("/admin/deletegroup")
	public ModelAndView deleteGroup(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest)
			throws Exception {



		if(checkForCancel(request)) return login(request, response);

		return deleteGroupInternal(request, response, userGroupRequest);
	}

	private ModelAndView deleteGroupInternal(HttpServletRequest request,
			HttpServletResponse response, UserGroupDAO userGroupRequest) {

		if (notNull(userGroupRequest.getPid())) {

			userGroupRequest.setType(Constants.DELETE_GROUP);
			userGroupRequest.setAdminName(request.getRemoteUser());

			UserGroupDAO userGroupResponse = uiWebService
					.userGroupOperation(userGroupRequest);

			userGroupRequest.setMessage(chooseMessage(request,
					userGroupResponse));

		} else {
			userGroupRequest.setMessage("");
			userGroupRequest.setPid("");
		}

		userGroupRequest.setType(Constants.GET_GROUPS);

		UserGroupDAO userGroupResponse = uiWebService
				.userGroupOperation(userGroupRequest);

		userGroupRequest.getGroups().clear();
		userGroupRequest.getGroups().addAll(userGroupResponse.getGroups());

		return new ModelAndView("deletegroup", "userGroupDAO", userGroupRequest);
	}

	private String chooseMessage(HttpServletRequest request,
			UserGroupDAO response) {
		String result = null;
		String message = response.getMessage();
		String type = response.getType();

		if (notNull(message)) {
			if (Constants.CREATE_USER.equals(type)) {
				if (Constants.SUCCESS.equals(message)) {
					String msg = new RequestContext(request).getMessage(
							"um.user.created", "");

					result = msg + " " + response.getUserName();
				} else if (Constants.EXISTS.equals(message)) {
					result = new RequestContext(request).getMessage(
							"um.user.exists", "");
				}
			}
			if (Constants.CREATE_GROUP.equals(type)) {
				if (Constants.SUCCESS.equals(message)) {
					String msg = new RequestContext(request).getMessage(
							"um.group.created", "");

					result = msg + " " + response.getGroupName();
				} else if (Constants.EXISTS.equals(message)) {
					result = new RequestContext(request).getMessage(
							"um.group.exists", "");
				}
			}
			if (Constants.DELETE_USER.equals(type)) {
				if (Constants.SUCCESS.equals(message)) {
					String msg = new RequestContext(request).getMessage(
							"um.user.deleted", "");

					result = msg + " " + response.getUserName();
				} else if (Constants.EXISTS.equals(message)) {
					result = new RequestContext(request).getMessage(
							"um.user.nonempty", "");
				}
			}
			if (Constants.DELETE_GROUP.equals(type)) {
				if (Constants.SUCCESS.equals(message)) {
					String msg = new RequestContext(request).getMessage(
							"um.group.deleted", "");

					result = msg + " " + response.getGroupName();
				} else if (Constants.EXISTS.equals(message)) {
					result = new RequestContext(request).getMessage(
							"um.group.nonempty", "");
				}
			}

		}

		return result;
	}

	private boolean notNull(String value) {
		if ((value == null) || (value.equals(""))) {
			return false;
		}

		return true;
	}

	public UserGroupDAO getUserGroupRequest() {
		return userGroupRequest;
	}

	public void setUserGroupRequest(UserGroupDAO userGroupRequest) {
		this.userGroupRequest = userGroupRequest;
	}


}