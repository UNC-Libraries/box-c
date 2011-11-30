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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.DeleteObjectDAO;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.PathInfoDao;
import edu.unc.lib.dl.schema.PathInfoResponse;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.services.FolderManager;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.UtilityMethods;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

/**
 * 
 * 
 */
public class AbstractItemListingController extends SimpleFormController {

	private UiWebService uiWebService;
	private FolderManager folderManager;
	private AgentFactory agentManager;
	private TripleStoreQueryService tripleStoreQueryService;
	private String deleteObjectUrl;
	private DigitalObjectManager digitalObjectManager;

	
	
	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {
		
		return onSubmitInternal(request, response, command, errors);
	}

	protected ModelAndView onSubmitInternal(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {
		Map model = errors.getModel();

		// get data transfer object if it exists
		DeleteObjectDAO dao = (DeleteObjectDAO) command;
		if (dao == null) {
			dao = new DeleteObjectDAO();
		}
		dao.setMessage(null);

		// get objects to be deleted, if any
		String[] objects = request.getParameterValues("delete");

		// try to delete objects
		if ((objects == null) || (objects.length == 0)) {
			logger.debug("No objects selected");
		} else {
			for (int i = 0; i < objects.length; i++) {
				logger.debug("Item " + i + " pid: " + objects[i]);
			}
			
			try {
				Agent mediator = agentManager.findPersonByOnyen(request.getRemoteUser(), false);

				for (int i = 0; i < objects.length; i++) {
					PID pid = new PID(objects[i]);

					digitalObjectManager.delete(pid, mediator,
							"Deleted through delete object UI");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// create breadcrumbs with delete url
		// if (notNull(request.getQueryString())) {

		getBreadcrumbs(request, dao);
		getChildren(request, dao);

		// } else {
		// logger.debug("Path is null");
		// }

		model.put("deleteObjectDAO", dao);

		return new ModelAndView("deleteobject", model);
	}

	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		DeleteObjectDAO object = new DeleteObjectDAO();

		getBreadcrumbs(request, object);

		getChildren(request, object);

		return object;
	}

	private void getBreadcrumbs(HttpServletRequest request, DeleteObjectDAO dao) {
		dao.getBreadcrumbs().clear();

		IrUrlInfo irUrlInfo = new IrUrlInfo();

		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		PID pathPid = null;

		try {
			pathPid = tripleStoreQueryService.fetchByRepositoryPath(URLDecoder
					.decode(irUrlInfo.getFedoraUrl(), Constants.UTF_8));
			if (pathPid == null) {
				logger.debug("getBreadcrumbs pathPid is NULL");
			} else {
				logger.debug("getBreadcrumbs pathPid is " + pathPid.getPid()
						+ " " + pathPid.getURI());
			}
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
		}

		List<PathInfo> pathList = tripleStoreQueryService
				.lookupRepositoryPathInfo(pathPid);

		PathInfoResponse pathInfoResponse = uiWebService.getBreadcrumbs(pathPid
				.getPid(), "test");

		if ((pathInfoResponse.getPaths().size() - 1) > 0) {

			for (int i = 1; i < pathInfoResponse.getPaths().size(); i++) {
				PathInfoDao pidao = new PathInfoDao();

				pidao.setLabel(pathInfoResponse.getPaths().get(i).getLabel());
				pidao.setPid(pathInfoResponse.getPaths().get(i).getPid());
				pidao.setPath(deleteObjectUrl
						+ pathInfoResponse.getPaths().get(i).getPath());

				dao.getBreadcrumbs().add(pidao);
			}
		}
	}

	private void getChildren(HttpServletRequest request, DeleteObjectDAO dao) {
		dao.getPaths().clear();

		// get children
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		logger.debug("in getChildren");

		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		irUrlInfo.setParameters(Constants.DS_PREFIX + Constants.MD_CONTENTS);
		DataResponse contents = uiWebService.getDataFromIrUrlInfo(irUrlInfo,
				"test");

		if (contents != null) {
			String contentsStr = new String(contents.getDissemination()
					.getStream());

			List<String> childPids = new ArrayList<String>();
			List<String> childUrls = new ArrayList<String>();

			try {
				Document d = new SAXBuilder().build(new ByteArrayInputStream(
						contentsStr.getBytes()));

				Element e = d.getRootElement();

				List<Element> children = (List<Element>) e.getChildren();

				for (Element child : children) {
					Attribute pid = child.getAttribute("pid");
					childPids.add(pid.getValue());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			String parentUrl = request.getRequestURL().toString();
			if (parentUrl.endsWith("/")) {
				parentUrl = parentUrl.substring(0, parentUrl.length() - 1);
			}

			for (String pid : childPids) {
				PathInfoResponse pathInfo = uiWebService.getBreadcrumbs(pid,
						"test");

				PathInfoDao childPath = pathInfo.getPaths().get(
						pathInfo.getPaths().size() - 1);

				childPath.setPath(deleteObjectUrl + childPath.getPath());

				dao.getPaths().add(childPath);
			}
		}
	}

	public UiWebService getUiWebService() {
		return uiWebService;
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	public FolderManager getFolderManager() {
		return folderManager;
	}

	public void setFolderManager(FolderManager folderManager) {
		this.folderManager = folderManager;
	}

	public AgentFactory getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentFactory agentManager) {
		this.agentManager = agentManager;
	}

	private boolean notNull(String value) {
		if ((value == null) || (value.equals(""))) {
			return false;
		}

		return true;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public String getDeleteObjectUrl() {
		return deleteObjectUrl;
	}

	public void setDeleteObjectUrl(String deleteObjectUrl) {
		this.deleteObjectUrl = deleteObjectUrl;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(
			DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}
}
