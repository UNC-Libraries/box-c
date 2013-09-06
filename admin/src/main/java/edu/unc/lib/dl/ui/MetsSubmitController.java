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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.MetsSubmitIngestObject;
import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.MediatedSubmitDAO;

/**
 *
 *
 */
public class MetsSubmitController extends AbstractFileUploadController {


	@Override
	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {


		return onSubmitInternal(request, response, command, errors);
	}

	protected ModelAndView onSubmitInternal(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {
		boolean noErrors = true;

		RequestContext requestContext = new RequestContext(request);

		MediatedSubmitDAO dao = (MediatedSubmitDAO) command;

		dao.setMessage(null);

		MultipartFile file = dao.getFile();
		MultipartFile metadata = dao.getMetadata();

		MetsSubmitIngestObject ingest = new MetsSubmitIngestObject();

		if ((file != null) && (file.getSize() > 0)) {

			String fileName = file.getOriginalFilename();
			String extension;

			if (fileName.endsWith(".zip"))
				extension = ".zip";
			else
				extension = ".xml";

			ingest.setFileName(writeFile(file, extension));

			if (uiUtilityMethods.notNull(dao.getFilePath())) {

				String filePath = dao.getFilePath();

				if (!filePath.startsWith(Constants.COLLECTIONS + "/")) {
					if (filePath.equals(Constants.COLLECTIONS)) {
						filePath = Constants.COLLECTIONS + "/";
					} else if (filePath.startsWith("/")) {
						filePath = Constants.COLLECTIONS + dao.getFilePath();
					} else {
						filePath = Constants.COLLECTIONS + "/"
								+ dao.getFilePath();
					}
				}

				try {
					PID writings = folderManager.createPath(filePath,
							dao.getOwnerPid(), request.getRemoteUser());
				} catch (Exception e) {
					e.printStackTrace();
				}

				ingest.setFilePath(filePath);
			} else {
				ingest.setFilePath(Constants.COLLECTIONS + "/");
			}
		} else {
			dao.setMessage(requestContext.getMessage("submit.file.missing"));
			noErrors = false;
		}

		logger.debug("noErrors is: " + noErrors);
		logger.debug(dao.getMessage());

		if (noErrors) {
			ingest.setAdminOnyen(request.getRemoteUser());
			ingest.setOwnerPid(dao.getOwnerPid());
			ingest.setVirusCheck(dao.isVirusCheck());
			ingest.setVirusSoftware(dao.getVirusSoftware());
			ingest.setVirusDate(dao.getVirusDate());

			MetsSubmitIngestObject wsResponse = new MetsSubmitIngestObject();

			wsResponse = uiWebService.metsSubmit(ingest);

			if (wsResponse != null) {

				if (Constants.SUCCESS.equals(wsResponse.getMessage())) {

					String message = requestContext.getMessage(
							"submit.file.added", "");

					dao.setMessage(message + " "
							+ dao.getFile().getOriginalFilename());

					return new ModelAndView("metsubmit", "mediatedSubmitDAO",
							dao);

				} else if (Constants.IN_PROGRESS_THREADED.equals(wsResponse
						.getMessage())) {
					dao.setMessage(requestContext
							.getMessage("submit.ingest.progress"));
				} else {
					dao.setMessage(wsResponse
							.getMessage());
					logger.debug("METS submit failure");
				}

			} else { // something went wrong upstream
				dao
						.setMessage(requestContext
								.getMessage("submit.ingest.error"));

				logger.debug("METS submit failure");
			}
		}

		Map model = errors.getModel();
		model.put("mediatedSubmitDAO", dao);

		return new ModelAndView("metsubmit", model);
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		MediatedSubmitDAO object = new MediatedSubmitDAO();

		logger.debug("in formBackingObject");

		UserGroupDAO userGroupRequest = new UserGroupDAO();
		userGroupRequest.setType(Constants.GET_GROUPS);

		userGroupRequest.setUserName("");

		UserGroupDAO userGroupResponse = uiWebService
				.userGroupOperation(userGroupRequest);

		object.getGroups().clear();
		object.getGroups().addAll(userGroupResponse.getGroups());

		userGroupRequest.setType(Constants.GET_USERS);

		userGroupRequest.setUserName("");

		userGroupResponse = uiWebService.userGroupOperation(userGroupRequest);

		object.getUsers().clear();
		object.getUsers().addAll(userGroupResponse.getUsers());

		return object;
	}
}
