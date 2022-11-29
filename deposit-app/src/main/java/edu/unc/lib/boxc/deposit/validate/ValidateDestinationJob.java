package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.deposit.work.DepositGraphUtils.getChildIterator;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * Validates that the destination of this deposit is valid for the incoming contents.
 *
 * @author bbpennel
 */
public class ValidateDestinationJob extends AbstractDepositJob {
    private final static Set<Resource> ROOT_CHILD_TYPES = new HashSet<>(asList(Cdr.AdminUnit));
    private final static Set<Resource> ADMINUNIT_CHILD_TYPES = new HashSet<>(asList(Cdr.Collection));
    private final static Set<Resource> COLL_FOLDER_CHILD_TYPES = new HashSet<>(
            asList(Cdr.Folder, Cdr.Work));
    private final static Set<Resource> WORK_CHILD_TYPES = new HashSet<>(asList(Cdr.FileObject));

    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private AccessControlService aclService;

    /**
     * Constructs a new validation job with generated ids
     */
    public ValidateDestinationJob() {
        this(null, null);
    }

    /**
     * Constructs a new validation job
     *
     * @param uuid job uuid
     * @param depositUUID deposit uuid
     */
    public ValidateDestinationJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        PID destPid = getDestinationPID();

        RepositoryObject destObj = repoObjLoader.getRepositoryObject(destPid);

        if (!(destObj instanceof ContentContainerObject)) {
            failJob("Cannot add children to destination", "Cannot deposit to destination " + destPid
                    + ", types does not support children");
        }

        if (destObj instanceof ContentRootObject) {
            assertHasPermission(destPid, Permission.createAdminUnit);
            topLevelObjectsMatchAllowedTypes(destPid, ROOT_CHILD_TYPES, false);
        } else if (destObj instanceof AdminUnit) {
            assertHasPermission(destPid, Permission.createCollection);
            topLevelObjectsMatchAllowedTypes(destPid, ADMINUNIT_CHILD_TYPES, false);
        } else if (destObj instanceof CollectionObject || destObj instanceof FolderObject) {
            assertHasPermission(destPid, Permission.ingest);
            topLevelObjectsMatchAllowedTypes(destPid, COLL_FOLDER_CHILD_TYPES, false);
        } else if (destObj instanceof WorkObject) {
            assertHasPermission(destPid, Permission.ingest);
            topLevelObjectsMatchAllowedTypes(destPid, WORK_CHILD_TYPES, true);
        }
    }

    private void assertHasPermission(PID destPid, Permission perm) {
        Map<String, String> depositStatus = getDepositStatus();
        AccessGroupSet groups = new AccessGroupSetImpl(depositStatus.get(DepositField.permissionGroups.name()));
        String depositor = depositStatus.get(DepositField.depositorName.name());
        AgentPrincipals agent = new AgentPrincipalsImpl(depositor, groups);
        aclService.assertHasAccess(
                "Depositor does not have permissions to ingest to destination " + destPid.getId(),
                destPid, agent.getPrincipals(), perm);
    }

    private void topLevelObjectsMatchAllowedTypes(PID destPid, Set<Resource> allowedTypes, boolean allowNoTypes) {
        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(depositPID.getRepositoryPath());

        NodeIterator iterator = getChildIterator(depositBag);
        // No more children, nothing further to do in this tree
        if (iterator == null) {
            return;
        }

        List<String> errors = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                Resource childResc = (Resource) iterator.next();

                List<Statement> typeStmts = childResc.listProperties(RDF.type).toList();
                if (allowNoTypes && typeStmts.size() == 0) {
                    continue;
                }
                boolean contains = typeStmts.stream()
                        .anyMatch(stmt -> allowedTypes.contains(stmt.getResource()));

                if (!contains) {
                    errors.add(PIDs.get(childResc.getURI()).getId());
                }
            }
        } finally {
            iterator.close();
        }

        if (errors.size() > 0) {
            failJob("Deposit contains invalid object types for deposit into " + destPid.getId(),
                    "Destination " + destPid.getId() + " may only receive children of types ["
                    + allowedTypes.stream().map(Resource::getURI).collect(Collectors.joining(","))
                    + "], but " + errors.size() + " object(s) for deposit do not meet this requirement:\n"
                    + "    " + String.join("\n    ", errors));
        }
    }
}
