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
package fedorax.server.module.storage.lowlevel.irods;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.LowlevelStorageInconsistencyException;
import org.fcrepo.server.storage.lowlevel.DBPathRegistry;
import org.fcrepo.server.storage.lowlevel.PathAlgorithm;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.IRODSGenQueryExecutor;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.IRODSGenQuery;
import org.irods.jargon.core.query.IRODSQueryResultRow;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.RodsGenQueryEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a registry implementation based on Bill Niebel's DBPathRegistry for Fedora lowlevel storage. This
 * implementation takes advantage of the iRODS iCAT database lookups to efficiently list files and perform the four
 * auditing operations specified in the parent class. At some point it may make sense to divorce the iRODS
 * implementation from the Fedora base classes, but for now this works. I suspect that a completely separate module
 * implementation would require some abstract classes to become interfaces in Fedora.
 *
 * Note: This registry relies on standard configuration information for the parent class and stores it's lookup table in
 * the Fedora database schema.
 *
 * Note: Another registry idea is to store PID information in the iCAT database and simply use that as the Fedora
 * registry. While this is a clean way of normalizing path registry data, it will introduce some latency. It also
 * presents some big challenges for rebuilds and audits as these fields would need to be reset individually in the
 * current iRODS API.
 *
 * @author Gregory Jansen
 */
public class IrodsDBPathRegistry extends DBPathRegistry {

	private IRODSFileSystem irodsFileSystem;
	private IRODSAccount account;

	private static final Logger LOG = LoggerFactory.getLogger(IrodsDBPathRegistry.class);

	public IrodsDBPathRegistry(IRODSFileSystem irodsFileSystem, IRODSAccount account, Map<String, Object> configuration) throws LowlevelStorageException {
		super(configuration);
		this.irodsFileSystem = irodsFileSystem;
		this.account = account;
	}

	/*
	 * Perform the selected operation on the entire tree of files
	 *
	 * @see fedora.server.storage.lowlevel.PathRegistry#traverseFiles(java.lang.String [], int, boolean, int)
	 */
	@Override
	public void traverseFiles(String[] storeBases, int operation, boolean stopOnError, int report)
			throws LowlevelStorageException {
		IRODSSession irodsSession = null;
		try {
			IRODSFileFactory irodsFileFactory = irodsFileSystem.getIRODSFileFactory(account);
			for (int i = 0; i < storeBases.length; i++) {
				IRODSFile base = irodsFileFactory.instanceIRODSFile(storeBases[i]);
				if (!base.exists() || !base.isDirectory()) {
					throw new LowlevelStorageException(true, "Base directory could not be found in IRODS: " + storeBases[i]);
				}
				Set<String[]> files = getAllIrodsFilePaths(base);
				for (String[] file : files) {
					String dir = file[1];
					String filename = file[0];
					String pid = PathAlgorithm.decode(filename);
					String path = dir + "/" + filename;
					if (pid == null) {
						if (report != NO_REPORT) {
							LOG.error("unexpected file at [" + path + "]");
						}
						if (stopOnError) {
							throw new LowlevelStorageException(true, "unexpected file traversing object store at [" + path
									+ "]");
						}
					} else {
						switch (operation) {
							case REPORT_FILES: {
								if (report == FULL_REPORT) {
									LOG.info("file [" + path + "] would have pid [" + pid + "]");
								}
								break;
							}
							case REBUILD: {
								put(pid, path);
								if (report == FULL_REPORT) {
									LOG.info("added to registry: [" + pid + "] ==> [" + path + "]");
								}
								break;
							}
							case AUDIT_FILES: {
								String rpath = null;
								try {
									rpath = get(pid);
								} catch (LowlevelStorageException e) {
								}
								boolean matches = rpath.equals(path);
								if (report == FULL_REPORT || !matches) {
									LOG.info((matches ? "" : "ERROR: ")
											+ "["
											+ path
											+ "] "
											+ (matches ? "" : "NOT ")
											+ "in registry"
											+ (matches ? "" : "; pid [" + pid + "] instead registered as ["
													+ (rpath == null ? "[OBJECT NOT IN STORE]" : rpath) + "]"));
								}
							}
						}
					}
				}
			}
		} catch (JargonException e) {
			throw new LowlevelStorageException(true, "irods problem traversing files", e);
		} finally {
			if (irodsSession != null) {
				try {
					irodsSession.closeSession();
				} catch (JargonException ignored) {
				}
			}
		}
	}

	private Set<String[]> getAllIrodsFilePaths(IRODSFile base) throws LowlevelStorageException {
		Set<String[]> result = new HashSet<String[]>();
		String basepath;
		IRODSSession irodsSession = null;
		try {
			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();
			basepath = base.getCanonicalPath();

			StringBuilder s = new StringBuilder("select ");
			s.append(RodsGenQueryEnum.COL_DATA_NAME.getName()).append(", ");
			s.append(RodsGenQueryEnum.COL_COLL_NAME.getName());
			s.append(" where ");
			s.append(RodsGenQueryEnum.COL_COLL_NAME.getName());
			s.append(" like '");
			s.append(basepath).append("/%'");

			System.out.println("quering for all irods paths: " + s.toString());

			IRODSGenQuery irodsQuery = IRODSGenQuery.instance(s.toString(), 1000);
			IRODSGenQueryExecutor irodsGenQueryExecutor = accessObjectFactory.getIRODSGenQueryExecutor(account);
			IRODSQueryResultSet resultSet = irodsGenQueryExecutor.executeIRODSQuery(irodsQuery, 0);
			for (IRODSQueryResultRow r : resultSet.getResults()) {
				String[] f = new String[2];
				f[0] = r.getColumn(0);
				f[1] = r.getColumn(1);
				result.add(f);
			}
			while (resultSet.isHasMoreRecords()) {
				resultSet = irodsGenQueryExecutor.getMoreResults(resultSet);
				for (IRODSQueryResultRow r : resultSet.getResults()) {
					String[] f = new String[2];
					f[0] = r.getColumn(0);
					f[1] = r.getColumn(1);
					result.add(f);
				}
			}
		} catch (NullPointerException e) {
			throw new LowlevelStorageException(true, "file system was null", e);
		} catch (JargonException e) {
			throw new LowlevelStorageException(true, "Could not query file system for descendant files", e);
		} catch (JargonQueryException e) {
			throw new LowlevelStorageException(true, "Could not query file system for descendant files", e);
		} catch (IOException e) {
			throw new LowlevelStorageException(true, "Could not query file system for descendant files", e);
		} finally {
			if (irodsSession != null) {
				try {
					irodsSession.closeSession();
				} catch (JargonException ignored) {
				}
			}
		}
		return result;
	}

	protected Enumeration<String> getKeys() throws LowlevelStorageInconsistencyException, LowlevelStorageException {
		return keys();
	}
}
