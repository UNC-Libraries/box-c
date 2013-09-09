package edu.unc.lib.dl.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSServerProperties;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.FileIntegrityException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.protovalues.ErrorEnum;
import org.irods.jargon.core.pub.EnvironmentalInfoAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.IRODSGenQueryExecutor;
import org.irods.jargon.core.pub.RemoteExecutionOfCommandsAO;
import org.irods.jargon.core.pub.RuleProcessingAO;
import org.irods.jargon.core.query.GenQueryBuilderException;
import org.irods.jargon.core.query.GenQueryOrderByField;
import org.irods.jargon.core.query.IRODSGenQueryBuilder;
import org.irods.jargon.core.query.IRODSGenQueryFromBuilder;
import org.irods.jargon.core.query.IRODSQueryResultRow;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.QueryConditionOperators;
import org.irods.jargon.core.query.RodsGenQueryEnum;
import org.irods.jargon.core.rule.IRODSRuleExecResult;
import org.irods.jargon.core.rule.IRODSRuleExecResultOutputParameter;
import org.irods.jargon.core.rule.IRODSRuleParameter;
import org.jdom.Element;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;

public class FixityLogTask implements Runnable {
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");

	private static final Log LOG = LogFactory.getLog(FixityLogTask.class);

	private IRODSAccount irodsAccount = null;
	private IRODSFileSystem irodsFileSystem = null;
	private ManagementClient managementClient = null;

	private List<String> resourceNames = null;
	private int staleInterval = 0;
	private int objectLimit = 0;
	private String fixityLogPath = null;

	public IRODSAccount getIrodsAccount() {
		return irodsAccount;
	}

	public void setIrodsAccount(IRODSAccount irodsAccount) {
		this.irodsAccount = irodsAccount;
	}

	public IRODSFileSystem getIrodsFileSystem() {
		return irodsFileSystem;
	}

	public void setIrodsFileSystem(IRODSFileSystem irodsFileSystem) {
		this.irodsFileSystem = irodsFileSystem;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public List<String> getResourceNames() {
		return resourceNames;
	}

	public void setResourceNames(List<String> resourceNames) {
		this.resourceNames = resourceNames;
	}

	public int getStaleInterval() {
		return staleInterval;
	}

	public void setStaleInterval(int staleInterval) {
		this.staleInterval = staleInterval;
	}

	public int getObjectLimit() {
		return objectLimit;
	}

	public void setObjectLimit(int objectLimit) {
		this.objectLimit = objectLimit;
	}
	
	public String getFixityLogPath() {
		return fixityLogPath;
	}
	
	public void setFixityLogPath(String fixityLogPath) {
		this.fixityLogPath = fixityLogPath;
	}

	public void run() {
		
		FileOutputStream fixityLogOutput = null;
		
		try {
			
			fixityLogOutput = new FileOutputStream(fixityLogPath, true);

			// Check that all resources are available
	
			List<String> unavailableResourceNames = new ArrayList<String>();
	
			for (String name : resourceNames) {
				if (!isResourceAvailable(name)) {
					unavailableResourceNames.add(name);
				}
			}
	
			if (unavailableResourceNames.size() > 0) {
				LOG.error("One or more resources are unavailable: " + unavailableResourceNames);
				return;
			}
	
			// Verify stale objects, write output, and touch timestamp
	
			List<String> objectPaths = getStaleObjectPaths(getCurrentIcatTime() - staleInterval, objectLimit);
			
			for (String path : objectPaths) {
	
				if (!shouldVerifyPath(path))
					continue;
	
				for (String name : resourceNames) {
					FixityVerificationResult fixityVerificationResult = verifyFixityForObjectOnResource(path, name);
	
					appendFixityLogEntry(fixityLogOutput, fixityVerificationResult);
					appendPremisEvent(fixityVerificationResult);
				}
	
				touchObjectFixityTimestamp(path);
	
			}
			
		} catch (IOException e) {
			
			LOG.error(e);
			throw new Error(e);
			
		} finally {
			
			if (fixityLogOutput != null) {
				try {
					fixityLogOutput.close();
				} catch (IOException e) {
					throw new Error(e);
				}
			}
			
			irodsFileSystem.closeAndEatExceptions();
			
		}

	}

	private void appendFixityLogEntry(OutputStream output, FixityVerificationResult fixityVerificationResult) throws IOException {
		
		Map<String, Object> entry = new LinkedHashMap<String, Object>();

		// The members "time", "result", "objectPath", "resourceName", "irodsReleaseVersion", and "jargonVersion" are always present.

		entry.put("time", dateFormat.format(fixityVerificationResult.getTime()));
		entry.put("result", fixityVerificationResult.getResult());
		entry.put("objectPath", fixityVerificationResult.getObjectPath());
		entry.put("resourceName", fixityVerificationResult.getResourceName());
		entry.put("irodsReleaseVersion", fixityVerificationResult.getIrodsReleaseVersion());
		entry.put("jargonVersion", fixityVerificationResult.getJargonVersion());
		
		// The members "expectedChecksum", "elapsed", "filePath", "irodsErrorCode", and "jargonException" are present only if non-null.

		if (fixityVerificationResult.getExpectedChecksum() != null)
			entry.put("expectedChecksum", fixityVerificationResult.getExpectedChecksum());
		
		if (fixityVerificationResult.getElapsed() != null)
			entry.put("elapsed", fixityVerificationResult.getElapsed());
		
		if (fixityVerificationResult.getFilePath() != null)
			entry.put("filePath", fixityVerificationResult.getFilePath());
		
		if (fixityVerificationResult.getIrodsErrorCode() != null)
			entry.put("irodsErrorCode", fixityVerificationResult.getIrodsErrorCode());
		
		// The "jargonException" member is serialized as an object with "className" and "message" members.
		
		if (fixityVerificationResult.getJargonException() != null) {
			JargonException exception = fixityVerificationResult.getJargonException();
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("className", exception.getClass().getName());
			map.put("message", exception.getMessage());
		    entry.put("jargonException", map);   
		}
		
		// Write the log entry to the output, followed by "\r\n".

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		
		mapper.writeValue(output, entry);
		output.write(new byte[] { 0x0D, 0x0A }); // \r\n
		output.flush();
		
	}

	private void appendPremisEvent(FixityVerificationResult fixityVerificationResult) {

		if (fixityVerificationResult.getResult() == FixityVerificationResult.Result.OK) {
			return;
		}

		PID pid = fixityVerificationResult.getPID();

		if (pid != null) {

			PremisEventLogger premisEventLogger = new PremisEventLogger(null);

			Element event = premisEventLogger.logEvent(pid);

			// The eventType is a fixity check

			PremisEventLogger.addType(event, PremisEventLogger.Type.FIXITY_CHECK);

			// The eventDateTime is the value from the log, not the current time

			PremisEventLogger.addDateTime(event, fixityVerificationResult.getTime());

			// Add eventDetailedOutcome, possibly with an eventDetailNote

			String detailNote = null;

			if (fixityVerificationResult.getResult() == FixityVerificationResult.Result.FAILED)
				detailNote = "The checksum of the replica on " + fixityVerificationResult.getResourceName() + " didn't match the value recorded in the iCAT.";
			else if (fixityVerificationResult.getResult() == FixityVerificationResult.Result.ERROR)
				detailNote = "An error occurred while attempting to calculate the checksum for the object on " + fixityVerificationResult.getResourceName() + ".";
			else if (fixityVerificationResult.getResult() == FixityVerificationResult.Result.MISSING)
				detailNote = "The replica on " + fixityVerificationResult.getResourceName() + " was not found in the iCAT.";

			PremisEventLogger.addDetailedOutcome(event, fixityVerificationResult.getResult().toString(), detailNote, null);

			// Add a linkingAgentIdentifier element for the software involved (this class, iRODS, and Jargon)

			PremisEventLogger.addLinkingAgentIdentifier(event, "Class", this.getClass().getName(), "Software");
			PremisEventLogger.addLinkingAgentIdentifier(event, "iRODS release version", fixityVerificationResult.getIrodsReleaseVersion(), "Software");
			PremisEventLogger.addLinkingAgentIdentifier(event, "Jargon version", fixityVerificationResult.getJargonVersion(), "Software");

			// Add a linkingObjectIdentifier for the iRODS object path

			PremisEventLogger.addLinkingObjectIdentifier(event, "iRODS object path", fixityVerificationResult.getObjectPath(), null);

			// Add a linkingObjectIdentifier for the datastream (if there was one)

			String datastream = fixityVerificationResult.getDatastream();

			if (datastream != null)
				PremisEventLogger.addLinkingObjectIdentifier(event, "PID", pid + "/" + datastream, null);

			// Write the event

			try {
				this.managementClient.writePremisEventsToFedoraObject(premisEventLogger, pid);
			} catch (FedoraException e) {
				LOG.error("Error writing PREMIS event for pid: " + pid, e);
			}

		} else {

			LOG.info("No PREMIS event added for fixity verification result without pid: " + fixityVerificationResult);

		}

	}

	private FixityVerificationResult verifyFixityForObjectOnResource(String objectPath, String resourceName) {

		// Assemble the basics of the verification result.

		FixityVerificationResult verificationResult = new FixityVerificationResult();

		verificationResult.setTime(new Date());
		verificationResult.setObjectPath(objectPath);
		verificationResult.setResourceName(resourceName);

		// Get server properties and copy to the verification result.

		IRODSServerProperties serverProperties = this.getServerProperties();

		verificationResult.setJargonVersion(IRODSServerProperties.getJargonVersion());
		verificationResult.setIrodsReleaseVersion(serverProperties.getRelVersion());

		// Query for info about this object.

		Map<String, String> info = getInfoForObjectOnResource(objectPath, resourceName);

		// If info is null, the iCAT contains no record of this object on this resource, so the result is MISSING.
		// Otherwise, we will try to verify the checksum.

		if (info == null) {

			verificationResult.setResult(FixityVerificationResult.Result.MISSING);

		} else {

			// Assign the checksum and filesystem path.

			verificationResult.setExpectedChecksum(info.get("DATA_CHECKSUM"));
			verificationResult.setFilePath(info.get("DATA_PATH"));

			// Verify the replica's checksum, recording elapsed time.

			double start, elapsed;
			Object result = null;
			Integer code = null;

			start = (double) System.currentTimeMillis();
			result = verifyChecksumForObjectAndReplica(objectPath, info.get("DATA_REPL_NUM"));
			elapsed = ((double) System.currentTimeMillis() - start) / 1000.0;
			
			// If the result is an integer, record it if it is non-zero (an error code).
			// If the result is an exception, record it.

			if (result instanceof Integer) {
				code = (Integer) result;
				if (code.intValue() != 0)
					verificationResult.setIrodsErrorCode(code);
			} else if (result instanceof JargonException) {
				verificationResult.setJargonException((JargonException) result);
			} else {
				throw new Error("Unexpected result from verifyChecksumForObjectAndReplica: " + result);
			}
			
			// Record elapsed time for checksum verification

			verificationResult.setElapsed(new Double(elapsed));
			
			// Interpret code as a result value.

			if (code != null) {
				if (code.intValue() == 0) {
					verificationResult.setResult(FixityVerificationResult.Result.OK);
				} else if (code.intValue() == ErrorEnum.USER_CHKSUM_MISMATCH.getInt()) {
					verificationResult.setResult(FixityVerificationResult.Result.FAILED);
				} else {
					verificationResult.setResult(FixityVerificationResult.Result.ERROR);
				}
			} else {
				verificationResult.setResult(FixityVerificationResult.Result.ERROR);
			}

		}

		return verificationResult;

	}


	// The following methods deal with Jargon directly.

	private IRODSServerProperties getServerProperties() {

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			EnvironmentalInfoAO environmentalInfo = accessObjectFactory.getEnvironmentalInfoAO(irodsAccount);

			return environmentalInfo.getIRODSServerPropertiesFromIRODSServer();

		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

	private Map<String, String> getInfoForObjectOnResource(String objectPath, String resourceName) {

		int lastPathSeparatorIndex = objectPath.lastIndexOf("/");
		String collName = objectPath.substring(0, lastPathSeparatorIndex);
		String dataName = objectPath.substring(lastPathSeparatorIndex + 1);

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			IRODSGenQueryExecutor genQueryExecutor = accessObjectFactory.getIRODSGenQueryExecutor(irodsAccount);

			IRODSGenQueryBuilder builder = new IRODSGenQueryBuilder(true, null);
			builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_DATA_REPL_NUM);
			builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_D_DATA_CHECKSUM);
			builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_D_DATA_PATH);
			builder.addConditionAsGenQueryField(RodsGenQueryEnum.COL_DATA_NAME, QueryConditionOperators.EQUAL, dataName);
			builder.addConditionAsGenQueryField(RodsGenQueryEnum.COL_COLL_NAME, QueryConditionOperators.EQUAL, collName);
			builder.addConditionAsGenQueryField(RodsGenQueryEnum.COL_R_RESC_NAME, QueryConditionOperators.EQUAL, resourceName);

			IRODSGenQueryFromBuilder query = builder.exportIRODSQueryFromBuilder(1);
			IRODSQueryResultSet queryResultSet = genQueryExecutor.executeIRODSQueryAndCloseResult(query, 0);
			IRODSQueryResultRow row = queryResultSet.getFirstResult();
			
			genQueryExecutor.closeResults(queryResultSet);

			Map<String, String> info = new HashMap<String, String>();
			info.put("DATA_REPL_NUM", row.getColumn("DATA_REPL_NUM"));
			info.put("DATA_CHECKSUM", row.getColumn("DATA_CHECKSUM"));
			info.put("DATA_PATH", row.getColumn("DATA_PATH"));

			return info;

		} catch (DataNotFoundException e) {
			return null;
		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		} catch (GenQueryBuilderException e) {
			LOG.error(e);
			throw new Error(e);
		} catch (JargonQueryException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

	private Object verifyChecksumForObjectAndReplica(String objectPath, String replicaNumber) {

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			RuleProcessingAO ruleProcessingAO = accessObjectFactory.getRuleProcessingAO(irodsAccount);

			StringBuilder sb = new StringBuilder("main { msiDataObjChksum(*path, \"verifyChksum=++++replNum=*replNum\", \"null\") }\n");
			sb.append("INPUT *path='-',*replNum='-'\n");
			sb.append("OUTPUT ruleExecOut");

			List<IRODSRuleParameter> inputOverrides = new ArrayList<IRODSRuleParameter>();
			inputOverrides.add(new IRODSRuleParameter("*path", "\"" + objectPath.replaceAll("\"", "\\\\\"") + "\""));
			inputOverrides.add(new IRODSRuleParameter("*replNum", "\"" + replicaNumber.replaceAll("\"", "\\\\\"") + "\""));

			try {

				ruleProcessingAO.executeRule(sb.toString(), inputOverrides, RuleProcessingAO.RuleProcessingType.EXTERNAL);
				return new Integer(0);

			} catch (JargonException e) {

				int code = e.getUnderlyingIRODSExceptionCode();

				// Since Jargon will sometimes translate error codes into exceptions without retaining the iRODS error code, most notably for USER_CHKSUM_MISMATCH, we will add a special case to restore the iRODS error code. See IRODSErrorScanner.

				if (code == 0 && e instanceof FileIntegrityException) {
					code = ErrorEnum.USER_CHKSUM_MISMATCH.getInt();
				}

				// If we have a reliable error code, return that. Otherwise, return the exception.

				if (code == 0) {
					return e;
				} else {
					return new Integer(code);
				}

			}

		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

	private boolean isResourceAvailable(String resourceName) {

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			IRODSGenQueryExecutor genQueryExecutor = accessObjectFactory.getIRODSGenQueryExecutor(irodsAccount);
			RemoteExecutionOfCommandsAO remoteExecutionOfCommands = accessObjectFactory.getRemoteExecutionOfCommandsAO(irodsAccount);

			IRODSGenQueryBuilder builder = new IRODSGenQueryBuilder(true, null);
			builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_R_LOC);
			builder.addConditionAsGenQueryField(RodsGenQueryEnum.COL_R_RESC_NAME, QueryConditionOperators.EQUAL, resourceName);

			IRODSGenQueryFromBuilder query = builder.exportIRODSQueryFromBuilder(1);
			IRODSQueryResultSet queryResultSet = genQueryExecutor.executeIRODSQueryAndCloseResult(query, 0);
			IRODSQueryResultRow row = queryResultSet.getFirstResult();
			
			genQueryExecutor.closeResults(queryResultSet);

			String host = row.getColumn(RodsGenQueryEnum.COL_R_LOC.getName());
			boolean available = false;

			try {
				remoteExecutionOfCommands.executeARemoteCommandAndGetStreamGivingCommandNameAndArgsAndHost("hello", "", host);
				available = true;
			} catch (JargonException e) {
			}

			return available;

		} catch (GenQueryBuilderException e) {
			LOG.error(e);
			throw new Error(e);
		} catch (JargonQueryException e) {
			LOG.error(e);
			throw new Error(e);
		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

	private int getCurrentIcatTime() {

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			RuleProcessingAO ruleProcessingAO = accessObjectFactory.getRuleProcessingAO(irodsAccount);

			StringBuilder sb = new StringBuilder("main { msiGetIcatTime(*result, \"unix\") }\n");
			sb.append("INPUT null\n");
			sb.append("OUTPUT *result");

			IRODSRuleExecResult result = ruleProcessingAO.executeRule(sb.toString());
			IRODSRuleExecResultOutputParameter resultOutputParam = result.getOutputParameterResults().get("*result");

			if (resultOutputParam == null) {
				throw new Error("Output parameter result was null");
			}

			return Integer.parseInt((String) resultOutputParam.getResultObject());

		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

	private List<String> getStaleObjectPaths(int staleTime, int objectLimit) {

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			IRODSGenQueryExecutor genQueryExecutor = accessObjectFactory.getIRODSGenQueryExecutor(irodsAccount);

			IRODSGenQueryBuilder builder = new IRODSGenQueryBuilder(true, null);
			builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_COLL_NAME);
			builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_DATA_NAME);
			builder.addSelectAsGenQueryValue(RodsGenQueryEnum.COL_META_DATA_ATTR_VALUE);
			builder.addOrderByGenQueryField(RodsGenQueryEnum.COL_META_DATA_ATTR_VALUE, GenQueryOrderByField.OrderByType.ASC);
			builder.addConditionAsGenQueryField(RodsGenQueryEnum.COL_META_DATA_ATTR_NAME, QueryConditionOperators.EQUAL, "cdrFixityTimestamp");
			builder.addConditionAsGenQueryField(RodsGenQueryEnum.COL_COLL_NAME, QueryConditionOperators.NOT_LIKE, "/" + irodsAccount.getZone() + "/trash/%");
			builder.addConditionAsGenQueryField(RodsGenQueryEnum.COL_META_DATA_ATTR_VALUE, QueryConditionOperators.NUMERIC_LESS_THAN_OR_EQUAL_TO, staleTime);

			IRODSGenQueryFromBuilder query = builder.exportIRODSQueryFromBuilder(objectLimit);
			IRODSQueryResultSet queryResultSet = genQueryExecutor.executeIRODSQueryAndCloseResult(query, 0);
			List<IRODSQueryResultRow> results = queryResultSet.getResults();
			
			genQueryExecutor.closeResults(queryResultSet);

			List<String> paths = new ArrayList<String>();

			for (IRODSQueryResultRow row : results) {
				String collName = row.getColumn(RodsGenQueryEnum.COL_COLL_NAME.getName());
				String dataName = row.getColumn(RodsGenQueryEnum.COL_DATA_NAME.getName());

				paths.add(collName + "/" + dataName);
			}

			return paths;

		} catch (GenQueryBuilderException e) {
			LOG.error(e);
			throw new Error(e);
		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		} catch (JargonQueryException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

	private void touchObjectFixityTimestamp(String objectPath) {

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			RuleProcessingAO ruleProcessingAO = accessObjectFactory.getRuleProcessingAO(irodsAccount);

			StringBuilder sb = new StringBuilder("main { msiGetIcatTime(*time, \"unix\"); msiAddKeyVal(*keyval, \"cdrFixityTimestamp\", str(int(*time))); msiSetKeyValuePairsToObj(*keyval, *path, \"-d\") }\n");
			sb.append("INPUT *path='-'\n");
			sb.append("OUTPUT ruleExecOut");

			List<IRODSRuleParameter> inputOverrides = new ArrayList<IRODSRuleParameter>();
			inputOverrides.add(new IRODSRuleParameter("*path", "\"" + objectPath.replaceAll("\"", "\\\\\"") + "\""));

			ruleProcessingAO.executeRule(sb.toString(), inputOverrides, RuleProcessingAO.RuleProcessingType.EXTERNAL);

		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

	private boolean shouldVerifyPath(String objectPath) {

		try {

			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			RuleProcessingAO ruleProcessingAO = accessObjectFactory.getRuleProcessingAO(irodsAccount);

			StringBuilder sb = new StringBuilder("main { *result = str(cdrShouldVerifyOrReplicate(*path)) }\n");
			sb.append("INPUT *path='-'\n");
			sb.append("OUTPUT *result");

			List<IRODSRuleParameter> inputOverrides = new ArrayList<IRODSRuleParameter>();
			inputOverrides.add(new IRODSRuleParameter("*path", "\"" + objectPath.replaceAll("\"", "\\\\\"") + "\""));

			IRODSRuleExecResult result = ruleProcessingAO.executeRule(sb.toString(), inputOverrides, RuleProcessingAO.RuleProcessingType.EXTERNAL);
			IRODSRuleExecResultOutputParameter resultOutputParam = result.getOutputParameterResults().get("*result");

			if (resultOutputParam == null) {
				throw new Error("Output parameter result was null");
			}

			return resultOutputParam.getResultObject().equals("true");

		} catch (JargonException e) {
			LOG.error(e);
			throw new Error(e);
		}

	}

}