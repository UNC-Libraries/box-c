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

import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.springframework.web.servlet.View;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JsonView implements View {
    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    public void render(Map map, HttpServletRequest request,
    HttpServletResponse response) throws Exception {
    	
    	logger.debug("<<<<<<<<<<<<<<<< in JsonView >>>>>>>>>>>>>>>>>>>>>");
    	
        JSONObject jsonObject = JSONObject.fromObject(map);
        PrintWriter writer = response.getWriter();
        writer.write(jsonObject.toString());
    }

	public String getContentType() {
		return "application/json; charset=UTF-8";
	}
 
}