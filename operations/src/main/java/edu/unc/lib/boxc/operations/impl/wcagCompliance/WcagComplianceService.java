package edu.unc.lib.boxc.operations.impl.wcagCompliance;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.utils.FedoraPropertiesUtil;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.wcagCompliance.WcagComplianceRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Service which assigns a WCAG Compliance level to a FileObject
 */
public class WcagComplianceService {
    private static final Logger log = LoggerFactory.getLogger(WcagComplianceService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private IndexingMessageSender indexingMessageSender;
    public static final String LEVEL_A_10 = "WCAG 1.0 Level A";
    public static final String LEVEL_AA_10 = "WCAG 1.0 Level AA";
    public static final String LEVEL_AAA_10 = "WCAG 1.0 Level AAA";
    public static final String LEVEL_A_20 = "WCAG 2.0 Level A";
    public static final String LEVEL_AA_20 = "WCAG 2.0 Level AA";
    public static final String LEVEL_AAA_20 = "WCAG 2.0 Level AAA";
    public static final String LEVEL_A_21 = "WCAG 2.1 Level A";
    public static final String LEVEL_AA_21 = "WCAG 2.1 Level AA";
    public static final String LEVEL_AAA_21 = "WCAG 2.1 Level AAA";
    public static final String LEVEL_A_22 = "WCAG 2.2 Level A";
    public static final String LEVEL_AA_22 = "WCAG 2.2 Level AA";
    public static final String LEVEL_AAA_22 = "WCAG 2.2 Level AAA";
    public static final List<String> ACCEPTED_LEVELS = Arrays.asList(LEVEL_A_10, LEVEL_AA_10,
            LEVEL_AAA_10, LEVEL_A_20, LEVEL_AA_20, LEVEL_AAA_20, LEVEL_A_21, LEVEL_AA_21, LEVEL_AAA_21,
            LEVEL_A_22, LEVEL_AA_22, LEVEL_AAA_22);

    public void updateWcagCompliance(WcagComplianceRequest request) {
        var pidString = request.getPidString();
        var filePid = PIDs.get(pidString);
        var agent = request.getAgent();
        var agentPrincipals = agent.getPrincipals();
        var requestLevel = request.getLevel();

        if (!StringUtils.isBlank(requestLevel) && !ACCEPTED_LEVELS.contains(requestLevel)) {
            throw new IllegalArgumentException("WCAG Compliance level update for " + pidString + " cannot be completed." +
                    " Level: " + requestLevel + " is not valid");
        }

        log.debug("Updating WCAG compliance level for {}", pidString);
        aclService.assertHasAccess(
                "User does not have permission to edit WCAG compliance level",
                filePid, agentPrincipals, Permission.editResourceType);

        var repoObj = repoObjLoader.getRepositoryObject(filePid);

        if (!(repoObj instanceof FileObject)) {
            throw new InvalidOperationForObjectType("Object " + pidString + " of type " + repoObj.getClass().getName()
                    + " cannot be assigned a WCAG compliance level.");
        }

        var currentLevel = FedoraPropertiesUtil.getValue(repoObj, Cdr.wcagCompliance);
        // if current level is null (does not exist) and the requested level is blank, do nothing
        if (currentLevel == null && StringUtils.isBlank(requestLevel)) {
            log.debug("The current WCAG compliance level is null and the requested level is blank");
            return;
        }
        // if we're just updating to the same existing compliance level, do nothing
        if (!StringUtils.isBlank(currentLevel) && Objects.equals(currentLevel, requestLevel)) {
            log.debug("The current WCAG compliance level and the requested level are the same value");
            return;
        }

        // now, if the request level is blank delete the property
        if (StringUtils.isBlank(requestLevel)) {
            log.debug("The requested WCAG compliance level is blank, deleting the property");
            repositoryObjectFactory.deleteProperty(repoObj, Cdr.wcagCompliance);
        } else {
            repositoryObjectFactory.createExclusiveRelationship(repoObj, Cdr.wcagCompliance, request.getLevel());
        }
        indexingMessageSender.sendIndexingOperation(agent.getUsername(), filePid, IndexingActionType.ADD);
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
