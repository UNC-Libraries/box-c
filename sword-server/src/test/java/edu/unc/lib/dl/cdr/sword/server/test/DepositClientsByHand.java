package edu.unc.lib.dl.cdr.sword.server.test;

import java.io.File;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.httpclient.HttpClientUtil;

public class DepositClientsByHand {
	private static final Logger LOG = LoggerFactory.getLogger(DepositClientsByHand.class);
	
	@Test
	public void testUpload() throws Exception {
		String user = "";
		String password = "";
		String pid = "uuid:c9aba0a2-12e9-4767-822e-b285a37b07d7";
		String payloadMimeType = "text/xml";
		String payloadPath = "src/test/resources/dcDocument.xml";
		String metadataPath = "src/test/resources/atompubMODS.xml";
		String depositPath = "https://localhost:444/services/sword/collection/";
		String testSlug = "ingesttestslug";
		
		HttpClient client = HttpClientUtil.getAuthenticatedClient(depositPath + pid, user, password);
		client.getParams().setAuthenticationPreemptive(true);
		
		PostMethod post = new PostMethod(depositPath + pid);

		File payload = new File(payloadPath);
		File atom = new File(metadataPath);
		FilePart payloadPart = new FilePart("payload", payload);
		payloadPart.setContentType(payloadMimeType);
		payloadPart.setTransferEncoding("binary");
		
		
		Part[] parts = {
		      payloadPart,
		      new FilePart("atom", atom, "application/atom+xml", "utf-8")
		  };
		MultipartRequestEntity mpEntity = new MultipartRequestEntity(parts, post.getParams());
		String boundary = mpEntity.getContentType();
		boundary = boundary.substring(boundary.indexOf("boundary=") + 9);
		
		Header header = new Header("Content-type", "multipart/related; type=application/atom+xml; boundary=" + boundary);
		post.addRequestHeader(header);
		
		Header slug = new Header("Slug", testSlug);
		post.addRequestHeader(slug);
		
		post.setRequestEntity(mpEntity);
		
		
		LOG.debug("" + client.executeMethod(post));
	}
}
