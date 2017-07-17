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
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.ObjectNotFoundException;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.NotFound404Exception;
import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a REST interface for accessing CDR Reports
 *
 * @author Greg Jansen
 */
public class ReportServlet extends HttpServlet implements Constants {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(ReportServlet.class);

	/** Instance of the Fedora Commons Server */
	private static Server fcserver = null;

	public static final String ACTION_LABEL = "CDR Report";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		try {
			Context context = ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri, request);
			PremisReport bean = new PremisReport(fcserver, context);
			String pid = URLDecoder.decode(request.getParameter("pid"), "utf-8");
			LOG.info("got PID of " + pid);

			// Note: Fedora APIM security check performed by container
			// String sessionToken = request.getParameter("sessionToken");

			// returns 404 for unrecognized PIDs
			Document report = null;
			try {
				report = bean.getXMLReport(pid);
			} catch (ObjectNotFoundException e) {
				throw new NotFound404Exception("Object Not Found", e, request, ACTION_LABEL, "", new String[0]);
			}

			response.setContentType("text/xml; charset=UTF-8");

			try(PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"))) {
				new XMLOutputter().output(report, out);
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
		} catch (InitializationException ie) {
			throw new ServletException("Error getting Fedora Server instance: " + ie.getMessage());
		}
	}
}
