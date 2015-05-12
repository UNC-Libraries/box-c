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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.helpers.DefaultHandler;

import fedorax.server.module.storage.lowlevel.irods.IrodsIFileSystem;

public class MockFedoraIT extends Assert {
	private static Log LOG = LogFactory.getLog(MockFedoraIT.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@SuppressWarnings("unused")
	private File getJunkFile(int kbytes) {
		File junk = null;
		try {
			junk = File.createTempFile("test", "junk");
			try (OutputStream out = new FileOutputStream(junk)) {
				byte[] buffer = new byte[4096];
				for (int i = 0; i < buffer.length; i++) {
					buffer[i] = 'C';
				}
				for (int i = 0; i * 4 < kbytes; i++) {
					out.write(buffer);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.debug("junk file created");
		return junk;
	}

	private IrodsIFileSystem getModule() {
		IrodsIFileSystem result = null;
		try {
			IRODSAccount account = new IRODSAccount("ono-sendai", 1247,
					"fedora", "inst1repo", "/count0Zone/home/fedora",
					"count0Zone", "count0Resc");
			IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();
			result = new IrodsIFileSystem(32768, irodsFileSystem, account);
		} catch (Exception e) {
			e.printStackTrace();
			fail("got an exception:" + e.getLocalizedMessage());
		}
		return result;
	}

	// @Test
	// public void rewriteAndReadTest() {
	// boolean buffering = true;
	// int loops = 100;
	// String irodsFilePath = "/cdrZone/home/fedora/test.txt";
	// File testFile = this.getJunkFile(1024);
	// try {
	// IrodsIFileSystem module = this.getModule();
	// FileInputStream fis1 = new FileInputStream(testFile);
	// module.write(new File(irodsFilePath), fis1);
	// System.out.println("test file written to irods");
	// for (int count = 0; count < loops; count++) {
	// FileInputStream fis = new FileInputStream(testFile);
	// module.rewrite(new File(irodsFilePath), fis);
	// System.out.println("rewrite " + count + " done");
	//
	// InputStream is = null;
	// if (buffering) {
	// is = new BufferedInputStream(module.read(new File(irodsFilePath)), 4096);
	// } else {
	// is = module.read(new File(irodsFilePath));
	// }
	// while (is.read() != -1) {
	// }
	// System.out.println("read done");
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	// @Test
	// public void readTest() {
	// String irodsFilePath = "/cdrZone/home/fedora/test.txt";
	// File testFile = this.getJunkFile(10240);
	// int count = 0;
	// try {
	// IrodsIFileSystem module = this.getModule();
	// FileInputStream fis1 = new FileInputStream(testFile);
	// module.write(new File(irodsFilePath), fis1);
	// System.out.println("test file written to irods");
	// InputStream is = module.read(new File(irodsFilePath));
	// while (is.read() != -1) {
	// count++;
	// }
	// System.out.println(is.available());
	// System.out.println("done with reads: " + count);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	@Test
	public void multithreadedTest() {
		IrodsIFileSystem module = this.getModule();
		String testPath = "/count0Zone/home/fedora/BigFOXML.xml";
		try {
			InputStream content = this.getClass().getResourceAsStream(
					"BigFOXML.xml");
			module.write(new File(testPath), content);
			System.err.println("copied big FOXML into test location");

			// Thread t2 = new Thread(new Runnable() {
			//
			// public void run() {
			// try {
			// echoIrodsFile(getModule(),
			// "/count0Zone/home/fedora/BigFOXML.xml", "/tmp/t2.output");
			// parseIrodsFile(getModule(),
			// "/count0Zone/home/fedora/BigFOXML.xml");
			// } catch (LowlevelStorageException e) {
			// System.err.println("second thread threw exception");
			// e.printStackTrace();
			// }
			// }
			//
			// });
			// t2.run();
			try {
				echoIrodsFile(getModule(),
						"/count0Zone/home/fedora/BigFOXML.xml",
						"/tmp/t1.output");
				parseIrodsFile(getModule(),
						"/count0Zone/home/fedora/BigFOXML.xml");
			} catch (LowlevelStorageException e) {
				System.err.println("main thread threw exception");
				e.printStackTrace();
				assertNull("Reading tests threw an exception, see stack trace",
						e);
			}

			System.err.println("parsed");
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseIrodsFile(IrodsIFileSystem module, String testPath)
			throws LowlevelStorageException {
		InputStream is = module.read(new File(testPath));
		// initialize sax for this parse
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			// spf.setValidating(false);
			// spf.setNamespaceAware(true);
			SAXParser parser = spf.newSAXParser();
			parser.parse(is, new DefaultHandler());
		} catch (Exception e) {
			throw new RuntimeException("Error with SAX parser", e);
		}
	}

	private void echoIrodsFile(IrodsIFileSystem module, String testPath,
			String outputPath) throws LowlevelStorageException {
		InputStream is = module.read(new File(testPath));
		// initialize sax for this parse
		try (FileOutputStream fos = new FileOutputStream(new File(outputPath))) {
			// byte[] buffer = new byte[10];
			for (int num = is.read(); num != -1; num = is.read()) {
				fos.write(num);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error", e);
		}

	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

}
