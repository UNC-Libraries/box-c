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
package fedorax.server.module.storage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.commons.httpclient.Header;
import org.fcrepo.common.http.HttpInputStream;
import org.fcrepo.common.http.WebClient;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.HttpServiceNotFoundException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.security.BackendPolicies;
import org.fcrepo.server.security.BackendSecurity;
import org.fcrepo.server.security.BackendSecuritySpec;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.Property;
import org.fcrepo.server.utilities.ServerUtility;
import org.fcrepo.server.validation.ValidationUtility;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingArea;
import edu.unc.lib.staging.StagingException;
import fedorax.server.module.storage.lowlevel.irods.IrodsLowlevelStorageModule;

/**
 * @author Gregory Jansen
 * 
 */
public class IrodsExternalContentManager extends Module implements
		ExternalContentManager {
	private static final Logger LOG = LoggerFactory
			.getLogger(IrodsExternalContentManager.class);

	static {
		// Register IRODS URL Protocol Handler (see metadata project)
		// by making a static reference to the class that loads it
		@SuppressWarnings("unused")
		String foo = IrodsLowlevelStorageModule.REGISTRY_NAME;
	}

	/**
	 * Stages configuration JSON file
	 */
	private File stagesConfiguration;

	public File getStagesConfiguration() {
		return stagesConfiguration;
	}

	public void setStagesConfiguration(File stagesConfiguration) {
		this.stagesConfiguration = stagesConfiguration;
	}

	private Stages stages;
	
	public Stages getStages() {
		return this.stages;
	}

	// injected properties
	private IRODSAccount irodsAccount;

	private int irodsReadBufferSize;

	public IRODSAccount getIrodsAccount() {
		return irodsAccount;
	}

	public void setIrodsAccount(IRODSAccount irodsAccount) {
		this.irodsAccount = irodsAccount;
	}

	public int getIrodsReadBufferSize() {
		return irodsReadBufferSize;
	}

	public void setIrodsReadBufferSize(int irodsReadBufferSize) {
		this.irodsReadBufferSize = irodsReadBufferSize;
	}

	// runtime stats
	int connectionsUsed = 0;
	int currentConnectionUsage = 0;
	boolean reuseConnections = false;

	// constants
	private static final String DEFAULT_MIMETYPE = "text/plain";

	// initialized properties
	private String fedoraServerPort = "80";
	private String fedoraServerRedirectPort = "443";
	private WebClient m_http;

	/**
	 * @param moduleParameters
	 * @param server
	 * @param role
	 * @throws ModuleInitializationException
	 */
	public IrodsExternalContentManager(Map<String, String> moduleParameters,
			Server server, String role) throws ModuleInitializationException {
		super(moduleParameters, server, role);
	}

	/**
	 * Initializes the Module based on configuration parameters. The
	 * implementation of this method is dependent on the schema used to define
	 * the parameter names for the role of
	 * <code>fedora.server.storage.DefaultExternalContentManager</code>.
	 * 
	 * @throws ModuleInitializationException
	 *             If initialization values are invalid or initialization fails
	 *             for some other reason.
	 */
	@Override
	public void initModule() throws ModuleInitializationException {
		try {
			Server s_server = getServer();
			if (s_server != null) {
				fedoraServerPort = s_server.getParameter("fedoraServerPort");
				fedoraServerRedirectPort = s_server
						.getParameter("fedoraRedirectPort");
			}
			m_http = new WebClient();

			StringBuilder sb = new StringBuilder();
			BufferedReader r = null;
			try {
				r = new BufferedReader(new FileReader(this.stagesConfiguration));
				for (String line = r.readLine(); line != null; line = r
						.readLine()) {
					sb.append(line).append('\n');
				}
			} finally {
				if (r != null)
					r.close();
			}
			LOG.debug("local staging config:\n"+sb.toString());
			this.stages = new Stages(sb.toString(), new IRODSStageResolver(irodsAccount));
			for (StagingArea s : this.stages.getAllAreas().values()) {
				if (!s.isConnected()) {
					this.stages.connect(s.getURI());
					if (!s.isConnected()) {
						LOG.warn("Cannot connect to staging area: "
								+ s.getURI());
					}
				}
			}

			// TODO register Stages as MBean
			//ObjectName name = new ObjectName("edu.unc.lib.cdr:type=Stages");
			//MBeanServer mbs = this.getMBeanServer();
			//mbs.registerMBean(this.stages, name);

		} catch (Throwable th) {
			th.printStackTrace();
			throw new ModuleInitializationException(
					"[IrodsExternalContentManager] "
							+ "An external content manager "
							+ "could not be instantiated. The underlying error was a "
							+ th.getClass() + "The message was \""
							+ th.getMessage() + "\".", getRole());
		}
	}

	private MBeanServer getMBeanServer() {
		MBeanServer mbserver = null;
		ArrayList<MBeanServer> mbservers = MBeanServerFactory
				.findMBeanServer(null);

		if (mbservers.size() > 0) {
			mbserver = (MBeanServer) mbservers.get(0);
		}

		if (mbserver != null) {
			System.out.println("Found our MBean server");
		} else {
			mbserver = MBeanServerFactory.createMBeanServer();
		}

		return mbserver;
	}

	/*
	 * Retrieves the external content. Currently the protocols <code>file</code>
	 * and <code>http[s]</code> are supported.
	 * 
	 * @see
	 * fedora.server.storage.ExternalContentManager#getExternalContent(fedora
	 * .server.storage.ContentManagerParams)
	 */
	public MIMETypedStream getExternalContent(ContentManagerParams params)
			throws GeneralException, HttpServiceNotFoundException {
		LOG.debug("in getExternalContent(), url=" + params.getUrl());

		String protocol = params.getProtocol();
		URI uri = URI.create(params.getUrl());

		boolean staged = false;
		try {
			LOG.debug("manifestURI: "+uri);
			LOG.debug("stages: "+this.stages);
			LOG.debug("stages size: "+this.stages.getAllAreas().size());
			URI storageURI = this.stages.getStorageURI(uri);
			LOG.debug("storageURI: "+storageURI);
			staged = true;
			protocol = storageURI.getScheme();
			uri = storageURI;
		} catch(StagingException e) {
			LOG.warn("Exception throw resolving local URL", e);
		}
		LOG.debug("protocol is " + protocol + ", uri is " + uri);
		if (protocol == null && uri.toString().startsWith("irods://")) {
			return getFromIrods(uri, params.getMimeType());
		} else if (protocol == null || protocol.equals("file")) {
			return getFromFilesystem(uri, params.getMimeType(), staged,
					params);
		} else if (protocol.equals("http") || protocol.equals("https")) {
			try {
				return getFromWeb(params);
			} catch(ModuleInitializationException e) {
				throw new GeneralException(e.getMessage()+"("+params.getUrl()+")", e);
			}
		} else if (protocol.equals("irods")) {
			return getFromIrods(uri, params.getMimeType());
		}
		throw new GeneralException(
				"protocol for retrieval of external content not supported. URL: "
						+ params.getUrl());
	}

	/**
	 * @param params
	 * @return
	 */
	private MIMETypedStream getFromIrods(URI uri, String mimeType)
			throws HttpServiceNotFoundException, GeneralException {
		try {
			LOG.debug("uri: "+uri);
			IRODSFileFactory ff = IRODSFileSystem.instance()
					.getIRODSFileFactory(irodsAccount);
			IRODSFile file = ff.instanceIRODSFile(URLDecoder.decode(
					uri.getRawPath(), "UTF-8"));
			InputStream result = ff.instanceIRODSFileInputStream(file);
			final long start = System.currentTimeMillis();
			result = new BufferedInputStream(result, this.irodsReadBufferSize) {
				int bytes = 0;

				@Override
				public void close() throws IOException {
					if (LOG.isInfoEnabled()) {
						long time = System.currentTimeMillis() - start;
						if (time > 0) {
							LOG.info("closed irods stream: " + bytes
									+ " bytes at " + (bytes / time) + " kb/sec");
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
				public synchronized int read(byte[] b, int off, int len)
						throws IOException {
					bytes = bytes + len;
					return super.read(b, off, len);
				}

			};

			// if mimeType was not given, try to determine it automatically
			if (mimeType == null || mimeType.equalsIgnoreCase("")) {
				String irodsFilename = file.getName();
				if (irodsFilename != null) {
					mimeType = new MimetypesFileTypeMap()
							.getContentType(irodsFilename);
				}
				if (mimeType == null || mimeType.equalsIgnoreCase("")) {
					mimeType = DEFAULT_MIMETYPE;
				}
			}
			return new MIMETypedStream(mimeType, result,
					getPropertyArray(mimeType));
			/*
			 * } catch (AuthzException ae) { LOG.error(ae.getMessage(), ae);
			 * throw new
			 * HttpServiceNotFoundException("Policy blocked datastream resolution"
			 * , ae); } catch (GeneralException me) { LOG.error(me.getMessage(),
			 * me); throw me; }
			 */

		} catch (JargonException e) {
			throw new GeneralException("Problem getting iRODS input stream", e);
		} catch (Throwable th) {
			th.printStackTrace(System.err);
			// catch anything but generalexception
			LOG.error(th.getMessage(), th);
			throw new HttpServiceNotFoundException(
					"[FileExternalContentManager] "
							+ "returned an error.  The underlying error was a "
							+ th.getClass().getName() + "  The message "
							+ "was  \"" + th.getMessage() + "\"  .  ", th);
		}
	}

	/**
	 * @param mimeType
	 * @return
	 */
	private Property[] getPropertyArray(String mimeType) {
		Property[] props = new Property[1];
		Property ctype = new Property("Content-Type", mimeType);
		props[0] = ctype;
		return props;
	}

	/**
	 * Get a MIMETypedStream for the given URL. If user or password are
	 * <code>null</code>, basic authentication will not be attempted.
	 */
	private MIMETypedStream get(String url, String user, String pass,
			String knownMimeType) throws GeneralException {
		LOG.debug("DefaultExternalContentManager.get(" + url + ")");
		try {
			HttpInputStream response = m_http.get(url, true, user, pass);
			String mimeType = response.getResponseHeaderValue("Content-Type",
					knownMimeType);
			Property[] headerArray = toPropertyArray(response
					.getResponseHeaders());
			return new MIMETypedStream(mimeType, response, headerArray);
		} catch (Exception e) {
			throw new GeneralException("Error getting " + url, e);
		}
	}

	/**
	 * Convert the given HTTP <code>Headers</code> to an array of
	 * <code>Property</code> objects.
	 */
	private static Property[] toPropertyArray(Header[] headers) {

		Property[] props = new Property[headers.length];
		for (int i = 0; i < headers.length; i++) {
			props[i] = new Property();
			props[i].name = headers[i].getName();
			props[i].value = headers[i].getValue();
		}
		return props;
	}

	/**
	 * Creates a property array out of the MIME type and the length of the
	 * provided file.
	 * 
	 * @param file
	 *            the file containing the content.
	 * @return an array of properties containing content-length and
	 *         content-type.
	 */
	private static Property[] getPropertyArray(File file, String mimeType) {
		Property[] props = new Property[2];
		Property clen = new Property("Content-Length", Long.toString(file
				.length()));
		Property ctype = new Property("Content-Type", mimeType);
		props[0] = clen;
		props[1] = ctype;
		return props;
	}

	/**
	 * Get a MIMETypedStream for the given URL. If user or password are
	 * <code>null</code>, basic authentication will not be attempted.
	 * 
	 * @param params
	 * @return
	 * @throws HttpServiceNotFoundException
	 * @throws GeneralException
	 */
	private MIMETypedStream getFromFilesystem(URI uri, String mimeType,
			boolean staged, ContentManagerParams params)
			throws HttpServiceNotFoundException, GeneralException {
		LOG.debug("in getFile(), url=" + uri);

		try {
			URL fileUri = new URL(URLDecoder.decode(uri.toString(), "utf-8"));
			String path = fileUri.getPath();
			File cFile = new File(path).getCanonicalFile();

			// security check, staged files are in known locations
			if (!staged) {
				URI cURI = cFile.toURI();
				LOG.info("Checking resolution security on " + cURI);
				Authorization authModule = (Authorization) getServer()
						.getModule("fedora.server.security.Authorization");
				if (authModule == null) {
					throw new GeneralException(
							"Missing required Authorization module");
				}
				authModule.enforceRetrieveFile(params.getContext(),
						cURI.toString());
			}

			// if mimeType was not given, try to determine it automatically
			if (mimeType == null || mimeType.equalsIgnoreCase("")) {
				mimeType = determineMimeType(cFile);
			}
			InputStream in = new FileInputStream(cFile);
			return new MIMETypedStream(mimeType, in,
					getPropertyArray(cFile, mimeType));
		} catch (AuthzException ae) {
			LOG.error(ae.getMessage(), ae);
			throw new HttpServiceNotFoundException(
					"Policy blocked datastream resolution", ae);
		} catch (GeneralException me) {
			LOG.error(me.getMessage(), me);
			throw me;
		} catch (Throwable th) {
			th.printStackTrace(System.err);
			// catch anything but generalexception
			LOG.error(th.getMessage(), th);
			throw new HttpServiceNotFoundException(
					"[FileExternalContentManager] "
							+ "returned an error.  The underlying error was a "
							+ th.getClass().getName() + "  The message "
							+ "was  \"" + th.getMessage() + "\"  .  ", th);
		}
	}

	/**
	 * Retrieves external content via http or https.
	 * 
	 * @param url
	 *            The url pointing to the content.
	 * @param context
	 *            The Map containing parameters.
	 * @param mimeType
	 *            The default MIME type to be used in case no MIME type can be
	 *            detected.
	 * @return A MIMETypedStream
	 * @throws ModuleInitializationException
	 * @throws GeneralException
	 */
	private MIMETypedStream getFromWeb(ContentManagerParams params)
			throws ModuleInitializationException, GeneralException {
		String username = params.getUsername();
		String password = params.getPassword();
		boolean backendSSL = false;
		String url = params.getUrl();

		if (ServerUtility.isURLFedoraServer(url) && !params.isBypassBackend()) {
			BackendSecuritySpec m_beSS;
			BackendSecurity m_beSecurity = (BackendSecurity) getServer()
					.getModule("fedora.server.security.BackendSecurity");
			try {
				m_beSS = m_beSecurity.getBackendSecuritySpec();
			} catch (Exception e) {
				throw new ModuleInitializationException(
						"Can't intitialize BackendSecurity module (in default access) from Server.getModule",
						getRole());
			}
			Hashtable<String, String> beHash = m_beSS
					.getSecuritySpec(BackendPolicies.FEDORA_INTERNAL_CALL);
			username = beHash.get("callUsername");
			password = beHash.get("callPassword");
			backendSSL = new Boolean(beHash.get("callSSL")).booleanValue();
			if (backendSSL) {
				if (params.getProtocol().equals("http:")) {
					url = url.replaceFirst("http:", "https:");
				}
				url = url.replaceFirst(":" + fedoraServerPort + "/", ":"
						+ fedoraServerRedirectPort + "/");
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("************************* backendUsername: "
						+ username + "     backendPassword: " + password
						+ "     backendSSL: " + backendSSL);
				LOG.debug("************************* doAuthnGetURL: " + url);
			}

		}
		return get(url, username, password, params.getMimeType());
	}

	/**
	 * Determines the mime type of a given file
	 * 
	 * @param file
	 *            for which the mime type needs to be detected
	 * @return the detected mime type
	 */
	private String determineMimeType(File file) {
		String mimeType = new MimetypesFileTypeMap().getContentType(file);
		// if mimeType detection failed, fall back to the default
		if (mimeType == null || mimeType.equalsIgnoreCase("")) {
			mimeType = DEFAULT_MIMETYPE;
		}
		return mimeType;
	}

	@Override
	public void postInitModule() throws ModuleInitializationException {
		super.postInitModule();
		// check if Fedora is patched via ValidateURL utility thing
		try {
			ValidationUtility.validateURL(
					"irods://example.com:1247/fooZone/home/foo", "M");
		} catch (ValidationException e1) {
			String msg = "Fedora Server is not patched to support the IrodsExternalContentManager";
			LOG.error(msg, e1);
			throw new ModuleInitializationException(msg,
					"fedora.server.storage.ExternalContentManager", e1);
		}
	}

}
