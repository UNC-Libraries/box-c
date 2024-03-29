package edu.unc.lib.boxc.deposit.impl.submit;

import java.util.Map;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.api.submit.DepositHandler;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.PackagingType;
import edu.unc.lib.boxc.persist.api.exceptions.UnsupportedPackagingTypeException;

/**
 * Service which submits deposits to the deposit pipeline based on packaging type
 *
 * @author bbpennel
 *
 */
public class DepositSubmissionService {
    private Map<PackagingType, DepositHandler> packageHandlers;
    private AccessControlService aclService;

    /**
     * Submits deposit to the deposit pipeline based on the packaging type provided
     * in the DepositData object
     *
     * @param destPid PID of the container the deposit will go to
     * @param deposit details of the deposit
     * @return PID of the deposit
     * @throws DepositException
     */
    public PID submitDeposit(PID destPid, DepositData deposit) throws DepositException {
        aclService.assertHasAccess("Insufficient permissions to deposit to " + destPid.getRepositoryPath(),
                destPid, deposit.getDepositingAgent().getPrincipals(), Permission.ingest);

        PackagingType type = deposit.getPackagingType();

        DepositHandler depositHandler = packageHandlers.get(type);
        if (type == null || depositHandler == null) {
            throw new UnsupportedPackagingTypeException("Cannot perform deposit of type " + type);
        }

        return depositHandler.doDeposit(destPid, deposit);
    }

    /**
     * @param packageHandlers the packageHandlers to set
     */
    public void setPackageHandlers(Map<PackagingType, DepositHandler> packageHandlers) {
        this.packageHandlers = packageHandlers;
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }
}
