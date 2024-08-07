package edu.unc.lib.boxc.operations.impl.delete;

import static edu.unc.lib.boxc.auth.api.Permission.markForDeletion;
import static edu.unc.lib.boxc.auth.api.Permission.markForDeletionUnit;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.markedForDeletion;
import static edu.unc.lib.boxc.model.api.sparql.SparqlUpdateHelper.createSparqlReplace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
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
import io.dropwizard.metrics5.Timer;

/**
 * Operation to mark a repository content object as deleted
 *
 * @author bbpennel
 *
 */
public class MarkForDeletionJob implements Runnable {

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private SparqlUpdateService sparqlUpdateService;
    private PremisLoggerFactory premisLoggerFactory;

    private AgentPrincipals agent;
    private PID pid;
    private String message;

    private static final Timer timer = TimerFactory.createTimerForClass(MarkForDeletionJob.class);


    public MarkForDeletionJob(PID pid, String message, AgentPrincipals agent,
            RepositoryObjectLoader repositoryObjectLoader, SparqlUpdateService sparqlUpdateService,
            AccessControlService aclService, PremisLoggerFactory premisLoggerFactory) {
        this.pid = pid;
        this.repositoryObjectLoader = repositoryObjectLoader;
        this.sparqlUpdateService = sparqlUpdateService;
        this.premisLoggerFactory = premisLoggerFactory;
        this.aclService = aclService;
        this.agent = agent;
        this.message = message;
    }

    @Override
    public void run() {
        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess("Insufficient privileges to delete object " + pid.getUUID(),
                    pid, agent.getPrincipals(), markForDeletion);

            RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

            if (repoObj instanceof AdminUnit) {
                aclService.assertHasAccess("Insufficient privileges to delete admin unit " + pid.getUUID(),
                        pid, agent.getPrincipals(), markForDeletionUnit);
            }

            if (!(repoObj instanceof ContentObject)) {
                throw new InvalidOperationForObjectType("Cannot mark object " + pid.getUUID()
                        + " for deletion, objects of type " + repoObj.getClass().getName() + " are not eligible.");
            }

            String updateString = createSparqlReplace(pid.getRepositoryPath(), markedForDeletion, true);

            sparqlUpdateService.executeUpdate(repoObj.getMetadataUri().toString(), updateString);

            premisLoggerFactory.createPremisLogger(repoObj)
                    .buildEvent(Premis.Deaccession)
                    .addImplementorAgent(AgentPids.forPerson(agent))
                    .addEventDetail("Item marked for deletion and not available without permissions")
                    .addEventDetail(message)
                    .writeAndClose();
        }
    }
}
