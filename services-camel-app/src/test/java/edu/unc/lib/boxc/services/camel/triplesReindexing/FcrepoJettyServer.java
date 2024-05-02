package edu.unc.lib.boxc.services.camel.triplesReindexing;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

/**
 * @author bbpennel
 */
public class FcrepoJettyServer {
    private static Server jettyServer;
    private static final int PORT = 48085;
    private Path userPropertiesPath;

    public FcrepoJettyServer() {
    }

    public void start() throws Exception {
        System.setProperty("fcrepo.modeshape.configuration", "classpath:/config/file-simple/repository.json");
        System.setProperty("com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean.default.objectStoreDir", "target/test-resources/objectStoreDefault");
        System.setProperty("com.arjuna.ats.arjuna.objectstore.objectStoreDir", "target/test-resources/objectStore");

        jettyServer = new Server(PORT);

        Path userPropertiesPath = Paths.get("target/test-resources/users.properties");
        if (!Files.exists(userPropertiesPath)) {
            Files.createFile(userPropertiesPath);
        }

        // Create a HashLoginService with no users
        HashLoginService loginService = new HashLoginService();
        loginService.setConfig(userPropertiesPath.toString());
        loginService.setName("fcrepo");
//        DefaultIdentityService identityService = new DefaultIdentityService();

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setLoginService(loginService);
        securityHandler.setIdentityService(loginService.getIdentityService());
        var basicAuthenticator = new BasicAuthenticator();
        basicAuthenticator.setConfiguration(securityHandler);

        securityHandler.setAuthenticator(basicAuthenticator);
//        jettyServer.setHandler(securityHandler);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        // Map the constraint to the desired URL pattern
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);

//         Add the constraint mapping to the security handler
        securityHandler.addConstraintMapping(mapping);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        webAppContext.setWar("/Users/bbpennel/git/boxc/services-camel-app/target/test-resources/fcrepo-webapp.war");

//        securityHandler.setHandler(webAppContext);
        webAppContext.setSecurityHandler(securityHandler);
        jettyServer.setHandler(webAppContext);
        jettyServer.start();
    }

    public void stop() throws Exception {
        jettyServer.stop();
        Files.deleteIfExists(userPropertiesPath);
    }
}
