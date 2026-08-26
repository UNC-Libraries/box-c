package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import org.fcrepo.client.FcrepoClient;

import java.net.URI;
import java.util.List;

/**
 * Service which provides information about PCDM membership relations in the repository
 *
 * @author bbpennel
 */
public class PcdmMembershipService implements MembershipService {
    private FcrepoClient client;

    @Override
    public List<PID> listMembers(PID parentPid) {
        URI parentUri = parentPid.getRepositoryUri();
        return FedoraRelationListingHelper.listSubjectsOfInboundRelations(client, parentUri, parentUri, PcdmModels.memberOf);
    }

    public void setClient(FcrepoClient client) {
        this.client = client;
    }
}
