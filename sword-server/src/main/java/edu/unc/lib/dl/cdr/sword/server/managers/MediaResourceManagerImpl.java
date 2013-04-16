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
package edu.unc.lib.dl.cdr.sword.server.managers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.MediaResource;
import org.swordapp.server.MediaResourceManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.acl.util.AccessControlTransformationUtil;
import edu.unc.lib.dl.cdr.sword.server.MethodAwareInputStream;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.DatastreamPID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ErrorURIRegistry;

public class MediaResourceManagerImpl extends AbstractFedoraManager implements MediaResourceManager {
	private static Logger log = Logger.getLogger(MediaResourceManagerImpl.class);

	private String fedoraPath;
	private Map<String, Datastream> virtualDatastreamMap;

	@Override
	public MediaResource getMediaResourceRepresentation(String uri, Map<String, String> accept, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {

		log.debug("Retrieving media resource representation for " + uri);

		DatastreamPID targetPID = (DatastreamPID) extractPID(uri, SwordConfigurationImpl.EDIT_MEDIA_PATH + "/");

		Datastream datastream = Datastream.getDatastream(targetPID.getDatastream());
		if (datastream == null)
			datastream = virtualDatastreamMap.get(targetPID.getDatastream());

		if (datastream == null)
			throw new SwordError(ErrorURIRegistry.RESOURCE_NOT_FOUND, 404,
					"Media representations other than those of datastreams are not currently supported");

		HttpClient client = new HttpClient();

		UsernamePasswordCredentials cred = new UsernamePasswordCredentials(accessClient.getUsername(),
				accessClient.getPassword());
		client.getState().setCredentials(new AuthScope(null, 443), cred);
		client.getState().setCredentials(new AuthScope(null, 80), cred);

		GetMethod method = new GetMethod(fedoraPath + "/objects/" + targetPID.getPid() + "/datastreams/" + datastream.getName()
				+ "/content");

		InputStream inputStream = null;
		String mimeType = null;
		String lastModified = null;

		try {
			method.setDoAuthentication(true);
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				StringBuffer query = new StringBuffer();
				query.append("select $mimeType $lastModified from <%1$s>")
						.append(" where <%2$s> <%3$s> $mimeType and <%2$s> <%4$s> $lastModified").append(";");
				String formatted = String.format(query.toString(),
						tripleStoreQueryService.getResourceIndexModelUri(), targetPID.getURI() + "/" + datastream.getName(), 
						ContentModelHelper.FedoraProperty.mimeType.getURI().toString(),
						ContentModelHelper.FedoraProperty.lastModifiedDate.getURI().toString());
				List<List<String>> datastreamResults = tripleStoreQueryService.queryResourceIndex(formatted);
				if (datastreamResults.size() > 0) {
					mimeType = datastreamResults.get(0).get(0);
					lastModified = datastreamResults.get(0).get(1);
				}
				inputStream = new MethodAwareInputStream(method);
			} else if (method.getStatusCode() >= 500) {
				throw new SwordError(ErrorURIRegistry.RETRIEVAL_EXCEPTION, method.getStatusCode(), "Failed to retrieve "
						+ targetPID.getPid() + ": " + method.getStatusLine().toString());
			} else if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				throw new SwordError(ErrorURIRegistry.RESOURCE_NOT_FOUND, 404, "Object " + targetPID.getPid()
						+ " could not be found.");
			}
		} catch (HttpException e) {
			throw new SwordError(ErrorURIRegistry.RETRIEVAL_EXCEPTION, "An exception occurred while attempting to retrieve " + targetPID.getPid());
		} catch (IOException e) {
			throw new SwordError(ErrorURIRegistry.RETRIEVAL_EXCEPTION, "An exception occurred while attempting to retrieve " + targetPID.getPid());
		}
		
		// For the ACL virtual datastream, transform RELS-EXT into accessControl tag
		if ("ACL".equals(targetPID.getDatastream())) {
			try {
				log.debug("Converting response XML to ACL format");
				SAXBuilder saxBuilder = new SAXBuilder();
				Document relsExt = saxBuilder.build(inputStream);
				XMLOutputter outputter = new XMLOutputter();
				Element accessElement = AccessControlTransformationUtil.rdfToACL(relsExt.getRootElement());
				inputStream.close();
				inputStream = new ByteArrayInputStream(outputter.outputString(accessElement).getBytes());
			} catch (Exception e) {
				log.debug("Failed to parse response from " + targetPID.getDatastreamURI() + " into ACL format", e);
				throw new SwordError(ErrorURIRegistry.RETRIEVAL_EXCEPTION, "An exception occurred while attempting to retrieve " + targetPID.getPid());
			}
		}

		MediaResource resource = new MediaResource(inputStream, mimeType, null, true);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Date lastModifiedDate;
		try {
			lastModifiedDate = formatter.parse(lastModified);
			resource.setLastModified(lastModifiedDate);
		} catch (ParseException e) {
			log.error("Unable to set last modified date for " + uri, e);
		}

		return resource;
	}

	@Override
	public DepositReceipt replaceMediaResource(String uri, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteMediaResource(String uri, AuthCredentials auth, SwordConfiguration config) throws SwordError,
			SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub

	}

	@Override
	public DepositReceipt addResource(String uri, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
			throws SwordError, SwordServerException, SwordAuthException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFedoraPath() {
		return fedoraPath;
	}

	public void setFedoraPath(String fedoraPath) {
		this.fedoraPath = fedoraPath;
	}

	public void setVirtualDatastreamMap(Map<String, Datastream> virtualDatastreamMap) {
		this.virtualDatastreamMap = virtualDatastreamMap;
	}
}
