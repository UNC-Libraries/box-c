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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.unc.lib.dl.ui.ws.UiWebService;

@Controller
public class AjaxPathController {
    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    private UiWebService uiWebService;

    @RequestMapping("/ajax/getallpaths")
    public ModelAndView handleRequest(HttpServletRequest request,
	    HttpServletResponse response) throws Exception {

    	logger.debug("**************** in Ajax Path Controller *************");
    	
	List paths = uiWebService.getCollectionPaths("test");

	ModelAndView mav = new ModelAndView(new JsonView());
	mav.addObject("paths", paths);

	return mav;
    }
    
    
    public UiWebService getUiWebService() {
	return uiWebService;
    }

    public void setUiWebService(UiWebService uiWebService) {
	this.uiWebService = uiWebService;
    }
}