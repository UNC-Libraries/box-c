package edu.unc.lib.dl.sword.server.managers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public abstract class AbstractFedoraManager implements ApplicationContextAware {
	protected ApplicationContext context;
	protected AccessClient accessClient;
	protected TripleStoreQueryService tripleStoreQueryService;
	protected PID collectionsPidObject;
	protected String swordPath;
	
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
		accessClient = (AccessClient)context.getBean("accessClient");
		tripleStoreQueryService = (TripleStoreQueryService)context.getBean("tripleStoreQueryService");
		collectionsPidObject = tripleStoreQueryService.fetchByRepositoryPath("/Collections");
		swordPath = (String)context.getBean("swordPath");
	}
}
