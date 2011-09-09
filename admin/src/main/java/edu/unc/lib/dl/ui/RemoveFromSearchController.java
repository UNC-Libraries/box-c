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
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.UtilityMethods;

@Controller
public class RemoveFromSearchController {
	protected final Log logger = LogFactory.getLog(getClass());
	private UiWebService uiWebService;

	@RequestMapping("/searchremove/**/*")
	public void streamContent(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		if (logger.isDebugEnabled())
			logger.debug("requestURL " + request.getRequestURL());
		if (logger.isDebugEnabled())
			logger.debug("requestURI " + request.getRequestURI());
		if (logger.isDebugEnabled())
			logger.debug("querystring " + request.getQueryString());

		Map map = request.getParameterMap();

		Set set = map.keySet();

		Object[] keys = set.toArray();

		for (int i = 0; i < keys.length; i++) {
			String[] values = (String[]) map.get(keys[i]);
			for (int j = 0; j < values.length; j++) {
				if (logger.isDebugEnabled())
					logger.debug(i + " " + j + " key: " + keys[i] + " value: "
							+ values[j]);
			}
		}

		uiWebService.removeFromSearchFromIrUrlInfo(irUrlInfo, "test");
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}
}
