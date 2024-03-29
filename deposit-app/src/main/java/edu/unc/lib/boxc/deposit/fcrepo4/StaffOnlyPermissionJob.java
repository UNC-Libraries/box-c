package edu.unc.lib.boxc.deposit.fcrepo4;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.deposit.work.DepositGraphUtils.getChildIterator;

import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;

/**
 * Marks file server ingests "staff only", if the appropriate flag is set on the deposit
 *
 * @author lfarrell
 *
 */
public class StaffOnlyPermissionJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(StaffOnlyPermissionJob.class);

    public StaffOnlyPermissionJob() {
        super();
    }

    public StaffOnlyPermissionJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        log.debug("Setting staff only permissions for deposit {}", getDepositPID());
        Map<String, String> depositStatus = getDepositStatus();
        String staffOnly = depositStatus.get(DepositField.staffOnly.name());

        if (!Boolean.parseBoolean(staffOnly)) {
            return;
        }

        Model model = getWritableModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());
        NodeIterator iterator = getChildIterator(depositBag);
        while (iterator.hasNext()) {
            Resource childResc = (Resource) iterator.next();
            setStaffOnly(childResc);
        }
    }

    private void setStaffOnly(Resource resc) {
        resc.addProperty(CdrAcl.none, PUBLIC_PRINC);
        resc.addProperty(CdrAcl.none, AUTHENTICATED_PRINC);
    }
}
