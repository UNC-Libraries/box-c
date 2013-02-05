/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.acl.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;

/**
 * @author Gregory Jansen
 * 
 */
public class SpoofShibbolethHeadersValve extends ValveBase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.catalina.valves.ValveBase#invoke(org.apache.catalina.connector
	 * .Request, org.apache.catalina.connector.Response)
	 */
	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute("spoofHeaders") != null) {
			@SuppressWarnings("unchecked")
			Map<String, String> spoofHeaders = (Map<String, String>)session.getAttribute("spoofHeaders");
			MimeHeaders headers = request.getCoyoteRequest().getMimeHeaders();
			for(Entry<String, String> ent : spoofHeaders.entrySet()) {
				headers.removeHeader(ent.getKey());
				MessageBytes memb = headers.addValue(ent.getKey());
				memb.setString(ent.getValue());
			}
			if(spoofHeaders.containsKey("REMOTE_USER")) {
				String remoteUser = spoofHeaders.get("REMOTE_USER");
				final String credentials = "credentials";
				final List<String> roles = new ArrayList<String>();
				final Principal principal = new GenericPrincipal(
						remoteUser, credentials, roles);
				request.setUserPrincipal(principal);
			}
		}
		getNext().invoke(request, response);
	}
}
