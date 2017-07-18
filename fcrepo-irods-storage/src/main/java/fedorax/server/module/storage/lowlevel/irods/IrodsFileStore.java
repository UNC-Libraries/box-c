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

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.fcrepo.common.FaultException;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.LowlevelStorageInconsistencyException;
import org.fcrepo.server.errors.ObjectAlreadyInLowlevelStorageException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.storage.lowlevel.PathAlgorithm;
import org.irods.jargon.core.query.IRODSQueryResultSet;

/**
 * This file store is based on the Fedora default lowlevel storage module
 * created by Bill Niebel. This store is highly iRODS-specific and was not
 * designed for more general use. It combines an iRODS filesystem with any path
 * algorithm and an iRODS path registry. It provides an additional iRODS
 * metadata query method, which retrieves storage metadata for any object or
 * datastream (version) PID.
 * 
 * @author Gregory Jansen
 * 
 */
public class IrodsFileStore {
	private final PathAlgorithm pathAlgorithm;

	private final IrodsDBPathRegistry pathRegistry;

	private final IrodsIFileSystem fileSystem;

	private IrodsPathCache irodsPathCache = new IrodsPathCache();

	// private final String storeBase;

	public IrodsFileStore(PathAlgorithm pathAlgorithm,
			IrodsDBPathRegistry pathRegistry, IrodsIFileSystem fileSystem)
			throws LowlevelStorageException {
		this.pathAlgorithm = pathAlgorithm;
		this.fileSystem = fileSystem;
		this.pathRegistry = pathRegistry;
	}

	/**
	 * Gets the keys of all stored items.
	 * 
	 * @return an iterator of all keys.
	 */
	public Iterator<String> list() {
		try {
			final Enumeration<String> keys = pathRegistry.getKeys();
			return new Iterator<String>() {
				public boolean hasNext() {
					return keys.hasMoreElements();
				}

				public String next() {
					return keys.nextElement();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		} catch (LowlevelStorageException e) {
			throw new FaultException(e);
		}
	}

	/**
	 * compares a. path registry with OS files; and b. OS files with registry
	 */
	public void audit() throws LowlevelStorageException {
		pathRegistry.auditFiles();
		pathRegistry.auditRegistry();
	}

	/** recreates path registry from OS files */
	public void rebuild() throws LowlevelStorageException {
		pathRegistry.rebuild();
	}

	/**
	 * add to lowlevel store content of Fedora object not already in lowlevel
	 * store
	 * 
	 * @return
	 */
	public final long add(String pid, InputStream content,
			Map<String, String> hints) throws LowlevelStorageException {
		String filePath;
		File file = null;
		try { // check that object is not already in store
			filePath = pathRegistry.get(pid);
			ObjectAlreadyInLowlevelStorageException already = new ObjectAlreadyInLowlevelStorageException(
					"" + pid);
			throw already;
		} catch (ObjectNotInLowlevelStorageException not) {
			// OK: keep going
		}
		filePath = pathAlgorithm.get(pid);
		if (filePath == null || filePath.equals("")) { // guard against
			// algorithm
			// implementation
			LowlevelStorageException nullPath = new LowlevelStorageException(
					true, "null path from algorithm for pid " + pid);
			throw nullPath;
		}

		try {
			file = new File(filePath);
		} catch (Exception eFile) { // purposefully general catch-all
			LowlevelStorageException newFile = new LowlevelStorageException(
					true, "couldn't make File for " + filePath, eFile);
			throw newFile;
		}
		long size = fileSystem.write(file, content);
		if (hints != null && hints.containsKey(IrodsLowlevelStorageModule.STORAGE_LEVEL_HINT)) {
			fileSystem.setStorageLevel(file,
					hints.get(IrodsLowlevelStorageModule.STORAGE_LEVEL_HINT));
		}
		pathRegistry.put(pid, filePath);
		irodsPathCache.put(pid, filePath);
		return size;
	}

	/**
	 * replace into low-level store content of Fedora object already in lowlevel
	 * store
	 * 
	 * @return
	 */
	public final long replace(String pid, InputStream content,
			Map<String, String> hints) throws LowlevelStorageException {
		String filePath;
		File file = null;
		try {
			filePath = pathRegistry.get(pid);
		} catch (ObjectNotInLowlevelStorageException ffff) {
			LowlevelStorageException noPath = new LowlevelStorageException(
					false, "pid " + pid + " not in registry", ffff);
			throw noPath;
		}
		if (filePath == null || filePath.equals("")) { // guard against registry
			// implementation
			LowlevelStorageException nullPath = new LowlevelStorageException(
					true, "pid " + pid + " not in registry");
			throw nullPath;
		}

		try {
			file = new File(filePath);
		} catch (Exception eFile) { // purposefully general catch-all
			LowlevelStorageException newFile = new LowlevelStorageException(
					true, "couldn't make new File for " + filePath, eFile);
			throw newFile;
		}
		long result = fileSystem.rewrite(file, content);
		if (hints != null && hints.containsKey(IrodsLowlevelStorageModule.STORAGE_LEVEL_HINT)) {
			fileSystem.setStorageLevel(file,
					hints.get(IrodsLowlevelStorageModule.STORAGE_LEVEL_HINT));
		}
		return result;
	}

	/** get content of Fedora object from low-level store */
	public final InputStream retrieve(String pid)
			throws LowlevelStorageException {
		String filePath = this.getIrodsPath(pid);
		File file;

		try {
			filePath = this.getIrodsPath(pid);
		} catch (ObjectNotInLowlevelStorageException eReg) {
			throw eReg;
		}

		if (filePath == null || filePath.equals("")) { // guard against registry
			// implementation
			LowlevelStorageException nullPath = new LowlevelStorageException(
					true, "null path from registry for pid " + pid);
			throw nullPath;
		}

		try {
			file = new File(filePath);
		} catch (Exception eFile) { // purposefully general catch-all
			LowlevelStorageException newFile = new LowlevelStorageException(
					true, "couldn't make File for " + filePath, eFile);
			throw newFile;
		}

		return fileSystem.read(file);
	}

	/** remove Fedora object from low-level store */
	public final void remove(String pid) throws LowlevelStorageException {
		String filePath;
		File file = null;

		try {
			filePath = pathRegistry.get(pid);
		} catch (ObjectNotInLowlevelStorageException eReg) {
			throw eReg;
		}
		if (filePath == null || filePath.equals("")) { // guard against registry
			// implementation
			LowlevelStorageException nullPath = new LowlevelStorageException(
					true, "null path from registry for pid " + pid);
			throw nullPath;
		}

		try {
			file = new File(filePath);
		} catch (Exception eFile) { // purposefully general catch-all
			LowlevelStorageException newFile = new LowlevelStorageException(
					true, "couldn't make File for " + filePath, eFile);
			throw newFile;
		}
		pathRegistry.remove(pid);
		fileSystem.delete(file);
	}

	public IRODSQueryResultSet getMetadata(String pid)
			throws LowlevelStorageException {
		String filePath;
		filePath = pathRegistry.get(pid);

		if (filePath == null || filePath.equals("")) { // guard against registry
			// implementation
			LowlevelStorageException nullPath = new LowlevelStorageException(
					true, "null path from registry for pid " + pid);
			throw nullPath;
		}
		return fileSystem.getMetadata(filePath);
	}

	/**
	 * @param dsID
	 * @return the iRODS URI location of the cache replica of the file
	 * @throws LowlevelStorageException
	 * @throws LowlevelStorageInconsistencyException
	 * @throws ObjectNotInLowlevelStorageException
	 */
	public String getIrodsPath(String id) throws LowlevelStorageException {
		String filePath = this.getIrodsPathCache().get(id);
		if (filePath == null) {
			filePath = pathRegistry.get(id);
			this.getIrodsPathCache().put(id, filePath);
		}
		return filePath;
	}

	public IrodsPathCache getIrodsPathCache() {
		return irodsPathCache;
	}
}
