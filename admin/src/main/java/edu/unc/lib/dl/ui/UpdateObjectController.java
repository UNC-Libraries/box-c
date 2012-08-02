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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.stream.StreamSource;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.DatastreamDef;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.ListDatastreamsResponse;
import edu.unc.lib.dl.schema.SingleUpdateFile;
import edu.unc.lib.dl.schema.UpdateIngestObject;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.UpdateFileDAO;
import edu.unc.lib.dl.util.UpdateObjectDAO;
import edu.unc.lib.dl.util.UtilityMethods;

public class UpdateObjectController extends
		CommonAdminObjectNavigationController {
	private String updateObjectUrl;
	
	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {

		return onSubmitInternal(request, response, command, errors);
	}
	
	protected ModelAndView onSubmitInternal(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException, IOException {
		Map model = errors.getModel();

		logger.warn("submit value: "+(String)request.getParameter("submit"));

		// get data transfer object if it exists
		UpdateObjectDAO dao = (UpdateObjectDAO) command;
		if (dao == null) {
			dao = new UpdateObjectDAO();
		}
		
		String submit = (String) request.getParameter("submit");
		if(submit.equals("Edit MODS")) {
			// get PID from session
			String pid = dao.getPid();
			
			HttpSession session = request.getSession();
			session.setAttribute("pid", dao.getPid());
			
			// call mods editor
		
			return new ModelAndView("modseditor", model);
			
		} else if(submit.equals("Submit MODS")) {
			// get PID from session
			
			logger.warn("Submit MODS reached");
			
			// get MODS from session
			
			// clear out MODS from session
			
			// transform MODS
			
			// try to put MODS to SWORD
		
		
			
		} else if(submit.equals("Edit Access Control")) {
			// get PID from session
			String pid = dao.getPid();
			
			HttpSession session = request.getSession();
			session.setAttribute("pid", dao.getPid());
			
			return new ModelAndView("aceditor", model);
		} else if(submit.equals("Submit Access Control")) {
			// get PID from Session
			
			// get access control xml from session
			
			// clear out access control xml from session
			
			// convert into triples
			
			// try to get RELS-EXT from SWORD
			
			// extract access control from RELS-EXT
			
			// put new access control into RELS-EXT
			
			// submit RELS-EXT to SWORD
			
		}
		
		
		dao.setMessage(null);

		try {
			UpdateIngestObject updateIngestObject = new UpdateIngestObject();
			
			updateIngestObject.setAdminOnyen(request.getRemoteUser());
			updateIngestObject.setPid(dao.getPid());
						
			// get metadata object
			// see if any changes
			if ((dao.getMetadata() != null) && (dao.getMetadata().getSourceFile() != null)) {
				updateIngestObject.setMetadata(dao.getMetadata().getSourceFile().getBytes());				
			} else {
				logger.debug("dao.getMetadata() is NULL");
			}
			// for each file object
			// see if any changes
			if (dao.getFiles() != null) {

				for (UpdateFileDAO afile : dao.getFiles()) {
					logger.debug("file id is: " + afile.getId());
					logger.debug("label is: " + afile.getLabel());
					
					SingleUpdateFile singleUpdateFile = new SingleUpdateFile();
					
					MultipartFile file = afile.getSourceFile();
					if ((file != null) && (file.getSize() > 0)) {
						singleUpdateFile.setFile(writeFile(file, ".cdr"));
					}
					
					singleUpdateFile.setDsId(afile.getId());
					singleUpdateFile.setFileName(afile.getSourceFile().getOriginalFilename());
					singleUpdateFile.setMimetype(afile.getSourceFile().getContentType());
					singleUpdateFile.setLabel(afile.getLabel());
					singleUpdateFile.setOriginalLabel(afile.getOriginalLabel());
					
					if((afile.getChecksum() == null) || (afile.getChecksum() == ""))
						singleUpdateFile.setChecksum(null);
					else singleUpdateFile.setChecksum(afile.getChecksum());
					
					updateIngestObject.getFiles().add(singleUpdateFile);
				}
			} else {
				logger.debug("dao.getFiles() is NULL");
			}
			
			uiWebService.update(updateIngestObject);
			
			// digitalObjectManager
		} catch (Exception e) {
			e.printStackTrace();
		}


		GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request, updateObjectUrl);

		dao.getBreadcrumbs().clear();
		dao.getBreadcrumbs().addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());

		dao.getPaths().clear();
		dao.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());

		model.put("updateObjectDAO", dao);

		return new ModelAndView("updateobject", model);
	}

	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		UpdateObjectDAO object = new UpdateObjectDAO();

		GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildrenResponse = getBreadcrumbsAndChildren(request, updateObjectUrl);

		object.getBreadcrumbs()
				.addAll(getBreadcrumbsAndChildrenResponse.getBreadcrumbs());

		object.getPaths().addAll(getBreadcrumbsAndChildrenResponse.getChildren());

		getDatastreams(object, request);

		return object;
	}

	private void getDatastreams(UpdateObjectDAO object,
			HttpServletRequest request) {

		String pid = request.getParameter("id");
		
		logger.debug("pid: "+pid);

		if(pid == null) {
			return;
		}

		object.setPid(pid);
		
		// get datastreams list to check for DC and MODS and MD_CONTENTS
		ListDatastreamsResponse listDatastreamsResponse = uiWebService
				.getDatastreams(pid, "test");

		for (DatastreamDef def : listDatastreamsResponse.getDatastreamDef()) {
			logger.debug("pid: " + pid + " datastream: " + def.getID()
					+ " label: " + def.getLabel());

			if (Constants.MD_DESCRIPTIVE.equals(def.getID())) {

				UpdateFileDAO file = new UpdateFileDAO();
				file.setId(def.getID());

				object.setMetadata(file);

			} else if (def.getID().startsWith("DATA_")) {

				UpdateFileDAO file = new UpdateFileDAO();
				file.setId(def.getID());
				file.setLabel(def.getLabel());
				file.setOriginalLabel(def.getLabel());

				object.getFiles().add(file);
			}
		}
		
		if(object.getMetadata() == null) {
			UpdateFileDAO file = new UpdateFileDAO();
			file.setId(Constants.MD_DESCRIPTIVE);

			object.setMetadata(file);			
		}
	}

	public String getUpdateObjectUrl() {
		return updateObjectUrl;
	}

	public void setUpdateObjectUrl(String updateObjectUrl) {
		this.updateObjectUrl = updateObjectUrl;
	}

}
