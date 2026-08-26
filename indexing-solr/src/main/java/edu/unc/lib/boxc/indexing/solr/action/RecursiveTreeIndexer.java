package edu.unc.lib.boxc.indexing.solr.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.model.fcrepo.services.FedoraRelationListingHelper;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;

/**
 * Performs depth first indexing of a tree of repository objects, starting at the PID of the provided update request.
 *
 * @author bbpennel
 *
 */
public class RecursiveTreeIndexer {
    private static final Logger log = LoggerFactory.getLogger(RecursiveTreeIndexer.class);

    private IndexingMessageSender messageSender;

    private MembershipService membershipService;

    private FcrepoClient client;

    private Set<String> CONTAINER_TYPES = new HashSet<>(Arrays.asList(Cdr.AdminUnit.getURI(),
            Cdr.Collection.getURI(),
            Cdr.ContentRoot.getURI(),
            Cdr.Folder.getURI(),
            Cdr.Work.getURI()));

    public RecursiveTreeIndexer() {
    }

    /**
     * Index the provided repoObj and all of its children
     * @param repoObj
     * @param actionType Type of indexing action to perform
     * @param userid
     * @throws IndexingException
     */
    public void index(RepositoryObject repoObj, IndexingActionType actionType, String userid)
            throws IndexingException {
        PID pid = repoObj.getPid();
        Set<String> types = repoObj.getResource().listProperties(RDF.type).toList().stream()
                .map(Statement::getResource)
                .map(Resource::getURI)
                .collect(Collectors.toSet());

        index(pid, types, actionType, userid);
    }

    private void index(PID pid, Set<String> types, IndexingActionType actionType, String userid)
            throws IndexingException {
        if (types.contains(Cdr.Tombstone.getURI())) {
            log.debug("Skipping indexing tombstone object {}", pid.getQualifiedId());
            return;
        }

        log.debug("Queueing indexing of {} {}", pid, actionType);
        messageSender.sendIndexingOperation(userid, pid, actionType);

        if (types.stream().anyMatch(CONTAINER_TYPES::contains)) {
            // Start indexing the children
            indexChildren(pid, actionType, userid);
        }
    }

    /**
     * Index all the children of the provided parentPid
     * @param parentPid
     * @param actionType Type of indexing action to perform
     * @param userid
     * @throws IndexingException
     */
    public void indexChildren(PID parentPid, IndexingActionType actionType, String userid)
            throws IndexingException {
        Map<PID, Set<String>> childToTypes = getMembers(parentPid);

        if (childToTypes.size() == 0) {
            return;
        }
        log.debug("Queuing {} children of {} for indexing", childToTypes.size(), parentPid);
        childToTypes.forEach((childPid, types) -> {
            index(childPid, types, actionType, userid);
        });
    }

    /**
     * Retrieves the PIDs of the direct members of parentPid, along with the rdf:type(s) of each
     * member. Types are retrieved via a lightweight HEAD request per member, rather than loading
     * the full RepositoryObject/model for each child.
     */
    private Map<PID, Set<String>> getMembers(PID parentPid) {
        var childPids = membershipService.listMembers(parentPid);

        Map<PID, Set<String>> childToTypes = new HashMap<>();
        for (PID childPid : childPids) {
            Set<String> types = FedoraRelationListingHelper.listTypes(client, childPid.getRepositoryUri());
            childToTypes.put(childPid, types);
        }

        return childToTypes;
    }

    /**
     * @param messageSender the messageSender to set
     */
    public void setIndexingMessageSender(IndexingMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setMembershipService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public void setClient(FcrepoClient client) {
        this.client = client;
    }
}
