package edu.unc.lib.boxc.operations.impl.delete;

import static edu.unc.lib.boxc.auth.api.Permission.markForDeletion;
import static edu.unc.lib.boxc.auth.api.Permission.markForDeletionUnit;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.markedForDeletion;
import static edu.unc.lib.boxc.model.api.sparql.SparqlUpdateHelper.createSparqlDelete;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;

/**
 * Operation which restores a content object which was marked for deletion
 *
 * @author bbpennel
 *
 */
public class RestoreDeletedJob implements Runnable {

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private SparqlUpdateService sparqlUpdateService;
    private PremisLoggerFactory premisLoggerFactory;

    private AgentPrincipals agent;
    private PID pid;

    public RestoreDeletedJob(PID pid, AgentPrincipals agent,
            RepositoryObjectLoader repositoryObjectLoader, SparqlUpdateService sparqlUpdateService,
            AccessControlService aclService, PremisLoggerFactory premisLoggerFactory) {
        this.pid = pid;
        this.repositoryObjectLoader = repositoryObjectLoader;
        this.sparqlUpdateService = sparqlUpdateService;
        this.premisLoggerFactory = premisLoggerFactory;
        this.aclService = aclService;
        this.agent = agent;
    }

    @Override
    public void run() {
        aclService.assertHasAccess("Insufficient privileges to restore object " + pid.getUUID(),
                pid, agent.getPrincipals(), markForDeletion);

        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

        if (repoObj instanceof AdminUnit) {
            aclService.assertHasAccess("Insufficient privileges to restore admin unit " + pid.getUUID(),
                    pid, agent.getPrincipals(), markForDeletionUnit);
        }

        if (!(repoObj instanceof ContentObject)) {
            throw new InvalidOperationForObjectType("Cannot perform restore on object " + pid.getUUID()
                    + ", objects of type " + repoObj.getClass().getName() + " are not eligible.");
        }

        String updateString = createSparqlDelete(pid.getRepositoryPath(), markedForDeletion, null);

        sparqlUpdateService.executeUpdate(repoObj.getMetadataUri().toString(), updateString);

        premisLoggerFactory.createPremisLogger(repoObj)
                .buildEvent(Premis.Accession)
                .addImplementorAgent(AgentPids.forPerson(agent))
                .addEventDetail("Item restored from deletion")
                .writeAndClose();
    }

}
