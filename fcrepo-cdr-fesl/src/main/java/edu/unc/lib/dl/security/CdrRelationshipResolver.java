/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
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
package edu.unc.lib.dl.security;

import java.util.Map;

import org.fcrepo.server.Server;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.util.RIRelationshipResolver;

/**
 * Relationship resolver which prevents Fedora from issuing a triple store query to get parents using a hierarchy
 * structure not used by the CDR.
 *
 * @author bbpennel
 * @date Apr 9, 2014
 */
public class CdrRelationshipResolver extends RIRelationshipResolver {

	public CdrRelationshipResolver(Server server, Map<String, String> options) throws MelcoeXacmlException {
		super(server, options);
	}

	@Override
	public String buildRESTParentHierarchy(String pid) throws MelcoeXacmlException {
		return "/" + pid;
	}

}
