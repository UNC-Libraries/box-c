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
  * Date: 26th February 2009
  *
  */

import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;

import java.io.IOException;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.InputStream;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.JDOMException;

import java.net.URL;

import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

public class TestExampleDeposits {
	private static final Logger LOG = Logger.getLogger(TestExampleDeposits.class);

	private Namespace SWORD = Namespace.getNamespace("sword","http://purl.org/net/sword/");
	private Namespace ATOM = Namespace.getNamespace("atom","http://www.w3.org/2005/Atom");

	protected String _depositUri = null;
	protected String _fedoraURL = null;
	protected String _exampleDir = null; 

	public TestExampleDeposits() {
		_exampleDir = System.getProperty("example_dir");
		_depositUri = System.getProperty("deposit_uri");
		_fedoraURL = System.getProperty("fedora_base");

		LOG.debug("Example Dir: " + _exampleDir);
		LOG.debug("Deposit URI: " + _depositUri);
		LOG.debug("Fedora URL: " + _fedoraURL);
	}

	protected Document doPost(final InputStream pContent, final String pMimeType, final String pPackaging) throws IOException, JDOMException{
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", pMimeType);
		tPost.setRequestEntity(new InputStreamRequestEntity(pContent));
		if (pPackaging != null) {
			tPost.addRequestHeader("X-Packaging", pPackaging);
		}

		int tStatus = this.getClient().executeMethod(tPost);

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		return tBuilder.build(tPost.getResponseBodyAsStream());
	}

	protected HttpClient getClient() {
		HttpClient tClient = new HttpClient();
		tClient.getParams().setAuthenticationPreemptive(true);

		Credentials tUserPass = new UsernamePasswordCredentials("sword", "sword");
		tClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), tUserPass);
	
		return tClient;
	}

	protected boolean exists(final String pURL) throws IOException {
		GetMethod tMethod = new GetMethod(pURL);

		int tStatus = this.getClient().executeMethod(tMethod);

		return tStatus == 200;
	}

	protected void checkDS(final String pPID, final String pDS) throws IOException {
		String tURL = _fedoraURL + "/" + pPID + "/" + pDS;
		assertTrue("Missing datastream: " + tURL, this.exists(tURL));
	}

	protected void checkDiss(final String pPID, final String pDef, final String pMethod) throws IOException {
		String tURL = _fedoraURL + "/" + pPID + "/" + pDef + "/" + pMethod;
		assertTrue("Missing disseminator: " + tURL, this.exists(tURL));
	}

	@Test
	public void testExample1() throws IOException, JDOMException {
		Document tDoc = this.doPost(new URL("http://www.jisc.ac.uk/images/printLogo.gif").openStream(), "image/gif", null);

		assertNotNull("Missing content element", tDoc.getRootElement().getChild("content", ATOM));
	}

	@Test
	public void testExample2() throws IOException, JDOMException {
		Document tDoc = this.doPost(new URL("http://www.swordapp.org/logo.jpg").openStream(), "image/jpeg", null);

		assertNotNull("Missing content element", tDoc.getRootElement().getChild("content", ATOM));

		assertNotNull("Missing Id", tDoc.getRootElement().getChild("id", ATOM));
		Element tID = tDoc.getRootElement().getChild("id", ATOM);

		this.checkDiss(tID.getText(), "demo:1", "getThumbnail");
	}

	@Test
	public void testExample3() throws IOException, JDOMException {
		Document tDoc = this.doPost(new FileInputStream(new File(new File(_exampleDir, "Example 3"), "deposit.zip")), "application/zip", null);

		assertNotNull("Missing content element", tDoc.getRootElement().getChild("content", ATOM));

		assertNotNull("Missing Id", tDoc.getRootElement().getChild("id", ATOM));
		Element tID = tDoc.getRootElement().getChild("id", ATOM);

		this.checkDS(tID.getText(), "mets");
		this.checkDS(tID.getText(), "RELS-EXT");
		this.checkDS(tID.getText(), "DC");
		this.checkDS(tID.getText(), "test");
		this.checkDS(tID.getText(), "upload");
	}

	@Test
	public void testExample4() throws IOException, JDOMException {
		Document tDoc = this.doPost(new FileInputStream(new File(new File(_exampleDir, "Example 4"), "deposit.zip")), "application/zip", "http://www.loc.gov/METS/");

		assertNotNull("Missing content element", tDoc.getRootElement().getChild("content", ATOM));

		assertNotNull("Missing Id", tDoc.getRootElement().getChild("id", ATOM));
		Element tID = tDoc.getRootElement().getChild("id", ATOM);

		this.checkDS(tID.getText(), "RELS-EXT");
		this.checkDS(tID.getText(), "METS");
		this.checkDS(tID.getText(), "test");
		this.checkDS(tID.getText(), "DC");
		this.checkDS(tID.getText(), "MODS");
		this.checkDS(tID.getText(), "epdcx");
	}

	@Test
	public void testExample5() throws IOException, JDOMException {
		Document tDoc = this.doPost(new FileInputStream(new File(new File(_exampleDir, "Example 5"), "external_mets.xml")), "text/xml", "http://www.loc.gov/METS/");

		assertNotNull("Missing content element", tDoc.getRootElement().getChild("content", ATOM));

		assertNotNull("Missing Id", tDoc.getRootElement().getChild("id", ATOM));
		Element tID = tDoc.getRootElement().getChild("id", ATOM);

		this.checkDS(tID.getText(), "RELS-EXT");
		this.checkDS(tID.getText(), "METS");
		this.checkDS(tID.getText(), "test");
		this.checkDS(tID.getText(), "DC");
		this.checkDS(tID.getText(), "MODS");
		this.checkDS(tID.getText(), "epdcx");
	}

	public static void main(final String pArgs[]) throws IOException, SAXException {
		org.junit.runner.JUnitCore.main(TestExampleDeposits.class.getName());
	}
}
