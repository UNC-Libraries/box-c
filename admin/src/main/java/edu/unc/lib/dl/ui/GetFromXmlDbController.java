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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.unc.lib.dl.schema.GetFromXmlDbResponse;
import edu.unc.lib.dl.ui.ws.UiWebService;

@Controller
public class GetFromXmlDbController {
	protected final Log logger = LogFactory.getLog(getClass());
	private UiWebService uiWebService;

	@RequestMapping("/xml/**/*")
	public void streamContent(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		GetFromXmlDbResponse xmlResponse = uiWebService.queryXmlDb(request,
				"test");

		response.setHeader("Content-Type", "text/xml");
		response.getOutputStream().println(xmlResponse.getResponse());
		response.getOutputStream().flush();
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}
}
