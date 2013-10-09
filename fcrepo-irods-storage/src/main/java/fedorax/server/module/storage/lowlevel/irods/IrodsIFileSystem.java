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
 * The original code was produced by Bing Zhu of DICE.
 */
package fedorax.server.module.storage.lowlevel.irods;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.commons.codec.binary.Hex;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.IRODSGenQueryExecutor;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileOutputStream;
import org.irods.jargon.core.pub.io.SessionClosingIRODSFileInputStream;
import org.irods.jargon.core.query.IRODSGenQuery;
import org.irods.jargon.core.query.IRODSQueryResultSet;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.RodsGenQueryEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an adapter to bridge between the Fedora default lowlevel storage file system interface and the iRODS
 * Jargon API. This adapter guarantees faithful transport for all write operations to iRODS via MD5 checksum comparison.
 * Any failed checksum comparison results in delete of the file in question from iRODS and throwing a
 * LowlevelStorageException. The class uses Java I/O buffered streams to prevent caller code from blocking when
 * possible. In addition to the standard Fedora lowlevel storage operations, this class has a getMetadata operation,
 * which queries iRODS for a standard set of system metadata.
 *
 * @author Gregory Jansen
 *
 */
public class IrodsIFileSystem {
	private static final Logger LOG = LoggerFactory.getLogger(IrodsIFileSystem.class);

	public IrodsIFileSystem(int irodsBufferSize, IRODSFileSystem irodsFileSystem, IRODSAccount account)
			throws LowlevelStorageException {
		LOG.debug("IrodsIFileSystem.IrodsIFileSystem()");
		this.account = account;
		this.irodsBufferSize = irodsBufferSize;
		this.irodsFileSystem = irodsFileSystem;
	}

	private static class CopyResult {
		long size = 0;
		String md5 = null;
	}

	private IRODSFileSystem irodsFileSystem;

	// private static final int BUFFER_SIZE = 32768;
	// private static final int BUFFER_SIZE = 4194304;

	private static final CopyResult stream2streamCopy(InputStream in, OutputStream out) throws IOException {
		int BUFFER_SIZE = 8192;
		LOG.debug("IrodsIFileSystem.stream2streamCopy() start");
		CopyResult result = new CopyResult();
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = 0;
		try {
			MessageDigest messageDigest;
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				throw new IOException("Cannot compare checksums without MD5 algorithm.", e);
			}
			messageDigest.reset();
			while ((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
				messageDigest.update(buffer, 0, bytesRead);
				result.size = result.size + bytesRead;
				// for transport failure test add the following line:
				// buffer[0] = '!';
				out.write(buffer, 0, bytesRead);
			}
			out.flush();
			Hex hex = new Hex();
			result.md5 = new String(hex.encode(messageDigest.digest()));
		} catch (IOException e) {
			LOG.error("Unexpected", e);
			throw e;
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				LOG.warn("Exception while trying to close jargon output stream", e);
			}
		}
		LOG.debug("IrodsIFileSystem.stream2streamCopy() end");
		return result;
	}

	IRODSAccount account = null;
	// IRODSFileSystem conn = null;

	int irodsBufferSize;

	// int connectionsUsed = 0;
	// int currentConnectionUsage = 0;
	// boolean reuseConnections = false;

	/*
	 * protected IRODSFileSystem getFileSystem() throws IOException { // GJ - no connection reuse to facilitate recovery
	 * from broken socket // if (reuseConnections && conn != null && conn.isConnected() && // !conn.isClosed()) { //
	 * currentConnectionUsage++; // return conn; // } LOG.info("Getting iRODS connection #" + (connectionsUsed + 1) +
	 * ". Last connection used " + currentConnectionUsage + " times"); Exception thrown = null; for (int attempts = 1;
	 * attempts <= this.maxConnectAttempts; attempts++) { try { conn = (IRODSFileSystem)
	 * FileFactory.newFileSystem(irodsAccount); LOG.info("Created iRODS connection #" + connectionsUsed + 1);
	 * currentConnectionUsage = 1; connectionsUsed++; return conn; } catch (Exception e) {
	 * LOG.error("Failed to connect to iRODS, attempt " + attempts + " of " + this.maxConnectAttempts, e); thrown = e; }
	 * int[] waitSeconds = { 1, 5, 10, 20, 30 }; int sleep = 60; try { sleep = waitSeconds[attempts]; } catch (Exception
	 * ignored) { } try { LOG.info("Sleeping for " + sleep + " seconds before next iRODS connect attempt.");
	 * Thread.sleep(sleep * 1000); } catch (InterruptedException ignored) { } } throw new
	 * IOException("connect_to_irods() failed:" + thrown.getMessage(), thrown); }
	 */

	public final void delete(File file) throws LowlevelStorageException {
		this.delete(file.getPath());
	}

	private boolean delete(String path) throws LowlevelStorageException {
		try {
			IRODSFile ifile = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(path);
			return ifile.delete();
		} catch (JargonException e) {
			LOG.error("Problem deleting irods file", e);
			throw new LowlevelStorageException(true, "Problem deleting iRODS file", e);
		} finally {
			if (irodsFileSystem != null) {
				try {
					irodsFileSystem.close();
				} catch (JargonException ignored) {
				}
			}
		}
	}

	public boolean deleteDirectory(String directory) {
		try {
			return this.delete(directory);
		} catch (LowlevelStorageException e) {
			LOG.error("Unexpected",e);
			throw new Error("Unexpected", e);
		}
	}

	private String getMD5ChecksumFromIRODS(IRODSFile file) throws IOException {
		try {

			DataObjectAO doao = irodsFileSystem.getIRODSAccessObjectFactory().getDataObjectAO(account);
			return doao.computeMD5ChecksumOnDataObject(file);
		} catch (JargonException e) {
			LOG.error("Unexpected",e);
			throw new IOException("Cannot compute checksum for irods file", e);
		} finally {
			if (irodsFileSystem != null) {
				try {
					irodsFileSystem.close();
				} catch (JargonException ignored) {
				}
			}
		}
	}

	public boolean isDirectory(File file) {
		try {
			IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(file.getPath());
			return irodsFile.isDirectory();
		} catch (JargonException e) {
			LOG.error("Unexpected",e);
			throw new Error("Unexpected", e);
		} finally {
			if (irodsFileSystem != null) {
				try {
					irodsFileSystem.close();
				} catch (JargonException ignored) {
				}
			}
		}
	}

	public String[] list(File directory) {
		try {

			IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(directory.getPath());
			String[] subdirs = irodsFile.list();
			return subdirs;
		} catch (JargonException e) {
			LOG.error("Unexpected",e);
			throw new Error("Unexpected error with list", e);
		} finally {
			if (irodsFileSystem != null) {
				try {
					irodsFileSystem.close();
				} catch (JargonException ignored) {
				}
			}
		}
	}
	
	public void setStorageLevel(File file, String level) throws LowlevelStorageException {
		try {
			DataObjectAO doao = irodsFileSystem.getIRODSAccessObjectFactory().getDataObjectAO(account);
			AvuData avu = new AvuData(IrodsLowlevelStorageModule.STORAGE_LEVEL_HINT, level.trim(), null);
			doao.modifyAvuValueBasedOnGivenAttributeAndUnit(file.getPath(), avu);
		} catch(JargonException e) {
			throw new LowlevelStorageException(true, "Failed to set storage level metadata", e);
		}
	}

	public final InputStream read(File file) throws LowlevelStorageException {
		LOG.debug("IrodsIFileSystem->read(): " + file.getAbsolutePath() + " with buffer of " + irodsBufferSize);
		try {
			SessionClosingIRODSFileInputStream fis = irodsFileSystem.getIRODSFileFactory(account)
					.instanceSessionClosingIRODSFileInputStream(file.getPath());
			final long start = System.currentTimeMillis();
			BufferedInputStream bis = new BufferedInputStream(fis, irodsBufferSize) {
				int bytes = 0;

				@Override
				public void close() throws IOException {
					if (LOG.isInfoEnabled()) {
						long time = System.currentTimeMillis() - start;
						if (time > 0) {
							LOG.info("closed irods stream: " + bytes + " bytes at " + (bytes / time) + " kb/sec");
						}
					}
					super.close();
				}

				@Override
				public synchronized int read() throws IOException {
					bytes++;
					return super.read();
				}

				@Override
				public synchronized int read(byte[] b, int off, int len) throws IOException {
					bytes = bytes + len;
					return super.read(b, off, len);
				}
			};
			return bis;
		} catch (JargonException e) {
			LOG.error("Unexpected",e);
			throw new LowlevelStorageException(true, "could not obtain IRODS File System", e);
		}
	}

	/*
	 * public static class ReadTrapBufferedInputStream extends InputStream { private BufferedInputStream buffered = null;
	 * private IRODSFileSystem fileSystem = null;
	 *
	 * public ReadTrapBufferedInputStream(IRODSFileInputStream in, int bufferSize, IRODSFileSystem fs) { this.buffered =
	 * new BufferedInputStream(in, bufferSize); this.fileSystem = fs; }
	 *
	 * @Override public synchronized int read() throws IOException { try { return buffered.read(); } catch (IOException
	 * e) { this.fileSystem.close(); LOG.error("Closed fileSystem due to IOException: " + e.getMessage()); throw e; } }
	 *
	 * @Override public int read(byte[] b) throws IOException { try { return super.read(b); } catch (IOException e) {
	 * this.fileSystem.close(); LOG.error("Closed fileSystem due to IOException: " + e.getMessage()); throw e; } }
	 *
	 * @Override public synchronized int read(byte[] b, int off, int len) throws IOException { if (len == 0) { return 0;
	 * } try { return buffered.read(b, off, len); } catch (IOException e) { this.fileSystem.close();
	 * LOG.error("Closed fileSystem due to IOException: " + e.getMessage()); throw e; } }
	 *
	 * @Override public void close() throws IOException { super.close(); buffered.close(); }
	 *
	 * @Override public int available() throws IOException { return buffered.available(); }
	 *
	 * }
	 */

	public long rewrite(File file, InputStream content) throws LowlevelStorageException {
		LOG.debug("IrodsIFileSystem.rewrite(): " + file.getAbsolutePath());
		long now = new Date().getTime();
		boolean rollback = false;
		StringBuilder rollbackLog = new StringBuilder();
		try {

			IRODSFile destination = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(file.getAbsolutePath());
			IRODSFile temp = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(
					destination.getAbsolutePath() + ".temp." + now);
			IRODSFile old = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(
					destination.getAbsolutePath() + ".old." + now);
			IRODSFile trueLocation = irodsFileSystem.getIRODSFileFactory(account)
					.instanceIRODSFile(file.getAbsolutePath());

			// IRODSFile destination = new IRODSFile(fileSystem,
			// file.getAbsolutePath());
			// IRODSFile temp = new IRODSFile(fileSystem,
			// destination.getAbsolutePath() + ".temp." + now);
			// IRODSFile old = new IRODSFile(fileSystem,
			// destination.getAbsolutePath() + ".old." + now);
			// IRODSFile trueLocation = new IRODSFile(fileSystem,
			// file.getAbsolutePath());

			if (!destination.exists()) {
				throw new LowlevelStorageException(true, "File to rewrite does not exist! (" + destination + ")");
			}

			rollbackLog.append("iRODS FILE REPAIR NEEDED FOR A FAILED REWRITE\n");
			//temp.createNewFile();
			IRODSFileOutputStream out = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFileOutputStream(temp);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out, irodsBufferSize);
			CopyResult copyResult = stream2streamCopy(content, bufferedOutputStream);
			rollbackLog.append("DELETE: ").append(temp.getAbsolutePath()).append("\n");

			// get IRODS checksum
			String irodschecksum = this.getMD5ChecksumFromIRODS(temp);
			if (!copyResult.md5.equals(irodschecksum)) {
				LOG.debug("local and iRODS checksums DO NOT MATCH");
				rollback = true;
				if (temp.deleteWithForceOption()) {
					rollback = false;
				}
				throw new LowlevelStorageException(true, temp.getAbsolutePath() + " did not match local checksum");
			}
			if (!destination.renameTo(old)) {
				rollback = true;
				throw new LowlevelStorageException(true, destination.getAbsolutePath() + " could not be renamed to "
						+ old.getAbsolutePath());
			}
			rollbackLog.append("MOVE: ").append(old.getAbsolutePath()).append(" to ")
					.append(destination.getAbsolutePath()).append("\n");

			if (!temp.renameTo(trueLocation)) {
				rollback = true;
				throw new LowlevelStorageException(true, temp.getAbsolutePath() + " could not be renamed to "
						+ trueLocation.getAbsolutePath());
			}
			if (!old.deleteWithForceOption()) {
				rollback = true;
				throw new LowlevelStorageException(true, old.getAbsolutePath() + " could not be deleted");
			}
			return copyResult.size;
		} catch (Exception e) {
			rollback = true;
			throw new LowlevelStorageException(true, "IRODSFedoraFileSystem.rewrite(): [" + file.getAbsolutePath() + "]",
					e);
		} finally {
			if (rollback) {
				LOG.error(rollbackLog.toString());
			}
			try {
				irodsFileSystem.close();
			} catch (JargonException e) {
				throw new Error("Cannot close irods session", e);
			}
		}
	}

	public final long write(File file, InputStream content) throws LowlevelStorageException {
		LOG.debug("IrodsIFileSystem.write(): " + file.getAbsolutePath());
		try {

			LOG.debug("trying to write to: " + file.getPath());
			IRODSFile parentFile = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(file.getParent());
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(file.getPath());
			//irodsFile.createNewFile();
			IRODSFileOutputStream irodsFileOutputStream = irodsFileSystem.getIRODSFileFactory(account)
					.instanceIRODSFileOutputStream(irodsFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(irodsFileOutputStream, irodsBufferSize);

			CopyResult copyResult = stream2streamCopy(content, bufferedOutputStream);
			// get IRODS checksum
			String irodschecksum = this.getMD5ChecksumFromIRODS(irodsFile);
			if (!copyResult.md5.equals(irodschecksum)) {
				LOG.debug("local and iRODS checksums DO NOT MATCH");
				if (!irodsFile.delete()) {
					throw new LowlevelStorageException(true, irodsFile.getAbsolutePath()
							+ " did not match local checksum and could not be deleted");
				} else {
					throw new LowlevelStorageException(true, irodsFile.getAbsolutePath()
							+ " did not match local checksum, file was deleted");
				}
			}
			return copyResult.size;
		} catch (JargonException e) {
			throw new LowlevelStorageException(true, "IRODSFedoraFileSystem.write(): [" + file.getPath() + "]", e);
		} catch (IOException e) {
			throw new LowlevelStorageException(true, "IRODSFedoraFileSystem.write(): [" + file.getPath() + "]", e);
		} finally {
			if (irodsFileSystem != null) {
				try {
					irodsFileSystem.close();
				} catch (JargonException e) {
					LOG.error("There was an error closing the irods filesystem within LLS", e);
				}
			}
		}
	}

	public static RodsGenQueryEnum[] metadataFields = null;
	public static String selectQuery = null;

	static {
		metadataFields = new RodsGenQueryEnum[] { RodsGenQueryEnum.COL_DATA_SIZE, RodsGenQueryEnum.COL_D_MODIFY_TIME,
				RodsGenQueryEnum.COL_D_CREATE_TIME, RodsGenQueryEnum.COL_D_OWNER_NAME,
				RodsGenQueryEnum.COL_D_DATA_CHECKSUM, RodsGenQueryEnum.COL_DATA_VERSION, RodsGenQueryEnum.COL_DATA_NAME,
				RodsGenQueryEnum.COL_COLL_NAME, RodsGenQueryEnum.COL_DATA_REPL_NUM, RodsGenQueryEnum.COL_D_REPL_STATUS,
				RodsGenQueryEnum.COL_D_RESC_NAME, RodsGenQueryEnum.COL_R_LOC, RodsGenQueryEnum.COL_R_CLASS_NAME,
				RodsGenQueryEnum.COL_R_RESC_COMMENT, RodsGenQueryEnum.COL_R_RESC_INFO, RodsGenQueryEnum.COL_R_TYPE_NAME,
				RodsGenQueryEnum.COL_R_VAULT_PATH, RodsGenQueryEnum.COL_R_ZONE_NAME };
		StringBuilder s = new StringBuilder();
		s.append("select ");
		boolean first = true;
		for (RodsGenQueryEnum e : metadataFields) {
			if (first) {
				first = false;
			} else {
				s.append(", ");
			}
			s.append(e.getName());
		}
		selectQuery = s.toString();
	}

	public IRODSQueryResultSet getMetadata(String path) throws LowlevelStorageException {
		LOG.debug("IrodsIFileSystem.getMetadata(): " + path);
		// Map<RodsGenQueryEnum, String> result = new HashMap<RodsGenQueryEnum,
		// String>();

		try {

			IRODSFile irodsFile = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(path);
			IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem.getIRODSAccessObjectFactory();

			StringBuilder q = new StringBuilder(selectQuery);
			q.append(" where ");
			q.append(RodsGenQueryEnum.COL_COLL_NAME.getName());
			q.append(" = '");
			q.append(irodsFile.getParent()).append("'");
			q.append(" AND ");
			q.append(RodsGenQueryEnum.COL_DATA_NAME.getName());
			q.append(" = '");
			q.append(irodsFile.getName()).append("'");
			IRODSGenQuery irodsQuery = IRODSGenQuery.instance(q.toString(), 1);
			IRODSGenQueryExecutor irodsGenQueryExecutor = accessObjectFactory.getIRODSGenQueryExecutor(account);
			IRODSQueryResultSet resultSet = irodsGenQueryExecutor.executeIRODSQuery(irodsQuery, 0);
			return resultSet;
		} catch (JargonException e) {
			throw new LowlevelStorageException(true, "could not obtain IRODS File System", e);
		} catch (JargonQueryException e) {
			throw new LowlevelStorageException(true, "could not query IRODS File System", e);
		} finally {
			if (irodsFileSystem != null) {
				try {
					irodsFileSystem.close();
				} catch (JargonException ignored) {
				}
			}
		}
	}
}
