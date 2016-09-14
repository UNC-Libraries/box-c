package edu.unc.lib.dl.fcrepo4;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-fedora-container.xml")
public class AbstractFedoraIT {

	protected static final int SERVER_PORT = Integer.parseInt(System.getProperty("fcrepo.dynamic.test.port", "8080"));

	protected static final String HOSTNAME = "localhost";

	protected static final String serverAddress = "http://" + HOSTNAME + ":" + SERVER_PORT + "/rest/";
}
