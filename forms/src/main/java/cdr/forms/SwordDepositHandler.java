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
package cdr.forms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Text.Type;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cdr.forms.DepositResult.Status;

import edu.unc.lib.dl.httpclient.HttpClientUtil;

public class SwordDepositHandler implements DepositHandler {
	
	@SuppressWarnings("unused") 
	private static final Logger LOG = LoggerFactory.getLogger(SwordDepositHandler.class);
	
	private String serviceUrl;
	private String username;
	private String password;
	public String getServiceUrl() {
		return serviceUrl;
	}
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	/* (non-Javadoc)
	 * @see cdr.forms.DepositHandler#deposit(java.lang.String, java.lang.String, java.io.InputStream)
	 */
	@Override
	public DepositResult deposit(String containerPid, String modsXml, String title, File depositData) {
      Abdera abdera = Abdera.getInstance();
      Factory factory = abdera.getFactory();
      Entry entry = factory.newEntry();
      String pid = "uuid:"+UUID.randomUUID().toString();
		entry.setId("urn:"+pid);
		entry.setSummary("mods and binary deposit", Type.TEXT);
		entry.setTitle(title);
		entry.setUpdated(new Date(System.currentTimeMillis()));
		Parser parser = abdera.getParser();
		Document<FOMExtensibleElement> doc = parser.parse(new ByteArrayInputStream(modsXml.getBytes()));
		entry.addExtension(doc.getRoot());
		
		StringWriter swEntry = new StringWriter();
		try {
			entry.writeTo(swEntry);
		} catch (IOException e2) {
			throw new Error(e2);
		}
		
		FilePart payloadPart;
		try {
			payloadPart = new FilePart("payload", title, depositData);
		} catch (FileNotFoundException e1) {
			throw new Error(e1);
		}
		payloadPart.setContentType("binary/octet-stream");
		payloadPart.setTransferEncoding("binary");
		
		FilePart atomPart = new FilePart("atom", 
				new ByteArrayPartSource("atom", swEntry.toString().getBytes()), "application/atom+xml", "utf-8");
		
		Part[] parts = {
		      payloadPart,
		      atomPart
		  };
		
		String depositPath = getServiceUrl() + "collection/" + containerPid;
		HttpClient client = HttpClientUtil.getAuthenticatedClient(depositPath, this.getUsername(), this.getPassword());
		client.getParams().setAuthenticationPreemptive(true);
		PostMethod post = new PostMethod(depositPath);
		RequestEntity multipartEntity = new MultipartRequestEntity(parts, post.getParams());
		String boundary = multipartEntity.getContentType();
		boundary = boundary.substring(boundary.indexOf("boundary=") + 9);
		Header header = new Header("Content-type", "multipart/related; type=application/atom+xml; boundary=" + boundary);
		post.addRequestHeader(header);
		post.setRequestEntity(multipartEntity);
		int responseCode;

		DepositResult result = new DepositResult();
		result.setObjectPid(pid);
		try {
			responseCode = client.executeMethod(post);
			if(responseCode >= 300) {
				LOG.error(String.valueOf(responseCode));
				LOG.error(post.getResponseBodyAsString());
				result.setStatus(Status.FAILED);
			} else {
				result.setStatus(Status.COMPLETE);
			}
		} catch (HttpException e) {
			LOG.error("Exception during SWORD deposit", e);
			throw new Error(e);
		} catch (IOException e) {
			LOG.error("Exception during SWORD deposit", e);
			throw new Error(e);
		}
		return result;
	}
}
