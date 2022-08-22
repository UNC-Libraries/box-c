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
package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.sparql.SparqlListingHelper;

import java.util.List;

/**
 * Service which provides information about PCDM membership relations in the repository
 *
 * @author bbpennel
 */
public class PcdmMembershipService implements MembershipService {
    private SparqlQueryService sparqlQueryService;

    @Override
    public List<PID> listMembers(PID parentPid) {
        String queryString = String.format("select ?pid where { ?pid <%1$s> <%2$s> }",
                PcdmModels.memberOf.getURI(), parentPid.getRepositoryPath());
        return SparqlListingHelper.listPids(sparqlQueryService, queryString);
    }

    public void setSparqlQueryService(SparqlQueryService sparqlQueryService) {
        this.sparqlQueryService = sparqlQueryService;
    }
}
