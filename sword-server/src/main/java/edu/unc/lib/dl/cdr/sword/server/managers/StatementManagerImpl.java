package edu.unc.lib.dl.cdr.sword.server.managers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.OriginalDeposit;
import org.swordapp.server.ResourcePart;
import org.swordapp.server.Statement;
import org.swordapp.server.StatementManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.cdr.sword.server.AtomStatementImpl;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.AccessControlRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class StatementManagerImpl extends AbstractFedoraManager implements StatementManager {

	private static Logger log = Logger.getLogger(StatementManagerImpl.class);

	@Override
	public Statement getStatement(String iri, Map<String, String> accept, AuthCredentials auth, SwordConfiguration config)
			throws SwordServerException, SwordError, SwordAuthException {

		PID targetPID = extractPID(iri, SwordConfigurationImpl.STATE_PATH + "/");

		SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

		// Get the users group
		List<String> groupList = this.getGroups(auth, configImpl);

		if (!accessControlUtils.hasAccess(targetPID, groupList, AccessControlRole.patron.getUri().toString())) {
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
		statement.setOriginalDeposits(getOriginalDeposits(targetPID, configImpl));

		statement.setResources(new ArrayList<ResourcePart>());

		statement.setStates(new HashMap<String, String>());
		statement.addState("Activity", tripleStoreQueryService.fetchState(targetPID));

		return statement;
	}

	private List<OriginalDeposit> getOriginalDeposits(PID pid, SwordConfigurationImpl config) {
		List<OriginalDeposit> results = new ArrayList<OriginalDeposit>();

		List<String> originalDeposits = tripleStoreQueryService.fetchBySubjectAndPredicate(pid,
				ContentModelHelper.Relationship.originalDeposit.toString());

		Date depositedOn = null;
		String depositedBy = null;
		String depositedOnBehalfOf = null;
		String originalDepositURI = null;
		String mimetype = null;
		List<String> packageTypes = null;
		List<String> values = null;
		for (String originalDeposit : originalDeposits) {
			PID depositPID = new PID(originalDeposit);
			Map<String, List<String>> depositTriples = tripleStoreQueryService.fetchAllTriples(depositPID);

			// Get originalDeposit URI
			values = depositTriples.get(ContentModelHelper.FedoraProperty.disseminates.toString());
			if (values != null) {
				for (String dissemination : values) {
					if (dissemination.endsWith("/" + Datastream.DATA_MANIFEST.getName())) {
						originalDepositURI = config.getSwordPath() + SwordConfigurationImpl.EDIT_MEDIA_PATH + "/"
								+ depositPID.getPid() + "/" + Datastream.DATA_MANIFEST.getName();
						mimetype = "text/xml";
						break;
					}
				}
			}

			// Use the objects datafile as its original deposit URI if there was no manifest
			if (originalDepositURI == null) {
				originalDepositURI = config.getSwordPath() + SwordConfigurationImpl.EDIT_MEDIA_PATH + "/" + pid.getPid()
						+ "/" + Datastream.DATA_FILE.getName();
				mimetype = tripleStoreQueryService.lookupSourceMimeType(pid);
			}

			// Get depositedOn value
			values = depositTriples.get(ContentModelHelper.FedoraProperty.createdDate.toString());
			if (values != null && values.size() > 0) {
				String depositedOnString = depositTriples.get(ContentModelHelper.FedoraProperty.createdDate.toString())
						.get(0);
				try {
					depositedOn = DateTimeUtil.parseUTCToDate(depositedOnString);
				} catch (ParseException e) {
					log.error("Could not parse deposited on", e);
				}
			}

			// Get package types
			packageTypes = depositTriples.get(ContentModelHelper.CDRProperty.depositPackageType.toString());
			values = depositTriples.get(ContentModelHelper.CDRProperty.depositPackageSubType.toString());
			if (values != null && values.size() > 0) {
				if (packageTypes == null)
					packageTypes = new ArrayList<String>();
				packageTypes.addAll(values);
			}

			// Get deposited by
			values = depositTriples.get(ContentModelHelper.Relationship.depositedBy.toString());
			if (values != null && values.size() > 0) {
				PID depositedByPID = new PID(values.get(0));
				depositedBy = tripleStoreQueryService.fetchFirstBySubjectAndPredicate(depositedByPID,
						ContentModelHelper.CDRProperty.onyen.toString());
			}

			// Get on behalf of
			values = depositTriples.get(ContentModelHelper.CDRProperty.depositedOnBehalfOf.toString());
			if (values != null && values.size() > 0) {
				depositedOnBehalfOf = values.get(0);
			}

			OriginalDeposit deposit = new OriginalDeposit(originalDepositURI, packageTypes, depositedOn, depositedBy,
					depositedOnBehalfOf);
			deposit.setMediaType(mimetype);
			results.add(deposit);
		}
		return results;
	}
}
