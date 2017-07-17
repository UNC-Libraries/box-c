/**
 * Copyright © 2009 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fedorax.server.module.storage.lowlevel.irods;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a REST interface for accessing CDR Reports
 *
 * @author Greg Jansen
 */
public class IrodsStorageLocationServlet extends HttpServlet implements Constants {

    private static final long serialVersionUID = 1345L;

    private static final Logger LOG = LoggerFactory.getLogger(IrodsStorageLocationServlet.class);

    /** Instance of the Fedora Commons Server */
    private Server fcserver = null;

    private IrodsLowlevelStorageModule irodslls = null;

    public static final String ACTION_LABEL = "Datastream Locator Service";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	request.setCharacterEncoding("UTF-8");
	try {
	    String pid = request.getParameter("pid");
	    LOG.info("got PID of " + pid);
	    String path = null;
	    if(pid.contains("+")) {
		path = irodslls.getDatastreamIrodsPath(pid);
	    } else {
		path = irodslls.getFOXMLIrodsPath(pid);
	    }
	    response.setContentType("text/xml; charset=UTF-8");
	    try(PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"))) {
	    	out.append(path);
	    }
	} catch (Throwable th) {
	    throw new InternalError500Exception("", th, request, ACTION_LABEL, "", new String[0]);
	}
    }

    /** Unsupported HTTP Method */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	// unimplemented
	throw new ServletException("Error: HTTP POST method is not supported, please use GET");
    }

    /** Gets the Fedora Server instance. */
    @Override
    public void init() throws ServletException {
	try {
	    fcserver = Server.getInstance(new File(Constants.FEDORA_HOME), false);
	    Module m = fcserver.getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage");
	    if (m instanceof IrodsLowlevelStorageModule) {
		irodslls = (IrodsLowlevelStorageModule) m;
	    } else {
		throw new ServletException("Error, unsupported low-level storage module");
	    }
	} catch (InitializationException ie) {
	    throw new ServletException("Error getting Fedora Server instance: " + ie.getMessage());
	}
    }
}
