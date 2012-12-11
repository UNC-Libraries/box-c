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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenRequest;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.PathInfoDao;
import edu.unc.lib.dl.schema.PathInfoResponse;
import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.UtilityMethods;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * CommonAdminObjectNavigationController provides common path processing for Admin UIs dealing with collections
 *
 */
public class CommonAdminObjectNavigationController extends AbstractFileUploadController {

	protected TripleStoreQueryService tripleStoreQueryService;
	protected DigitalObjectManager digitalObjectManager;
	protected String baseUrl;
	protected final Logger logger = Logger.getLogger(getClass());
	
	protected GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildren(HttpServletRequest request, String url){
		IrUrlInfo irUrlInfo = new IrUrlInfo();
		
		logger.debug("getBreadcrumbsAndChildren entry");
		
		String pid = request.getParameter("id");
		
		logger.debug("pid: "+pid);

		if(pid == null) {
			PID collectionsPid = tripleStoreQueryService.fetchByRepositoryPath(Constants.COLLECTIONS);
			pid = collectionsPid.getPid();
		}
		
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);
		
		
		logger.debug("fedoraUrl: "+irUrlInfo.getFedoraUrl());
		
		String fedoraUrl = irUrlInfo.getFedoraUrl();
	
		logger.debug("requestURI: "+request.getRequestURI());
		logger.debug("decodedURL: "+irUrlInfo.getDecodedUrl());
				
		if(irUrlInfo.getFedoraUrl().length() < Constants.COLLECTIONS.length()) irUrlInfo.setFedoraUrl(Constants.COLLECTIONS);
		if(irUrlInfo.getDecodedUrl().indexOf(Constants.COLLECTIONS) < 0) irUrlInfo.setDecodedUrl(irUrlInfo.getDecodedUrl()+Constants.COLLECTIONS);			
	
        logger.debug("fedoraUrl: "+irUrlInfo.getFedoraUrl());
		logger.debug("decodedURL: "+irUrlInfo.getDecodedUrl());
		
		
		// GetBreadcrumbsAndChildrenRequest getBreadcrumbsAndChildrenRequest = new GetBreadcrumbsAndChildrenRequest();
		

		logger.debug("getBreadcrumbsAndChildren exit");
		
		return uiWebService.getBreadcrumbsAndChildren(irUrlInfo, url, GroupsThreadStore.getGroups().toString(), pid);		
	}
	
	/**
	 * 
	 * @param request the HTTPServletRequest object
	 * @param url 
	 * @return List of PathInfoDao
	 */
	protected List<PathInfoDao> getBreadcrumbs(HttpServletRequest request, String url) {
		List<PathInfoDao> list = new ArrayList<PathInfoDao>();

		IrUrlInfo irUrlInfo = new IrUrlInfo();

		logger.debug("getBreadcrumbs entry");
		
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		logger.debug("getBreadcrumbs uri: "+irUrlInfo.getUri());
		logger.debug("getBreadcrumbs fedoraUrl: "+irUrlInfo.getFedoraUrl());
		logger.debug("getBreadcrumbs passed-in url: "+url);
		
		if(!irUrlInfo.getFedoraUrl().startsWith(baseUrl)) {
			irUrlInfo.setFedoraUrl(baseUrl);
		}
		
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

		PathInfoResponse pathInfoResponse = uiWebService.getBreadcrumbs(pathPid
				.getPid(), "test");
		
		if ((pathInfoResponse.getPaths().size() - 1) > 0) {
			
			for (int i = 1; i < pathInfoResponse.getPaths().size(); i++) {
				PathInfoDao pidao = new PathInfoDao();

				pidao.setLabel(pathInfoResponse.getPaths().get(i).getLabel());
				pidao.setPid(pathInfoResponse.getPaths().get(i).getPid());
				pidao.setPath(url
						+ pathInfoResponse.getPaths().get(i).getPath());
				
				list.add(pidao);
			}
		}

		if(logger.isDebugEnabled()) {
			for(PathInfoDao pidao : list){
				logger.debug("getBreadcrumbs label: "+pidao.getLabel()+" Pid: "+pidao.getPid()+" path: "+pidao.getPath());				
			}
		}
		
		logger.debug("getBreadcrumbs exit");
		
		return list;
	}

	protected List<PathInfoDao> getChildren(HttpServletRequest request, String url) {
		List<PathInfoDao> list = new ArrayList<PathInfoDao>();

		logger.debug("getChildren entry");
		
		// get children
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		logger.debug("getChildren uri: "+irUrlInfo.getUri());
		logger.debug("getChildren fedoraUrl: "+irUrlInfo.getFedoraUrl());
		logger.debug("getChildren passed-in url: "+url);
		
		if(!irUrlInfo.getFedoraUrl().startsWith(baseUrl)) {
			irUrlInfo.setFedoraUrl(baseUrl);
		}

		irUrlInfo.setParameters(Constants.DS_PREFIX + Constants.MD_CONTENTS);
		DataResponse contents = uiWebService.getDataFromIrUrlInfo(irUrlInfo,
				"test");

		if (contents != null) {
			String contentsStr = new String(contents.getDissemination()
					.getStream());

			List<String> childPids = new ArrayList<String>();
//			List<String> childUrls = new ArrayList<String>();

			logger.debug("getChildren before parsing MD_CONTENTS");

			try {
				Document d = new SAXBuilder().build(new ByteArrayInputStream(
						contentsStr.getBytes()));

				Element parentDiv = d.getRootElement().getChild("div", JDOMNamespaceUtil.METS_NS);
				List<Element> childDivs = parentDiv.getChildren();
//				ArrayList<PID> order = new ArrayList<PID>();

				for (Element child : childDivs) {
					childPids.add(child.getAttributeValue("ID"));
			    }

			} catch (Exception e) {
				e.printStackTrace();
			}
			logger.debug("getChildren after parsing MD_CONTENTS");

//			String parentUrl = request.getRequestURL().toString();
//			if (parentUrl.endsWith("/")) {
//				parentUrl = parentUrl.substring(0, parentUrl.length() - 1);
//			}

			
			logger.debug("getChildren before loop to get child paths");

			for (String pid : childPids) {
				PathInfoResponse pathInfo = uiWebService.getBreadcrumbs(pid,
						"test");

				PathInfoDao childPath = pathInfo.getPaths().get(
						pathInfo.getPaths().size() - 1);

				childPath.setPath(url + childPath.getPath());

				list.add(childPath);
			}
		}

		if(logger.isDebugEnabled()) {
			for(PathInfoDao childPath : list) {
				logger.debug("getChildren childPath: "+childPath.getPath());
			}
		}
		
		logger.debug("getChildren exit");
		
		return list;
	}


	
	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(
			DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
}
