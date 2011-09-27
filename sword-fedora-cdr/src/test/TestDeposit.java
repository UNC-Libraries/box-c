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

import java.net.URL;

import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.JDOMException;

public class TestDeposit {
	private Namespace SWORD = Namespace.getNamespace("sword","http://purl.org/net/sword/");
	private Namespace ATOM = Namespace.getNamespace("atom","http://www.w3.org/2005/Atom");

	protected String _depositUri = null;
	protected String _limitedDepositUri = null;

	public TestDeposit() {
		_depositUri = System.getProperty("deposit_uri");
		_limitedDepositUri = System.getProperty("limited-deposit_uri");
	}

	protected HttpClient getClient() {
		HttpClient tClient = new HttpClient();
		tClient.getParams().setAuthenticationPreemptive(true);

		Credentials tUserPass = new UsernamePasswordCredentials("sword", "sword");
		tClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), tUserPass);
	
		return tClient;
	}

	@Test
	public void testWrongPackaging() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/mets/dspace");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Should have thrown a 415 error but looks like it has succeded", 415, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertEquals("Wrong root element. ", tEntityDoc.getRootElement().getName(), "error");
		assertEquals("Wrong error URI given ", tEntityDoc.getRootElement().getAttributeValue("href"), "http://purl.org/net/sword/error/ErrorContent");

		assertNotNull("Missing title" , tEntityDoc.getRootElement().getChild("title", ATOM));
		assertNotNull("Missing summary" , tEntityDoc.getRootElement().getChild("summary", ATOM));
		assertNotNull("Missing updated" , tEntityDoc.getRootElement().getChild("updated", ATOM));
		assertNotNull("Missing userAgent" , tEntityDoc.getRootElement().getChild("userAgent", SWORD));

	} 
	
	@Test
	public void testCorrectPackaging() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing content element", tEntityDoc.getRootElement().getChild("content", ATOM));
		Element tContent = tEntityDoc.getRootElement().getChild("content", ATOM);
		assertEquals("Invalid content type", tContent.getAttributeValue("type"), "application/zip");

		assertNotNull("Missing packaging element", tEntityDoc.getRootElement().getChild("packaging", SWORD));
		Element tPackaging = tEntityDoc.getRootElement().getChild("packaging", SWORD);
		assertEquals("Invalid content type", tPackaging.getText(), "http://purl.org/net/sword-types/METSDSpaceSIP");
	}

	@Test
	public void testWrongContentType() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/TOTALY_UNSUPPORTED");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/mets/dspace");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Should have thrown a 415 error but looks like it has succeded", 415, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertEquals("Wrong root element. ", tEntityDoc.getRootElement().getName(), "error");
		assertEquals("Wrong error URI given ", tEntityDoc.getRootElement().getAttributeValue("href"), "http://purl.org/net/sword/error/ErrorContent");

		assertNotNull("Missing title" , tEntityDoc.getRootElement().getChild("title", ATOM));
		assertNotNull("Missing summary" , tEntityDoc.getRootElement().getChild("summary", ATOM));
		assertNotNull("Missing updated" , tEntityDoc.getRootElement().getChild("updated", ATOM));
		assertNotNull("Missing userAgent" , tEntityDoc.getRootElement().getChild("userAgent", SWORD));

	}

	@Test
	public void testNonMediatedAuthor() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing author element", tEntityDoc.getRootElement().getChild("author", ATOM));
		assertNotNull("Missing name element", tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM));
		Element tAuthorName = tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM);
		assertEquals("Author name incorrect", tAuthorName.getText(), "sword");
	}

	@Test
	public void testMediatedAuthor() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-On-Behalf-Of", "Glen");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing author element", tEntityDoc.getRootElement().getChild("author", ATOM));
		assertNotNull("Missing name element", tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM));
		Element tAuthorName = tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM);
		assertEquals("Author name incorrect", tAuthorName.getText(), "sword");

		assertNotNull("Missing contributor element", tEntityDoc.getRootElement().getChild("contributor", ATOM));
		assertNotNull("Missing name element", tEntityDoc.getRootElement().getChild("contributor", ATOM).getChild("name", ATOM));
		Element tContributerName = tEntityDoc.getRootElement().getChild("contributor", ATOM).getChild("name", ATOM);
		assertEquals("Contributer name incorrect", tContributerName.getText(), "Glen");
	}

	@Test
	public void testUnkownMediatedAuthor() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_limitedDepositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-On-Behalf-Of", "THIS_USER_SHOULD_NOT_EXIST");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 401 result", 401, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertEquals("Wrong root element. ", tEntityDoc.getRootElement().getName(), "error");
		assertEquals("Wrong error URI given ", tEntityDoc.getRootElement().getAttributeValue("href"), "http://purl.org/net/sword/error/TargetOwnerUnknown");

		assertNotNull("Missing title" , tEntityDoc.getRootElement().getChild("title", ATOM));
		assertNotNull("Missing summary" , tEntityDoc.getRootElement().getChild("summary", ATOM));
		assertNotNull("Missing updated" , tEntityDoc.getRootElement().getChild("updated", ATOM));
		assertNotNull("Missing userAgent" , tEntityDoc.getRootElement().getChild("userAgent", SWORD));
	}

	@Test
	public void testVerbose() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-Verbose", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing verbose element", tEntityDoc.getRootElement().getChild("verboseDescription", SWORD));
	}
	
	@Test
	public void testNoVerbose() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-Verbose", "false");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNull("Verbose element present where should be removed", tEntityDoc.getRootElement().getChild("verboseDescription", SWORD));
	}

	@Test
	public void testNoOp() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "image/jpg");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-Verbose", "false");
		tPost.setRequestEntity(new InputStreamRequestEntity(new URL("http://www.swordapp.org/logo.jpg").openStream()));
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		Element tContent = tEntityDoc.getRootElement().getChild("content", ATOM);
		assertNotNull("Missing content element", tContent);

		GetMethod tMethod = new GetMethod(tContent.getAttributeValue("src"));

		tStatus = this.getClient().executeMethod(tMethod);

		assertEquals("No op set so should return 404 from " + tContent.getAttributeValue("src") + ". This will always fail on fedora 2.x because it doesn't throw the correct errors", 404, tStatus);
	}

	@Test
	public void testOp() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "image/jpg");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-Verbose", "false");
		tPost.setRequestEntity(new InputStreamRequestEntity(new URL("http://www.swordapp.org/logo.jpg").openStream()));
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		Element tContent = tEntityDoc.getRootElement().getChild("content", ATOM);
		assertNotNull("Missing content element", tContent);

		GetMethod tMethod = new GetMethod(tContent.getAttributeValue("src"));

		tStatus = this.getClient().executeMethod(tMethod);

		assertEquals("No op isn't set so should return 200", 200, tStatus);
	}


	@Test
	public void testUserAgent() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing user agent element", tEntityDoc.getRootElement().getChild("userAgent", SWORD));
		
		assertNotNull("Missing server user agent element", tEntityDoc.getRootElement().getChild("generator", ATOM));
	}

	@Test
	public void testLocation() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		String tLocation = tPost.getResponseHeader("Location").getValue();
		assertNotNull("Missing Location in header", tLocation);
		assertNotNull("Missing link element", tEntityDoc.getRootElement().getChild("link", ATOM));

		Element tEditLink = tEntityDoc.getRootElement().getChild("link", ATOM);
		assertEquals("Location header doesn't match link href", tEditLink.getAttributeValue("href"), tLocation);
	}
		
	/**
	 *  Test section 1.3 Package description entry document of SWORD 1.3 Spec
	 */
	@Test
	public void testContentType() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing Content type in header", tPost.getResponseHeader("Content-Type"));
		String tContentType = tPost.getResponseHeader("Content-Type").getValue();
		assertNotNull("Missing Content type in header", tContentType);
		Element tContent = tEntityDoc.getRootElement().getChild("content", ATOM);
		assertNotNull("Missing content element", tContent);

		//assertEquals("Content header and content element type aren't equal", tContentType, tContent.getAttributeValue("type"));
		assertEquals("Content header not equal to what was submitted", tContent.getAttributeValue("type"), "application/zip");
	}

	@Test
	public void testGetEntry() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		String tLocation = tPost.getResponseHeader("Location").getValue();
		assertNotNull("Missing Location in header", tLocation);
		
		GetMethod tMethod = new GetMethod(tLocation);

		tStatus = this.getClient().executeMethod(tMethod);
		assertEquals("Get returned a non 200 result", 200, tStatus);

		Document tSavedEntiyDoc = tBuilder.build(tMethod.getResponseBodyAsStream());
		for (int i = 0; i < tSavedEntiyDoc.getRootElement().getChildren().size(); i++) {
			Element tCurEl = (Element)tSavedEntiyDoc.getRootElement().getChildren().get(i);
			assertEquals("Elements '" + tCurEl.getName() + "' are not equal. ", tCurEl.getText(), tEntityDoc.getRootElement().getChild(tCurEl.getName(), tCurEl.getNamespace()).getText());
		}
	}

	@Test
	public void testSuggestedFilename() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "image/jpg");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("Content-Disposition", "logo.jpg");
		tPost.setRequestEntity(new InputStreamRequestEntity(new URL("http://www.swordapp.org/logo.jpg").openStream()));
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		Element tContent = tEntityDoc.getRootElement().getChild("content", ATOM);
		assertNotNull("Missing content element", tContent);

		String[] tURI = tContent.getAttributeValue("src").split("/");
		assertEquals("Content disposition ignored", "logo", tURI[tURI.length - 1]);
	}

	@Test
	public void testRequiredFieldsInEntryDoc() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "image/jpg");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-Verbose", "true");
		tPost.addRequestHeader("X-On-Behalf-Of", "gmr");
		tPost.setRequestEntity(new InputStreamRequestEntity(new URL("http://www.swordapp.org/logo.jpg").openStream()));
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		Element tContributor = tEntityDoc.getRootElement().getChild("contributor", ATOM);
		assertNotNull("Missing contributor element", tContributor);

		Element tGenerator = tEntityDoc.getRootElement().getChild("generator", ATOM);
		assertNotNull("Missing generator element", tGenerator);

		Element tUserAgent = tEntityDoc.getRootElement().getChild("userAgent", SWORD);
		assertNotNull("Missing user agent element", tUserAgent);

		Element tTreatement = tEntityDoc.getRootElement().getChild("treatment", SWORD);
		assertNotNull("Missing treatment element", tTreatement);

		Element tVerbose = tEntityDoc.getRootElement().getChild("verboseDescription", SWORD);
		assertNotNull("Missing verbose description element", tVerbose);

		Element tNoOp = tEntityDoc.getRootElement().getChild("noOp", SWORD);
		assertNotNull("Missing no op element", tNoOp);
		assertTrue("No op must have a value of true or false not " + tNoOp.getText(), tNoOp.getText().equals("true") || tNoOp.getText().equals("false"));

		Element tPackaging = tEntityDoc.getRootElement().getChild("packaging", SWORD);
		assertNotNull("Missing packaging element", tPackaging);
	}


	public static void main(final String pArgs[]) throws IOException, JDOMException {
		org.junit.runner.JUnitCore.main(TestDeposit.class.getName());
	}
}
