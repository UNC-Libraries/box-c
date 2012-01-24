/**
 * Copyright 2012 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.security.access;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;

/**
 * @author Gregory Jansen
 *
 */
public class SpoofShibbolethHeadersValve extends ValveBase {
	String remoteUser = null;
	String isMemberOf = null;

	/* (non-Javadoc)
	 * @see org.apache.catalina.valves.ValveBase#invoke(org.apache.catalina.connector.Request, org.apache.catalina.connector.Response)
	 */
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		MessageBytes memb = request.getCoyoteRequest().getMimeHeaders().addValue("isMemberOf");
	   memb.setString(this.getIsMemberOf());
      final String credentials = "credentials";
      final List<String> roles = new ArrayList<String>();
      final Principal principal = new GenericPrincipal(this.getRemoteUser(), credentials, roles);
      request.setUserPrincipal(principal);
	   getNext().invoke(request, response);
	}

	public String getRemoteUser() {
		return remoteUser;
	}

	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}

	public String getIsMemberOf() {
		return isMemberOf;
	}

	public void setIsMemberOf(String isMemberOf) {
		this.isMemberOf = isMemberOf;
	}

}
