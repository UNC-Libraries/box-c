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
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
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
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.staging.StagesURLStreamHandlerFactory;

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
	
	static {
		// Register IRODS URL Protocol Handler (see metadata project)
		// https://issues.apache.org/bugzilla/show_bug.cgi?id=26701
		URLStreamHandlerFactory urlHandlerFactory = new StagesURLStreamHandlerFactory();
		try {
			URL.setURLStreamHandlerFactory(urlHandlerFactory);
		} catch(Error e) {}
		DirContextURLStreamHandlerFactory.addUserFactory(new StagesURLStreamHandlerFactory());
	}

	// constants
	public static final String REGISTRY_NAME = "registryName";
	public static final String OBJECT_REGISTRY_TABLE = "objectPaths";
	public static final String DATASTREAM_REGISTRY_TABLE = "datastreamPaths";	
	public static final String STORAGE_LEVEL_HINT = "storage level";
	
	// injected properties
	private IRODSAccount account;
	private String objectStoreBase;
	private String datastreamStoreBase;
	private int irodsReadBufferSize;
	private int irodsSocketTimeout;
	
	public IRODSAccount getAccount() {
		return account;
	}

	public void setAccount(IRODSAccount account) {
		this.account = account;
	}

	public String getObjectStoreBase() {
		return objectStoreBase;
	}

	public void setObjectStoreBase(String objectStoreBase) {
		this.objectStoreBase = objectStoreBase;
	}

	public String getDatastreamStoreBase() {
		return datastreamStoreBase;
	}

	public void setDatastreamStoreBase(String datastreamStoreBase) {
		this.datastreamStoreBase = datastreamStoreBase;
	}

	public int getIrodsReadBufferSize() {
		return irodsReadBufferSize;
	}

	public void setIrodsReadBufferSize(int irodsReadBufferSize) {
		this.irodsReadBufferSize = irodsReadBufferSize;
	}

	public int getIrodsSocketTimeout() {
		return irodsSocketTimeout;
	}

	public void setIrodsSocketTimeout(int irodsSocketTimeout) {
		this.irodsSocketTimeout = irodsSocketTimeout;
	}
	
	// initialized properties
	private IRODSFileSystem irodsFileSystem;
	public IRODSFileSystem getIrodsFileSystem() {
		return irodsFileSystem;
	}

	public void setIrodsFileSystem(IRODSFileSystem irodsFileSystem) {
		this.irodsFileSystem = irodsFileSystem;
	}

	private IrodsFileStore objectStore;
	private IrodsFileStore datastreamStore;
	private ConnectionPool connectionPool;

	public IrodsLowlevelStorageModule(Map<String, String> moduleParameters, Server server, String role)
			throws ModuleInitializationException {
		super(moduleParameters, server, role);
		LOG.info("IrodsLowlevelStorageModule()");
	}

	@Override
	public void postInitModule() throws ModuleInitializationException {
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

		JargonProperties origProps = irodsFileSystem.getIrodsSession().getJargonProperties();
		SettableJargonProperties overrideJargonProperties = new SettableJargonProperties(origProps);
		overrideJargonProperties.setIrodsSocketTimeout(irodsSocketTimeout); // was 300
		overrideJargonProperties.setIrodsParallelSocketTimeout(irodsSocketTimeout); // was 300
		irodsFileSystem.getIrodsSession().setJargonProperties(overrideJargonProperties);
		objectStore = makeStore(objectStoreBase, OBJECT_REGISTRY_TABLE);
		datastreamStore = makeStore(datastreamStoreBase, DATASTREAM_REGISTRY_TABLE);
	}

	private IrodsFileStore makeStore(String storeBase, String registryTable) throws ModuleInitializationException {
		IrodsFileStore result = null;
		try {
			IrodsIFileSystem filesystem = new IrodsIFileSystem(irodsReadBufferSize, irodsFileSystem, account);
			Map<String, Object> dsConfig = new HashMap<String, Object>();
			dsConfig.put(REGISTRY_NAME, registryTable);
			dsConfig.put("storeBase", storeBase);
			dsConfig.put("storeBases", new String[] { storeBase });
			dsConfig.put("connectionPool", connectionPool);
			dsConfig.put("backslashIsEscape", "true");
			IrodsDBPathRegistry pathRegistry = new IrodsDBPathRegistry(irodsFileSystem, account, dsConfig);
			TimestampPathAlgorithm pathAlgorithm = new TimestampPathAlgorithm(storeBase);
			result = new IrodsFileStore(pathAlgorithm, pathRegistry, filesystem);
		} catch (LowlevelStorageException e) {
			throw new ModuleInitializationException(e.getMessage(), getRole());
		}
		return result;
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
