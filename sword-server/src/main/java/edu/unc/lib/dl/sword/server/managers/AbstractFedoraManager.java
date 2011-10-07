package edu.unc.lib.dl.sword.server.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.servlets.SwordServlet;
import org.w3c.dom.Element;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.sword.server.FedoraAuthException;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public abstract class AbstractFedoraManager implements ApplicationContextAware {
	protected static ApplicationContext context;
	protected static AccessClient accessClient;
	protected static TripleStoreQueryService tripleStoreQueryService;
	protected static PID collectionsPidObject;
	protected static String swordPath;

	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(this.getClass().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		context = arg0;
		accessClient = (AccessClient) context.getBean("accessClient");
		tripleStoreQueryService = (TripleStoreQueryService) context.getBean("tripleStoreQueryService");
		collectionsPidObject = tripleStoreQueryService.fetchByRepositoryPath("/Collections");
		swordPath = (String) context.getBean("swordPath");
	}

	public void authenticate(AuthCredentials auth) throws SwordAuthException, SwordServerException {
		HttpClient client = new HttpClient();
		UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
				auth.getUsername(), auth.getPassword());
		client.getState().setCredentials(new AuthScope(null, 443), cred);
		client.getState().setCredentials(new AuthScope(null, 80), cred);

		GetMethod method = new GetMethod(accessClient.getFedoraContextUrl());
		
		try {
			method.setDoAuthentication(true);
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				return;
			} else if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
				throw new FedoraAuthException();
			} else {
				throw new SwordServerException();
			}
		} catch (HttpException e){
			throw new SwordServerException(e);
		} catch (IOException e) {
			throw new SwordServerException(e);
		} finally {
			method.releaseConnection();
		}
	}
}
