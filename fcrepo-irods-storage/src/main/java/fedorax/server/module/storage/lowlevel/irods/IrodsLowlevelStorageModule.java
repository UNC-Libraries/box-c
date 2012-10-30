/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * This code was adapted from that originally produced by Bing Zhu of DICE.
 */
package fedorax.server.module.storage.lowlevel.irods;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ConnectionPoolNotFoundException;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.storage.ConnectionPool;
import org.fcrepo.server.storage.ConnectionPoolManager;
import org.fcrepo.server.storage.lowlevel.IListable;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.JargonProperties;
import org.irods.jargon.core.connection.SettableJargonProperties;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * iRODS implementation of the Fedora's LowlevelStorage.
 *
 * To install this module, fedora.fcfg must reference this implementation and the IrodsIFileSystem class.
 *
 * Additional required fedora.fcfg module parameters are: irods_host, irods_port, irods_username, irods_password,
 * irods_homeDirectory, irods_zone, irods_defaultStorageResource.
 *
 */
public class IrodsLowlevelStorageModule extends Module implements ILowlevelStorage, IListable {
	/** Logger for this class. */
	private static final Logger LOG = LoggerFactory.getLogger(IrodsLowlevelStorageModule.class.getName());

	private IRODSFileSystem irodsFileSystem;

	private IrodsFileStore objectStore;

	private IrodsFileStore datastreamStore;

	private ConnectionPool connectionPool;

	IRODSAccount account;

	private int irodsReadBufferSize;

	public static final String REGISTRY_NAME = "registryName";

	public static final String OBJECT_REGISTRY_TABLE = "objectPaths";

	public static final String DATASTREAM_REGISTRY_TABLE = "datastreamPaths";
	
	public static final String STORAGE_LEVEL_HINT = "storage level"; 

	public enum Parameter {
		REGISTRY_NAME("registryName"), OBJECT_REGISTRY_TABLE("objectPaths"), DATASTREAM_REGISTRY_TABLE("datastreamPaths"), OBJECT_STORE_BASE(
				"object_store_base"), DATASTREAM_STORE_BASE("datastream_store_base"), FILESYSTEM("file_system"), PATH_ALGORITHM(
				"path_algorithm"), BACKSLASH_IS_ESCAPE("backslash_is_escape"), CONNECTION_POOL("connectionPool"), PATH_REGISTRY(
				"path_registry"), IRODS_HOST("irods_host"), IRODS_PORT("irods_port"), IRODS_USERNAME("irods_username"), IRODS_PASSWORD(
				"irods_password"), IRODS_HOME_DIRECTORY("irods_homeDirectory"), IRODS_ZONE("irods_zone"), IRODS_DEFAULT_RESOURCE(
				"irods_defaultStorageResource"), IRODS_READ_BUFFER_SIZE("irods_readBufferSize"), STAGING_LOCATIONS("stagingLocations"),
				IRODS_SOCKET_TIMEOUT("irods_socketTimeout");

		private final String name;

		Parameter(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public IrodsLowlevelStorageModule(Map moduleParameters, Server server, String role)
			throws ModuleInitializationException {
		super(moduleParameters, server, role);
		LOG.info("IrodsLowlevelStorageModule()");
	}

	@Override
	public void postInitModule() throws ModuleInitializationException {
		LOG.debug("Setting up IRODS account");
		String irodsHost = getModuleParameter(Parameter.IRODS_HOST, false);
		String irodsPortString = getModuleParameter(Parameter.IRODS_PORT, false);
		int irodsPort = -1;
		try {
			irodsPort = Integer.parseInt(irodsPortString);
			if (irodsPort < 1) {
				throw new ModuleInitializationException("Parameter, " + "\"irods_port\" must be greater than 0", getRole());
			}
		} catch (NumberFormatException e) {
			throw new ModuleInitializationException(e.getMessage(), getRole());
		}
		String irodsUsername = getModuleParameter(Parameter.IRODS_USERNAME, false);
		String irodsPassword = getModuleParameter(Parameter.IRODS_PASSWORD, false);
		String irodsHomeDir = getModuleParameter(Parameter.IRODS_HOME_DIRECTORY, false);
		String irodsZone = getModuleParameter(Parameter.IRODS_ZONE, false);
		String irodsDefaultStorageResource = getModuleParameter(Parameter.IRODS_DEFAULT_RESOURCE, false);
		this.account = new IRODSAccount(irodsHost, irodsPort, irodsUsername, irodsPassword, irodsHomeDir, irodsZone,
				irodsDefaultStorageResource);
		try {
			this.irodsReadBufferSize = Integer.parseInt(getModuleParameter(Parameter.IRODS_READ_BUFFER_SIZE, false));
			if (this.irodsReadBufferSize < 1) {
				throw new ModuleInitializationException("Parameter, \"" + Parameter.IRODS_READ_BUFFER_SIZE
						+ "\" must be greater than 0", getRole());
			}
			String irodsSocketTimeout = getModuleParameter(Parameter.IRODS_SOCKET_TIMEOUT, false);
		} catch (NumberFormatException e) {
			throw new ModuleInitializationException("Cannot parse irods read buffer size "+ e.getMessage(), getRole());
		}
		int irodsSocketTimeout;
		try {
			irodsSocketTimeout = Integer.parseInt(getModuleParameter(Parameter.IRODS_SOCKET_TIMEOUT, false));
			if (irodsSocketTimeout < 0) {
				throw new ModuleInitializationException("Parameter, \"" + Parameter.IRODS_SOCKET_TIMEOUT
						+ "\" cannot be negative", getRole());
			}
		} catch (NumberFormatException e) {
			throw new ModuleInitializationException("Cannot configure irods socket timeout "+ e.getMessage(), getRole());
		}
		LOG.debug("irodsHost=" + irodsHost);
		LOG.debug("irodsPort=" + irodsPort);
		LOG.debug("irodsUsername=" + irodsUsername);
		LOG.debug("irodsPassword=" + irodsPassword);
		LOG.debug("irodsHomeDir=" + irodsHomeDir);
		LOG.debug("irodsZone=" + irodsZone);
		LOG.debug("irodsDefaultStorageResource=" + irodsDefaultStorageResource);

		String objectStoreBase = getModuleParameter(Parameter.OBJECT_STORE_BASE, true);
		String datastreamStoreBase = getModuleParameter(Parameter.DATASTREAM_STORE_BASE, true);

		// parameter required by DBPathRegistry
		String backslashIsEscape = getModuleParameter(Parameter.BACKSLASH_IS_ESCAPE, false).toLowerCase();
		if (backslashIsEscape.equals("true") || backslashIsEscape.equals("false")) {
		} else {
			throw new ModuleInitializationException("backslash_is_escape parameter must be either true or false",
					getRole());
		}

		// get connectionPool from ConnectionPoolManager
		ConnectionPoolManager cpm = (ConnectionPoolManager) getServer().getModule(
				"org.fcrepo.server.storage.ConnectionPoolManager");
		if (cpm == null) {
			throw new ModuleInitializationException("ConnectionPoolManager module was required, but apparently has "
					+ "not been loaded.", getRole());
		}
		try {
			this.connectionPool = cpm.getPool();
		} catch (ConnectionPoolNotFoundException e1) {
			throw new ModuleInitializationException("Could not find requested " + "connectionPool.", getRole());
		}

		try {
			irodsFileSystem = IRODSFileSystem.instance();
			JargonProperties origProps = irodsFileSystem.getIrodsSession().getJargonProperties();
			SettableJargonProperties overrideJargonProperties = new SettableJargonProperties(origProps);
			overrideJargonProperties.setIrodsSocketTimeout(irodsSocketTimeout); // was 300
			overrideJargonProperties.setIrodsParallelSocketTimeout(irodsSocketTimeout); // was 300
			irodsFileSystem.getIrodsSession().setJargonProperties(overrideJargonProperties);
		} catch (JargonException e) {
			throw new ModuleInitializationException("Could not create IRODSFileSystem: " + e.getLocalizedMessage(),
					getRole());
		}

		// build up base config map for DBPathRegistry and PathAlgorithm
		Map configuration = new HashMap();
		configuration.put("connectionPool", this.connectionPool);
		configuration.put("backslashIsEscape", backslashIsEscape);

		Map<String, Object> objConfig = new HashMap<String, Object>();
		objConfig.putAll(configuration);
		objConfig.put(REGISTRY_NAME, OBJECT_REGISTRY_TABLE);
		objConfig.put("storeBase", objectStoreBase);
		objConfig.put("storeBases", new String[] { objectStoreBase });
		objectStore = makeStore(objConfig);

		Map<String, Object> dsConfig = new HashMap<String, Object>();
		dsConfig.putAll(configuration);
		dsConfig.put(REGISTRY_NAME, DATASTREAM_REGISTRY_TABLE);
		dsConfig.put("storeBase", datastreamStoreBase);
		dsConfig.put("storeBases", new String[] { datastreamStoreBase });
		datastreamStore = makeStore(dsConfig);
	}

	private IrodsFileStore makeStore(Map configuration) throws ModuleInitializationException {
		IrodsFileStore result = null;
		try {
			IrodsIFileSystem filesystem = new IrodsIFileSystem(irodsReadBufferSize, irodsFileSystem, account);
			IrodsDBPathRegistry pathRegistry = new IrodsDBPathRegistry(irodsFileSystem, account, configuration);
			TimestampPathAlgorithm pathAlgorithm = new TimestampPathAlgorithm(configuration);
			result = new IrodsFileStore(pathAlgorithm, pathRegistry, filesystem);
		} catch (LowlevelStorageException e) {
			throw new ModuleInitializationException(e.getMessage(), getRole());
		}
		return result;
	}

	protected String getModuleParameter(Parameter parameter, boolean parameterAsAbsolutePath)
			throws ModuleInitializationException {
		String parameterValue = getParameter(parameter.toString(), parameterAsAbsolutePath);

		if (parameterValue == null) {
			throw new ModuleInitializationException(parameter + " parameter must be specified", getRole());
		}
		return parameterValue;
	}

	public void addObject(String pid, InputStream content, Map<String, String> hints) throws LowlevelStorageException {
		objectStore.add(pid, content, hints);
	}

	public void replaceObject(String pid, InputStream content, Map<String, String> hints) throws LowlevelStorageException {
		objectStore.replace(pid, content, hints);
	}

	public InputStream retrieveObject(String pid) throws LowlevelStorageException {
		return objectStore.retrieve(pid);
	}

	public void removeObject(String pid) throws LowlevelStorageException {
		objectStore.remove(pid);
	}

	public void rebuildObject() throws LowlevelStorageException {
		objectStore.rebuild();
	}

	public void auditObject() throws LowlevelStorageException {
		objectStore.audit();
	}

	public long addDatastream(String pid, InputStream content, Map<String, String> hints) throws LowlevelStorageException {
		return datastreamStore.add(pid, content, hints);
	}

	public long replaceDatastream(String pid, InputStream content, Map<String, String> hints) throws LowlevelStorageException {
		return datastreamStore.replace(pid, content, hints);
	}

	public InputStream retrieveDatastream(String pid) throws LowlevelStorageException {
		return datastreamStore.retrieve(pid);
	}

	public void removeDatastream(String pid) throws LowlevelStorageException {
		datastreamStore.remove(pid);
	}

	public void rebuildDatastream() throws LowlevelStorageException {
		datastreamStore.rebuild();
	}

	public void auditDatastream() throws LowlevelStorageException {
		datastreamStore.audit();
	}

	public Iterator<String> listObjects() {
		return objectStore.list();
	}

	public Iterator<String> listDatastreams() {
		return datastreamStore.list();
	}

	public IRODSQueryResultSet getDatastreamStorageMetadata(String dsID) throws LowlevelStorageException {
		return datastreamStore.getMetadata(dsID);
	}

	public IRODSQueryResultSet getFOXMLStorageMetadata(String pid) throws LowlevelStorageException {
		return objectStore.getMetadata(pid);
	}

	public String getDatastreamIrodsPath(String dsID) throws LowlevelStorageException {
		return datastreamStore.getIrodsPath(dsID);
	}

	public String getFOXMLIrodsPath(String pid) throws LowlevelStorageException {
		return objectStore.getIrodsPath(pid);
	}

}
