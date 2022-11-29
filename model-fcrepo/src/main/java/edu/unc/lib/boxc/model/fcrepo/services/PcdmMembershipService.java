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
