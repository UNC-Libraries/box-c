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
package edu.unc.lib.dl.fedora;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import org.springframework.test.context.ContextConfiguration;

import edu.unc.lib.dl.fedora.ManagementClient.Format;

/**
 * This class tests exception conditions that should arise when Fedora is offline or the properties of the client are
 * misconfigured to make it unreachable.
 * @author Gregory Jansen
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class ManagementClientIT {

    @Autowired
    ApplicationContext beanFactory;

    private ManagementClient getManagementClient() {
	return this.beanFactory.getBean("managementClient", ManagementClient.class);

    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    @ExpectedException(ObjectExistsException.class)
    public void testIngestDuplicatePID() throws Exception {
	ManagementClient c = this.getManagementClient();

	try {
	    c.purgeObject(new PID("cdr-test:Container"), "testing ingest", false);
	} catch(NotFoundException e) {
	    // cool, proceed
	} catch(Exception e) {
	    throw new Error(e);
	}

	File testFile = new File("src/test/resources/ContainerModel.xml");
	InputStream xmlis = new FileInputStream(testFile);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i = xmlis.read(); i != -1; i = xmlis.read()) {
	    baos.write(i);
	}
	byte[] testData = baos.toByteArray();

	// first ingest should succeed..
	try {
	    c.ingestRaw(testData, Format.FOXML_1_1, "this is a test");
	} catch(ServiceException e) {
	    throw new Error(e);
	}

	// this should throw the checked exception
	c.ingestRaw(testData, Format.FOXML_1_1, "this is a test");
    }

    /**
     * Test method for {@link edu.unc.lib.dl.fedora.ManagementClient#ingest(org.jdom.Document, edu.unc.lib.dl.fedora.ManagementClient.Format, java.lang.String)}.
     */
    @Test
    @ExpectedException(Error.class)
    public void testUnknownHost() throws Exception {
	// break the Fedora URL and re-init
	ManagementClient c = this.getManagementClient();
	c.setFedoraContextUrl("http://aljoiweisojjkallwef.com/fedora/");
	c.init();

	File testFile = new File("src/test/resources/ContainerModel.xml");
	InputStream xmlis = new FileInputStream(testFile);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i = xmlis.read(); i != -1; i = xmlis.read()) {
	    baos.write(i);
	}
	c.ingestRaw(baos.toByteArray(), Format.FOXML_1_1, "this is a test");
    }

    @Test
    @ExpectedException(ServiceException.class)
    public void test404() throws Exception {
	// break the Fedora URL and re-init
	ManagementClient c = this.getManagementClient();
	c.setFedoraContextUrl("http://localhost/fedora/foo/");
	c.init();

	File testFile = new File("src/test/resources/ContainerModel.xml");
	InputStream xmlis = new FileInputStream(testFile);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i = xmlis.read(); i != -1; i = xmlis.read()) {
	    baos.write(i);
	}
	c.ingestRaw(baos.toByteArray(), Format.FOXML_1_1, "this is a test");
    }

    @Test
    @ExpectedException(ServiceException.class)
    public void testIngestAuth() throws Exception {
	ManagementClient c = this.getManagementClient();
	c.setPassword("swineflu876bad_password");
	c.init();

	File testFile = new File("src/test/resources/ContainerModel.xml");
	InputStream xmlis = new FileInputStream(testFile);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i = xmlis.read(); i != -1; i = xmlis.read()) {
	    baos.write(i);
	}
	c.ingestRaw(baos.toByteArray(), Format.FOXML_1_1, "this is a test");
    }

    @Test
    @ExpectedException(FedoraException.class)
    public void testIngestBadFormat() throws Exception {
	ManagementClient c = this.getManagementClient();

	File testFile = new File("src/test/resources/ContainerModel.xml");
	InputStream xmlis = new FileInputStream(testFile);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i = xmlis.read(); i != -1; i = xmlis.read()) {
	    baos.write(i);
	}
	c.ingestRaw(baos.toByteArray(), Format.ATOM_1_0, "this is a test");
    }

    @Test
    @ExpectedException(FedoraException.class)
    public void testIngestBadExternalUrl() throws IOException, FedoraException {
	ManagementClient c = this.getManagementClient();

	File testFile = new File("src/test/resources/BadExternalUrl.xml");
	InputStream xmlis = new FileInputStream(testFile);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for(int i = xmlis.read(); i != -1; i = xmlis.read()) {
	    baos.write(i);
	}
	c.ingestRaw(baos.toByteArray(), Format.FOXML_1_1, "this is a test");
    }

}
