/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.fcrepo4;

import org.fcrepo.client.FcrepoClient;

/**
 * 
 * @author bbpennel
 *
 */
public class FcrepoClientFactory {

	/**
	 * Construct a TransactionalFcrepoClient object with the supplied authentication information
	 * 
	 * @param host
	 * @param user
	 * @param password
	 * @return
	 */
	public static FcrepoClient makeAuthenticatedClient(String host, String user, String password) {
		return TransactionalFcrepoClient.client()
				.credentials(user, password)
				.authScope(host)
				.throwExceptionOnFailure()
				.build();
	}

	/**
	 * Construct a TransactionalFcrepoClient with exceptions thrown on failure and no authentication.
	 * 
	 * @return
	 */
	public static FcrepoClient makeClient() {
		return TransactionalFcrepoClient.client().throwExceptionOnFailure().build();
	}
}
