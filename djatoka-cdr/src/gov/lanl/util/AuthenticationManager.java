package gov.lanl.util;


import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Storage class for holding onto authentication information
 * @author bbpennel
 *
 */
public class AuthenticationManager {
	private static final String TYPE_INLINE = "inline";
	private static final String TYPE_PROPERTIES = "properties";
	
	private static Logger logger = Logger.getLogger(AuthenticationManager.class);
	private HashMap<String,AuthenticationCredentials> authMap;
	
	public AuthenticationManager(Properties properties){
		logger.debug("Initializing Authentication manager");
		authMap = new HashMap<String,AuthenticationCredentials>();
		
		Iterator<Entry<Object,Object>> propIt = properties.entrySet().iterator();
		while (propIt.hasNext()){
			Entry<Object,Object> property = propIt.next();
			try {
				String key = (String)property.getKey();
				if (key.endsWith(".type")){
					String authPrefix = key.substring(0, key.indexOf(".type"));
					String type = (String)property.getValue();
					if (TYPE_INLINE.equals(type)){
						String domain = properties.getProperty(authPrefix + ".domain");
						if (domain != null){
							authMap.put(domain, new AuthenticationCredentials(
									properties.getProperty(authPrefix + ".user"),
									properties.getProperty(authPrefix + ".pass")));
						}
					} else if (TYPE_PROPERTIES.equals(type)){
						String path = properties.getProperty(authPrefix + ".properties.path");
						logger.debug("Getting external properties file: " + path);
						if (path != null){
							Properties domainProperties = new Properties();
							FileInputStream fis = new FileInputStream(new File(path));
							domainProperties.load(fis);
							fis.close();
							
							String domainProp = properties.getProperty(authPrefix + ".properties.domain");
							String userProp = properties.getProperty(authPrefix + ".properties.user");
							String passProp = properties.getProperty(authPrefix + ".properties.pass");
							
							if (domainProperties != null){
								authMap.put(domainProperties.getProperty(domainProp), new AuthenticationCredentials(
										domainProperties.getProperty(userProp),
										domainProperties.getProperty(passProp)));
							}
						}
					}
				}
				
			} catch (Exception e){
				logger.error("Failed to load authentication property: " + (String)property.getKey(), e);
			}
		}
	}
	
	public AuthenticationCredentials getDomainCredentials(String domain){
		return this.authMap.get(domain);
	}
	
	public static class AuthenticationCredentials {
		private String username;
		private String password;
		
		public AuthenticationCredentials(String username, String password) {
			this.username = username;
			this.password = password;
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
		public String toString(){
			return username + ":" + password;
		}
	}
	
}
