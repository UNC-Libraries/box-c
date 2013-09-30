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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.DeleteObjectDAO;
import edu.unc.lib.dl.schema.DeleteObjectsRequest;
import edu.unc.lib.dl.schema.DeleteObjectsResponse;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.PathInfoDao;

/**
 * 
 * 
 */
public class DeleteObjectController extends CommonAdminObjectNavigationController {

	private String deleteObjectUrl;

	protected final Logger logger = Logger.getLogger(getClass());

	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {

		logger.debug("onSubmit entry/exit");
		
		return onSubmitInternal(request, response, command, errors);
	}

	protected ModelAndView onSubmitInternal(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {
		Map model = errors.getModel();

		logger.debug("onSubmitInternal entry");
		
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

			GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request, deleteObjectUrl);

			dao.getBreadcrumbs().clear();
			dao.getBreadcrumbs().addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());

			logger.debug("breadcrumbs: "+dao.getBreadcrumbs().size());
			
			dao.getPaths().clear();
			dao.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());

			logger.debug("paths: "+dao.getPaths().size());
		} else {
			DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest();
			
			deleteObjectsRequest.getPid().addAll(Arrays.asList(objects));

			deleteObjectsRequest.setAdmin(request.getRemoteUser());

			logger.debug("Delete user: "+deleteObjectsRequest.getAdmin());
			
			if(logger.isDebugEnabled()) {
				int size = deleteObjectsRequest.getPid().size();
				for(int i = 0; i < size; i++) {
					logger.debug("To be deleted: " + deleteObjectsRequest.getPid().get(i));
				}
			}
			
			DeleteObjectsResponse deleteObjectsResponse = uiWebService.deleteObjects(deleteObjectsRequest);
			
			dao.setMessage(deleteObjectsResponse.getResponse());
			dao.getBreadcrumbs().clear();
			dao.getPaths().clear();
		}
		
		
		model.put("deleteObjectDAO", dao);

		logger.debug("onSubmitInternal exit");
		
		return new ModelAndView("deleteobject", model);
	}

	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		DeleteObjectDAO object = new DeleteObjectDAO();

		logger.debug("formBackingObject entry");
		
		GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request, deleteObjectUrl);
		
		object.getBreadcrumbs().addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());

		object.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());
		
		logger.debug("formBackingObject exit");
		
		return object;
	}

	public String getDeleteObjectUrl() {
		return deleteObjectUrl;
	}

	public void setDeleteObjectUrl(String deleteObjectUrl) {
		this.deleteObjectUrl = deleteObjectUrl;
	}
}
