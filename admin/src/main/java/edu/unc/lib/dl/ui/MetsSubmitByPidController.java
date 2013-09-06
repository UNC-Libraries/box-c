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
import edu.unc.lib.dl.util.MetsSubmitByPidDAO;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.UtilityMethods;

public class MetsSubmitByPidController extends CommonAdminObjectNavigationController {
	private String metsIngestObjectUrl;

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
			BindException errors) throws ServletException, IOException {

		return onSubmitInternal(request, response, command, errors);
	}

	protected ModelAndView onSubmitInternal(HttpServletRequest request, HttpServletResponse response, Object command,
			BindException errors) throws ServletException, IOException {
		Map model = errors.getModel();

		boolean noErrors = true;

		RequestContext requestContext = new RequestContext(request);

		// get data transfer object if it exists
		MetsSubmitByPidDAO dao = (MetsSubmitByPidDAO) command;
		if (dao == null) {
			dao = new MetsSubmitByPidDAO();
		}
		dao.setMessage(null);

		MultipartFile file = dao.getFile();

		MetsSubmitIngestObject ingest = new MetsSubmitIngestObject();
		String ingestMessage = null;
		if(dao.getIngestMessage() != null && dao.getIngestMessage().trim().length() > 0) {
			ingestMessage = dao.getIngestMessage().trim();
		}

		if ((file != null) && (file.getSize() > 0)) {

			String fileName = file.getOriginalFilename();
			if(ingestMessage == null) {
				ingestMessage = fileName;
			}

			String extension;

			if (fileName.endsWith(".zip"))
				extension = ".zip";
			else
				extension = ".xml";

			ingest.setFileName(writeFile(file, extension));

			String filePath = dao.getFilePath();

			PID parentPid = new PID(dao.getPid());

			String pidPath = tripleStoreQueryService.lookupRepositoryPath(parentPid);

			if (uiUtilityMethods.notNull(dao.getPid())) {

				if ((filePath != null) && (!filePath.equals(""))) {
					StringBuffer buffer = new StringBuffer(128);
					buffer.append(pidPath);

					if (!filePath.startsWith("/")) {
						buffer.append("/");
					}

					buffer.append(filePath);
					pidPath = buffer.toString();

					try {
						folderManager.createPath(pidPath, dao.getOwnerPid(), request.getRemoteUser());
					} catch (Exception e) {
						if(e.getLocalizedMessage() != null) {
							dao.setMessage(e.getLocalizedMessage().replace("\n", "<br />\n"));
						}
						noErrors = false;
					}
				}

				ingest.setFilePath(pidPath);
			} else {
				ingest.setFilePath(Constants.COLLECTIONS + "/");
			}

			logger.debug("mets file path: " + ingest.getFilePath());

		} else {
			dao.setMessage(requestContext.getMessage("submit.file.missing"));
			noErrors = false;
		}

		logger.debug("noErrors is: " + noErrors);
		logger.debug(dao.getMessage());

		if (noErrors) {
			ingest.setAdminOnyen(request.getRemoteUser());
			ingest.setOwnerPid(dao.getOwnerPid());
			ingest.setParentPid(dao.getPid());
			ingest.setVirusCheck(dao.isVirusCheck());
			ingest.setVirusSoftware(dao.getVirusSoftware());
			ingest.setVirusDate(dao.getVirusDate());
			ingest.setMessage(ingestMessage);

			MetsSubmitIngestObject wsResponse = new MetsSubmitIngestObject();

			wsResponse = uiWebService.metsSubmit(ingest);

			if (wsResponse != null) {

				if (Constants.SUCCESS.equals(wsResponse.getMessage())) {

					String message = requestContext.getMessage("submit.file.added", "");

					dao.setMessage(message + " " + dao.getFile().getOriginalFilename());

					return new ModelAndView("metsubmitbypid", "metsSubmitByPidDAO", dao);

				} else if (Constants.IN_PROGRESS_THREADED.equals(wsResponse.getMessage())) {
					dao.setMessage(requestContext.getMessage("submit.ingest.progress"));
				} else {
					dao.setMessage(wsResponse
							.getMessage().replace("\n", "<br />\n"));

					logger.debug("METS submit failure");
				}

			} else { // something went wrong upstream
				dao.setMessage(requestContext.getMessage("submit.ingest.error"));

				logger.debug("METS submit failure");
			}
		}

		GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request,
				metsIngestObjectUrl);

		dao.getBreadcrumbs().clear();
		dao.getBreadcrumbs().addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());

		dao.getPaths().clear();
		dao.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());

		model.put("metsSubmitByPidDAO", dao);

		return new ModelAndView("metsubmitbypid", model);
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		MetsSubmitByPidDAO object = new MetsSubmitByPidDAO();

		logger.debug("in formBackingObject");

		GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request,
				metsIngestObjectUrl);

		UserGroupDAO userGroupRequest = new UserGroupDAO();
		userGroupRequest.setType(Constants.GET_GROUPS);

		userGroupRequest.setUserName("");

		UserGroupDAO userGroupResponse = uiWebService.userGroupOperation(userGroupRequest);

		object.getGroups().clear();
		object.getGroups().addAll(userGroupResponse.getGroups());

		userGroupRequest.setType(Constants.GET_USERS);

		userGroupRequest.setUserName("");

		userGroupResponse = uiWebService.userGroupOperation(userGroupRequest);

		object.getUsers().clear();
		object.getUsers().addAll(userGroupResponse.getUsers());

		object.getBreadcrumbs().addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());

		object.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());

		String pid = request.getParameter("id");

		logger.debug("pid: " + pid);

		if (pid == null) {
			PID collectionsPid = tripleStoreQueryService.fetchByRepositoryPath(Constants.COLLECTIONS);
			pid = collectionsPid.getPid();
		}

		object.setPid(pid);

		return object;
	}

	public String getMetsIngestObjectUrl() {
		return metsIngestObjectUrl;
	}

	public void setMetsIngestObjectUrl(String metsIngestObjectUrl) {
		this.metsIngestObjectUrl = metsIngestObjectUrl;
	}
}
