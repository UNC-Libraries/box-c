package org.purl.sword.server.fedora.fedoraObjects;

/**
  * Copyright (c) 2007, Aberystwyth University
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *  - Redistributions of source code must retain the above
  *    copyright notice, this list of conditions and the
  *    following disclaimer.
  *
  *  - Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  *  - Neither the name of the Centre for Advanced Software and
  *    Intelligent Systems (CASIS) nor the names of its
  *    contributors may be used to endorse or promote products derived
  *    from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
  * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  *
  * @author Glen Robson
  * @version 1.0
  * Date: 18 October 2007 
  *
  * This encapsualtes datastreams which are on the local file system
  * and need to be uploaded to fedora. These files are set to a Managed state
  */

import java.io.File;
import java.io.IOException;

import org.jdom.Element;
import org.jdom.Namespace; 

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;

import org.purl.sword.server.fedora.utils.XMLProperties;

import org.purl.sword.base.SWORDException;

import org.apache.log4j.Logger;

public class LocalDatastream extends Datastream {
	private static final Logger LOG = Logger.getLogger(LocalDatastream.class);
	protected String _path = "";
	protected String _uploadedURL = null;

	/**
	 * @param String datastream name 
	 * @param String Mime type
	 * @param String the absolute path for the datastream
	 */
	public LocalDatastream(final String pID, final String pMimeType, final String pPath) {
		super(pID, State.ACTIVE, ControlGroup.MANAGED, pMimeType);
		super.setLabel("SWORD Generic File Upload");
		
		this.setPath(pPath);
	}

	public String getPath() {
		return _path;
	}

	public void setPath(final String pPath) {
		_path = pPath;
	}

	/**
	 * This uploads the datastream to Fedora so that it is accessible on ingest
	 * 
	 * @param String Username for fedora repository
	 * @param String Password for fedora repository
	 *
	 * @throws IOException if there are problems accessing the file
	 * @throws SWORDException if there are problems contacting the repository
	 */
	public void upload(final String pUsername, final String pPassword) throws IOException, SWORDException {
		if (_uploadedURL == null) { // Ensure no one uploads same object twice
			if (new File(this.getPath()).exists()) {
				String body = uploadFollowRedirects(new XMLProperties().getFedoraURL() + "/management/upload", pUsername, pPassword);
			
				if (body == null) {
					body = "[empty response body]";
				}
				LOG.error("Body" + body);		
				_uploadedURL = body.trim().replaceAll("\n", " ");
			} else{
				LOG.error("File '" + this.getPath() + "' dosn't exist");
				throw new IOException("File doesn't exists: " + this.getPath());
			}

			// File has been uploaded now safe to delete the local copy

			new File(_path).delete();
		} 	
	}

	protected String uploadFollowRedirects(final String pURL, final String pUsername, final String pPassword) throws IOException {
		HttpClient tClient = new HttpClient();
		tClient.getParams().setAuthenticationPreemptive(true);

		Credentials tUserPass = new UsernamePasswordCredentials(pUsername, pPassword);
		tClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), tUserPass);

		// execute and get the response
		LOG.error("Uploading " + this.getPath() + " to " + pURL);
		PostMethod tPost = new PostMethod(pURL);
		tPost.getParams().setParameter("Connection","Keep-Alive");

		LOG.debug("Content length: " + tPost.getRequestHeader("Content-length") + " content type " + tPost.getRequestHeader("Content-Type"));
		tPost.setContentChunked(true);

		// add the file part
		Part[] parts = { new FilePart("file", new File(this.getPath())) };
		tPost.setRequestEntity(new MultipartRequestEntity(parts, tPost.getParams()));
		LOG.debug("Multipart length: " + tPost.getRequestEntity().getContentLength());

		int responseCode = tClient.executeMethod(tPost);
		if (responseCode >= 300 && responseCode <= 399) {
			Header tLocationHeader = tPost.getResponseHeader("location");
			// Redirected
			return this.uploadFollowRedirects(tLocationHeader.getValue(), pUsername, pPassword);
		}

		if (responseCode != 201) {
			LOG.error("Couldn't upload " + this.getPath() + " none 201 result: " + responseCode);
			throw new IOException("Couldn't upload file: " + this.getPath());
		} 
	
		return tPost.getResponseBodyAsString(); 
	}

	/**
	 * Converts this datastream into FOXML so it can be ingested. 
	 * @param Namespace the FOXML namespace
	 * @return Element the FOXML datastream node
	 */
	public Element dsToFOXML(final Namespace FOXML) {
		if (_uploadedURL == null) {
			throw new IllegalArgumentException("Please upload the datastream before ingesting");
		}
		
		Element tContentLocation = new Element("contentLocation", FOXML);
		tContentLocation.setAttribute("TYPE", "URL");
		tContentLocation.setAttribute("REF", _uploadedURL);

		return tContentLocation;
	}	
}
