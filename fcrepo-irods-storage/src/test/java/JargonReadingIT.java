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
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileInputStream;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Copyright 2010 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * @author Gregory Jansen
 *
 */
public class JargonReadingIT {

    @Test
    public void jargonReadingTest() {
	String testFile = "/count0Zone/home/fedora/BigFOXML.xml";
	IRODSSession irodsSession = null;
	try {
	    IRODSAccount account = new IRODSAccount("ono-sendai", 1247, "fedora", "inst1repo", "/count0Zone/home/fedora",
			    "count0Zone", "count0Resc");
	    IRODSAccessObjectFactory accessObjectFactory;
	    accessObjectFactory = IRODSFileSystem.instance().getIRODSAccessObjectFactory();
	    IRODSFileFactory irodsFileFactory = accessObjectFactory.getIRODSFileFactory(account);
	    IRODSFile irodsFile = irodsFileFactory.instanceIRODSFile(testFile);
	    IRODSFileInputStream fis = irodsFileFactory.instanceIRODSFileInputStream(irodsFile);
	    //BufferedInputStream bis = new BufferedInputStream(fis, IrodsIFileSystem.BUFFER_SIZE);
	    irodsSession.closeSession();
	    SAXParserFactory spf = SAXParserFactory.newInstance();
	    spf.setValidating(false);
	    spf.setNamespaceAware(true);
	    SAXParser parser = spf.newSAXParser();
	    parser.parse(fis, new DefaultHandler());
	} catch (JargonException e) {
	    e.printStackTrace();
	    assertNull("exception thrown", e);
	} catch (ParserConfigurationException e) {
	    e.printStackTrace();
	    assertNull("exception thrown", e);
	} catch (SAXException e) {
	    e.printStackTrace();
	    assertNull("exception thrown", e);
	} catch (IOException e) {
	    e.printStackTrace();
	    assertNull("exception thrown", e);
	} finally {
	    if (irodsSession != null) {
		try {
		    irodsSession.closeSession();
		} catch (JargonException ignored) {}
	    }
	}
    }
}
