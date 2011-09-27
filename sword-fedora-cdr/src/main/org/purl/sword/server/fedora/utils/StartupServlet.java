package org.purl.sword.server.fedora.utils;

/**
  * Copyright (c) 2007, Aberystwyth University
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *  - Redistributions of source code must retain the above
  *    copyright notice, this list of conditions and the
  *    following disclaimer.
  *
  *  - Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  *  - Neither the name of the Centre for Advanced Software and
  *    Intelligent Systems (CASIS) nor the names of its
  *    contributors may be used to endorse or promote products derived
  *    from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
  * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  *
  * @author Glen Robson
  * @version 1.0
  * Date: 18 October 2007 
  *
  * This servlet sets up the log4j configuration and reads in the properties file
  * for the application.
  *
  * In the web.conf ensure the load-on-startup property is set to 1 so this servlet runs first.
  * The init-properties are shown below:
  *
  * &lt;servlet&gt;
  *     &lt;servlet-name&gt;StartupServlet&lt;/servlet-name&gt;
  *     &lt;servlet-class&gt;org.purl.sword.server.fedora.utils.StartupServlet&lt;/servlet-class&gt;
  *     &lt;init-param&gt;
  *       &lt;param-name&gt;log-config&lt;/param-name&gt;
  *       &lt;param-value&gt;WEB-INF/log4j.xml&lt;/param-value&gt;
  *     &lt;/init-param&gt;
  *     &lt;init-param&gt;
  *       &lt;param-name&gt;project.properties&lt;/param-name&gt;
  *       &lt;param-value&gt;WEB-INF/properties.xml&lt;/param-value&gt;
  *     &lt;/init-param&gt;
  *     &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
  *  &lt;/servlet&gt;
  */

import org.apache.log4j.xml.DOMConfigurator;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

public class StartupServlet extends HttpServlet {
	private static Logger LOG = Logger.getLogger(StartupServlet.class);
	
	protected static HttpServlet SERVLET = null;
	protected static String _properties =	null; 

	public StartupServlet() {
	}

	/**
	 * If you want to run this project from the command line you can
	 * call this method
	 * @param String the properties file
	 */
	public StartupServlet(final String pPropsFile) {
		_properties = pPropsFile;
	}


	/** 
	 * This method loads in the properties file and log4j configuration
	 */ 
	public void init() {
		if (_properties == null) {
			_properties = this.getServletConfig().getInitParameter("project.properties");
			if (SERVLET == null) {	
				SERVLET = this;
			}
			String tLog4jConf = this.getServletConfig().getInitParameter("log-config");
		
			DOMConfigurator.configure(StartupServlet.getRealPath(tLog4jConf));

			System.out.println("loading properties files: " + _properties);
			LOG.debug("Taking log4j config from: " + StartupServlet.getRealPath(tLog4jConf));
		}
	}

	/**
	 * Allows static access to the properties location
	 *
	 * @return String the full path to the properties file
	 */ 
	public static String getPropertiesLocation() {
		System.out.println("Properties Location:" + StartupServlet.getRealPath(_properties));
		return _properties;//StartupServlet.getRealPath(_properties);
	}

	/** 
	 * Allows classes to get real path of files on the file system
	 * @param String path to convert 
	 * @param String the absolute path to a file
	 */ 
	public static String getRealPath(final String pPath) {
		return SERVLET.getServletContext().getRealPath(pPath);
	}
}
