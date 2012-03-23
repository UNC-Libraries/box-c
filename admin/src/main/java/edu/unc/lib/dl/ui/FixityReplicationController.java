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

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.FixityReplicationObject;
import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.FixityReplicationDAO;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.util.UtilityMethods;

public class FixityReplicationController extends CommonAdminObjectNavigationController {
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
		FixityReplicationDAO dao = (FixityReplicationDAO) command;
		if (dao == null) {
			logger.debug("FixityReplicationDAO is null");

			dao = new FixityReplicationDAO();
		} else {
			dao.setMessage(null);

			// Get files uploaded
			MultipartFile goodReplicationFile = dao.getGoodReplicationFile();
			MultipartFile badReplicationFile = dao.getBadReplicationFile();
			MultipartFile goodFixityFile = dao.getGoodFixityFile();
			MultipartFile badFixityFile = dao.getBadFixityFile();

			FixityReplicationObject frobject = new FixityReplicationObject();

			/*
			 * MetsSubmitIngestObject ingest = new MetsSubmitIngestObject(); String ingestMessage = null;
			 * if(dao.getIngestMessage() != null && dao.getIngestMessage().trim().length() > 0) { ingestMessage =
			 * dao.getIngestMessage().trim(); }
			 */
			if ((goodReplicationFile != null) && (goodReplicationFile.getSize() > 0)) {

				frobject.setGoodReplicationFileName(writeFile(goodReplicationFile, ".log"));
				frobject.setGoodReplicationFile(goodReplicationFile.getBytes());
			} else {
				logger.debug("good replication file is NULL");
			}
			if ((badReplicationFile != null) && (badReplicationFile.getSize() > 0)) {

				frobject.setBadReplicationFileName(writeFile(badReplicationFile, ".log"));
				frobject.setBadReplicationFile(badReplicationFile.getBytes());
			} else {
				logger.debug("bad replication file is NULL");
			}
			if ((goodFixityFile != null) && (goodFixityFile.getSize() > 0)) {

				frobject.setGoodFixityFileName(writeFile(goodFixityFile, ".log"));
				frobject.setGoodFixityFile(goodFixityFile.getBytes());
			} else {
				logger.debug("good fixity file is NULL");
			}
			if ((badFixityFile != null) && (badFixityFile.getSize() > 0)) {

				frobject.setBadFixityFileName(writeFile(badFixityFile, ".log"));
				frobject.setBadFixityFile(badFixityFile.getBytes());
			} else {
				logger.debug("bad fixity file is NULL");
			}

			try {
				Agent mediator = agentManager.findPersonByOnyen(request.getRemoteUser(), false);

			} catch (Exception e) {
				dao.setMessage(e.getLocalizedMessage().replace("\n", "<br />\n"));
				noErrors = false;
			}

			frobject.setAdminOnyen(request.getRemoteUser());

			FixityReplicationObject wsResponse = new FixityReplicationObject();

			wsResponse = uiWebService.fixityReplication(frobject);

			if (wsResponse != null) {

				if (Constants.SUCCESS.equals(wsResponse.getMessage())) {

					String message = requestContext.getMessage("submit.file.added", "");

					dao.setMessage(message);

					return new ModelAndView("fixityreplication", "fixityReplicationDAO", dao);

				} else if (Constants.IN_PROGRESS_THREADED.equals(wsResponse.getMessage())) {
					dao.setMessage(requestContext.getMessage("submit.ingest.progress"));
				} else {
					dao.setMessage(wsResponse.getMessage().replace("\n", "<br />\n"));

					logger.debug("fixity replication submit failure");
				}

			} else { // something went wrong upstream
				dao.setMessage(requestContext.getMessage("submit.ingest.error"));

				logger.debug("fixity replication submit failure");
			}

		}

		model.put("fixityReplicationDAO", dao);

		return new ModelAndView("fixityreplication", model);
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		FixityReplicationDAO object = new FixityReplicationDAO();

		logger.debug("in formBackingObject");

		return object;
	}
}
