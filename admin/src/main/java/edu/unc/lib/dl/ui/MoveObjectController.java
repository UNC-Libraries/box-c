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
import javax.servlet.http.HttpSession;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.MoveObjectDAO;
import edu.unc.lib.dl.schema.MoveObjectRequest;

/**
 * 
 * 
 */
public class MoveObjectController extends CommonAdminObjectNavigationController {

	private String moveObjectUrl;

	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {


		return onSubmitInternal(request, response, command, errors);
	}

	protected ModelAndView onSubmitInternal(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {
		// get the collection pid
		Id collectionId;		
		IrUrlInfo collectionInfo = new IrUrlInfo();
		collectionInfo.setFedoraUrl("/Collections");
		collectionId = uiWebService.getIdFromIrUrlInfo(collectionInfo, "test");
		
				
		
		Map model = errors.getModel();

		// get data transfer object if it exists
		MoveObjectDAO dao = (MoveObjectDAO) command;
		if (dao == null) {
			dao = new MoveObjectDAO();
		}
		dao.setMessage(null);
		dao.getBreadcrumbs().clear();
		dao.getPaths().clear();
		dao.setCollectionPath("/Collections");
		dao.setCollectionPid(collectionId.getPid());

		// get objects to be deleted, if any
		String[] objects = request.getParameterValues("movefrom");
		if ((objects != null) && (objects.length > 0)) {
		
			GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request, moveObjectUrl);

			HttpSession session = request.getSession();
			session.setAttribute("move", objects);

			dao.setGroupType("radio");
			dao.setGroupName("parent");
			dao.getBreadcrumbs().addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());
			dao.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());

			session.setAttribute("moveDao", dao);
		}

		HttpSession session = request.getSession();
		String[] moveObjects = (String[]) session.getAttribute("move");

		String[] parents = request.getParameterValues("parent");

		// try to move objects
		if (((parents != null) && (parents.length > 0))
				&& ((moveObjects != null) && (moveObjects.length > 0))) {

			MoveObjectRequest moveObjectRequest = new MoveObjectRequest();

			moveObjectRequest.setAdminOnyen(request.getRemoteUser());
			moveObjectRequest.setParent(parents[0]);
			moveObjectRequest.getChildren().addAll(Arrays.asList(moveObjects));

			session.removeAttribute("move");
			session.removeAttribute("parent");
			dao.setGroupType("checkbox");
			dao.setGroupName("movefrom");

			uiWebService.moveObjects(moveObjectRequest);
			return new ModelAndView("moveobjectsuccess", model);
		} 
		
		model.put("moveObjectDAO", dao);
		
		return new ModelAndView("moveobject", model);
	}

	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {

		logger.debug("formBackingObject called");

		HttpSession session = request.getSession();
		MoveObjectDAO moveDao = (MoveObjectDAO) session.getAttribute("moveDao");

		if (moveDao == null) {

			moveDao = new MoveObjectDAO();

			moveDao.setGroupType("checkbox");
			moveDao.setGroupName("movefrom");
		}

		GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request, moveObjectUrl);
		
		moveDao.getBreadcrumbs().clear();
		moveDao.getPaths().clear();

		moveDao.getBreadcrumbs().addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());

		moveDao.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());

		session.setAttribute("moveDao", moveDao);

		return moveDao;
	}

	public String getMoveObjectUrl() {
		return moveObjectUrl;
	}

	public void setMoveObjectUrl(String moveObjectUrl) {
		this.moveObjectUrl = moveObjectUrl;
	}
}
