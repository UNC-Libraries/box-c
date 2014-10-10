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
package edu.unc.lib.dl.cdr.services.imaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gregory Jansen
 * 
 */
public class ImageAccessServlet extends HttpServlet {

	private static final Logger LOG = LoggerFactory.getLogger(ImageAccessServlet.class);

	/**
     *
     */
	private static final long serialVersionUID = -7970721747990335238L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// getImage?width=(parm1)&amp;height=(parm2)&amp;format=(parm3)
		// &amp;usenote=(parm4)&amp;purl=(parm5)
		// &amp;dsid=(DATA_JP2)&amp;pid=(pid)
		String pid = req.getParameter("pid");
		String dsid = req.getParameter("dsid");
		String width = req.getParameter("width");
		String height = req.getParameter("height");
		String format = req.getParameter("format");
		String comment = req.getParameter("comment");

		// validate ints
		int w = Integer.parseInt(width);
		int h = Integer.parseInt(height);
		dp("pid", req);
		dp("dsid", req);
		dp("width", req);
		dp("height", req);
		dp("format", req);
		dp("comment", req);
		ImageAccessService service = (ImageAccessService) getServletContext().getAttribute("imageAccessService");
		try (
			InputStream in = service.get(pid, dsid, w, h, format, comment);
			OutputStream out = resp.getOutputStream()
					) {
			byte[] buf = new byte[32 * 1024]; // 32k buffer
			int nRead = 0;
			while ((nRead = in.read(buf)) != -1) {
				out.write(buf, 0, nRead);
			}
			out.flush();
		} catch (Exception e){
			
		}
		
	}

	private void dp(String p, HttpServletRequest req) {
		LOG.debug("got request parameter " + p + ": " + req.getParameter(p));
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO honor HEAD requests to allow client caching
		super.doHead(req, resp);
	}

}
