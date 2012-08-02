package edu.unc.lib.dl.cdr.sword.server.managers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ResourcePart;
import org.swordapp.server.Statement;
import org.swordapp.server.StatementManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.cdr.sword.server.AtomStatementImpl;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.AccessControlRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DateTimeUtil;

public class StatementManagerImpl extends AbstractFedoraManager implements StatementManager {

	private static Logger log = Logger.getLogger(StatementManagerImpl.class);
	
	private DepositReportingUtil depositReportingUtil;

	@Override
	public Statement getStatement(String iri, Map<String, String> accept, AuthCredentials auth, SwordConfiguration config)
			throws SwordServerException, SwordError, SwordAuthException {

		PID targetPID = extractPID(iri, SwordConfigurationImpl.STATE_PATH + "/");

		SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

		if (!hasAccess(auth, targetPID, AccessControlRole.patron, configImpl)) {
			throw new SwordAuthException("Insufficient privileges to retrieve statement for " + targetPID.getPid());
		}

		String label = tripleStoreQueryService.lookupLabel(targetPID);
		String lastModifiedString = tripleStoreQueryService.fetchFirstBySubjectAndPredicate(targetPID,
				ContentModelHelper.FedoraProperty.lastModifiedDate.toString());

		Statement statement = new AtomStatementImpl(iri, "CDR", label, lastModifiedString);

		if (lastModifiedString != null) {
			try {
				statement.setLastModified(DateTimeUtil.parseUTCToDate(lastModifiedString));
			} catch (ParseException e) {
				log.error("Could not parse last modified", e);
			}
		}
		statement.setOriginalDeposits(depositReportingUtil.getOriginalDeposits(targetPID, configImpl));

		statement.setResources(new ArrayList<ResourcePart>());

		statement.setStates(new HashMap<String, String>());
		statement.addState("Activity", tripleStoreQueryService.fetchState(targetPID));

		return statement;
	}
	
	public void setDepositReportingUtil(DepositReportingUtil depositReportingUtil) {
		this.depositReportingUtil = depositReportingUtil;
	}
}
