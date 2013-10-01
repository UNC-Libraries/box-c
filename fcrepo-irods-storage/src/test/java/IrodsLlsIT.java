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
import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.fcrepo.server.errors.LowlevelStorageException;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.helpers.DefaultHandler;

import fedorax.server.module.storage.lowlevel.irods.IrodsIFileSystem;

// TODO test all public methods inc. rebuild interface

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-context-IT.xml" })
public class IrodsLlsIT extends Assert {

    @Autowired
    private IRODSAccount account = null;
    private int bufferSize = 32768;

    public int getBufferSize() {
	return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
	this.bufferSize = bufferSize;
    }

    public IRODSAccount getAccount() {
	return account;
    }

    public void setAccount(IRODSAccount account) {
	this.account = account;
    }

    private IrodsIFileSystem getModule() {
	IrodsIFileSystem result = null;
	try {
	    IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();
	    result = new IrodsIFileSystem(this.bufferSize, irodsFileSystem, this.account);
	    System.out.println("module loaded");
	    return result;
	} catch (Exception e) {
	    throw new Error("got an exception creating module", e);
	}
    }

    private String getTestPath() {
	return this.account.getHomeDirectory() + "/IrodsLLSTests/";
    }

    @Test
    public void writeParseDeleteXML() {
	IrodsIFileSystem module = this.getModule();
	String filename = "BigFOXML.xml";
	try {
	    InputStream content = this.getClass().getResourceAsStream(filename);

	    // make sure it parses locally first
	    SAXParserFactory spf = SAXParserFactory.newInstance();
	    SAXParser localparser = spf.newSAXParser();
	    localparser.parse(content, new DefaultHandler());
	    content.close();

	    // write to irods
	    content = this.getClass().getResourceAsStream(filename);
	    File testFile = new File(this.getTestPath() + filename);
	    module.write(testFile, content);
	    System.out.println("copied big XML into test location");

	    // read/parse test file
	    InputStream read = module.read(testFile);
	    SAXParser irodsparser = spf.newSAXParser();
	    irodsparser.parse(read, new DefaultHandler());
	    System.err.println("parsed");

	    // delete test file
	    module.delete(testFile);

	    IRODSSession irodsSession = null;
	    try {
		IRODSFileFactory ff = IRODSFileSystem.instance().getIRODSFileFactory(account);
		IRODSFile ifile = ff.instanceIRODSFile(this.getTestPath() + filename);
		assertTrue(!ifile.exists());
	    } catch (JargonException e) {
		throw new LowlevelStorageException(true, "Problem deleting iRODS file", e);
	    } finally {
		if (irodsSession != null) {
		    try {
			irodsSession.closeSession();
		    } catch (JargonException ignored) {
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    fail(e.getMessage());
	}
    }

    @Test
    public void largeFileWriteReadDelete() {
	IrodsIFileSystem module = this.getModule();
	String filename = "65MB.dat";
	try {
	    InputStream content = this.getClass().getResourceAsStream(filename);
	    File testFile = new File(this.getTestPath() + filename);
	    module.write(testFile, content);
	    System.out.println("copied big XML into test location");

	    InputStream read = module.read(testFile);

	    System.out.println("reading supposedly buffered stream byte by byte");
	    while (read.read() != -1) {
		continue;
	    }
	    module.delete(testFile);

	    IRODSSession irodsSession = null;
	    try {
		IRODSFileFactory ff = IRODSFileSystem.instance().getIRODSFileFactory(account);
		IRODSFile ifile = ff.instanceIRODSFile(this.getTestPath() + filename);
		assertTrue(!ifile.exists());
	    } catch (JargonException e) {
		throw new LowlevelStorageException(true, "Problem deleting iRODS file", e);
	    } finally {
		if (irodsSession != null) {
		    try {
			irodsSession.closeSession();
		    } catch (JargonException ignored) {
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    fail(e.getMessage());
	}
    }
}
