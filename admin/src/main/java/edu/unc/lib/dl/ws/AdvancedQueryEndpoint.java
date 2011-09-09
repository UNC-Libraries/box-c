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
package edu.unc.lib.dl.ws;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.schema.AdvancedQueryRequest;
import edu.unc.lib.dl.schema.AdvancedQueryResponse;

@Endpoint
public class AdvancedQueryEndpoint {
    @PayloadRoot(localPart = "advancedQueryRequest", namespace = "http://www.lib.unc.edu/dlservice/schemas")
    public AdvancedQueryResponse getSearchResult(
	    AdvancedQueryRequest advancedQueryRequest) {

	// QName qnm1 = new QName("SearchResult");
	// JAXBElement<String> returnString = new JAXBElement<String>(qnm1,
	// String.class, "In AdvancedQueryEndpoint");
	System.out.println("In AdvancedQueryEndpoint");

	AdvancedQueryResponse advancedQueryResponse = new AdvancedQueryResponse();
	advancedQueryResponse.setResult("In AdvancedQueryEndpoint");

	return advancedQueryResponse;
    }
}
