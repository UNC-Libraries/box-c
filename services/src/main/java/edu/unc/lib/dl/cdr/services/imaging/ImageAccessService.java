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
package edu.unc.lib.dl.cdr.services.imaging;

import java.io.InputStream;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.RemoteExecutionOfCommandsAO;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author Gregory Jansen
 * 
 */
public class ImageAccessService {
	private static final Logger LOG = LoggerFactory.getLogger(ImageAccessService.class);
	private ManagementClient managementClient = null;
	private IRODSAccount irodsAccount = null;
	private static String irodsCommand = "imageAccess";

	public InputStream get(String pidstr, String dsid, int width, int height, String format, String comment) throws Exception {
		InputStream result = null;
		// get irods path to JP2 data stream
		String dsLocation = null;
		String dsIrodsPath = null;

		PID pid = new PID(pidstr);

		try {
			Datastream ds = getManagementClient().getDatastream(pid, dsid, "");
			String vid = ds.getVersionID();
			Document foxml = getManagementClient().getObjectXML(pid);
			Element dsEl = FOXMLJDOMUtil.getDatastream(foxml, dsid);
			for (Object o : dsEl.getChildren("datastreamVersion", JDOMNamespaceUtil.FOXML_NS)) {
				if (o instanceof Element) {
					Element dsvEl = (Element) o;
					if (vid.equals(dsvEl.getAttributeValue("ID"))) {
						dsLocation = dsvEl.getChild("contentLocation", JDOMNamespaceUtil.FOXML_NS).getAttributeValue("REF");
						break;
					}
				}
			}
			dsIrodsPath = getManagementClient().getIrodsPath(dsLocation);
		} catch (FedoraException e) {
			LOG.error("Cannot get datastream details from fedora:" + pidstr, e);	
			throw e;
		}

		// convert <path-to-input> -strip -comment <comment> -resize <w>x<h> <format>:-
		StringBuilder args = new StringBuilder(); // arguments appended to script after the physical path to iRODS data
		args.append("\"").append(comment).append("\" ");
		args.append(width).append(" ").append(height).append(" ").append(format);

		// invoke convert script via jargon
		try {
			RemoteExecutionOfCommandsAO rexecAO = null;
			rexecAO = IRODSFileSystem.instance().getIRODSAccessObjectFactory()
					.getRemoteExecutionOfCommandsAO(this.getIrodsAccount());
			result = rexecAO.executeARemoteCommandAndGetStreamUsingAnIRODSFileAbsPathToDetermineHost(irodsCommand,
					args.toString(), dsIrodsPath);
		} catch (JargonException e) {
			throw e;
		}
		return result;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public IRODSAccount getIrodsAccount() {
		return irodsAccount;
	}

	public void setIrodsAccount(IRODSAccount irodsAccount) {
		this.irodsAccount = irodsAccount;
	}
}
