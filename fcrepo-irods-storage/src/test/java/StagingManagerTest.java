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
import static org.junit.Assert.fail;

import org.fcrepo.server.errors.authorization.AuthzDeniedException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fedorax.server.module.storage.StagingManager;

public class StagingManagerTest {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory
			.getLogger(StagingManagerTest.class);

	private StagingManager stagingManager = new StagingManager();

	public StagingManager getStagingManager() {
		return stagingManager;
	}

	public void setStagingManager(StagingManager stagingManager) {
		this.stagingManager = stagingManager;
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testResolveStageLocation() {
		this.getStagingManager().addURLPattern(
				"^http://digitalarchive.lib.unc.edu/sfc/(.*)",
				"file:/mnt/da/sfc/$1");
		String expected = "file:/mnt/da/sfc/home/projectA/file1.txt";
		try {
			String resolved = this
					.getStagingManager()
					.resolveStageLocation(
							"http://digitalarchive.lib.unc.edu/sfc/home/projectA/file1.txt");
			if (!expected.equals(resolved)) {
				fail("Expected string not resolved: " + expected + "\t"
						+ resolved);
			}
		} catch (AuthzDeniedException e) {
			fail("Unsafe file fail");
		}
	}

	@Test
	public void testResolveFederatedIrodsLocation() {
		this.getStagingManager()
				.addURLPattern(
						"^irods://(\\w*@)?cdr-stage.libint.unc.edu:3333/cdrStageZone/(.*)",
						"irods://cdr-vault.libint.unc.edu:3333/cdrStageZone/$2");
		String expected = "irods://cdr-vault.libint.unc.edu:3333/cdrStageZone/home/projectA/file1.txt";
		try {
			String resolved = this
					.getStagingManager()
					.resolveStageLocation(
							"irods://count0@cdr-stage.libint.unc.edu:3333/cdrStageZone/home/projectA/file1.txt");
			if (!expected.equals(resolved)) {
				fail("Expected string not resolved: " + expected + "\t"
						+ resolved);
			}
		} catch (AuthzDeniedException e) {
			fail("Unsafe file fail");
		}
	}

	@Test(expected = AuthzDeniedException.class)
	public void testErrorUnsafeFileLocation() throws AuthzDeniedException {
		String home = System.getProperty("user.home");
		String sneakyStagedUrl = "http://digitalarchive.lib.unc.edu/sfc/../passwords.txt";
		this.getStagingManager().addURLPattern(
				"^http://digitalarchive.lib.unc.edu/sfc/(.*)",
				"file:" + home + "/$1");
		String resolved = this.getStagingManager().resolveStageLocation(
				sneakyStagedUrl);
	}

	@Test
	public void testResolveSafeFileLocation() {
		this.getStagingManager().addURLPattern("^file:/Z:/born_digital/(.*)",
				"file:/mnt/born_digital/$1");
		String expected = "file:/mnt/born_digital/home/projectA/file1.txt";
		try {
			String resolved = this.getStagingManager().resolveStageLocation(
					"file:/Z:/born_digital/home/projectA/file1.txt");
			if (!expected.equals(resolved)) {
				fail("Expected string not resolved: " + expected + "\t"
						+ resolved);
			}
		} catch (AuthzDeniedException e) {
			fail("Unsafe file fail");
		}
	}

	@Test
	public void testRemoveURLPattern() {
		this.getStagingManager().clearURLPatterns();
		this.getStagingManager().addURLPattern("^file:/Z:/born_digital/(.*)",
				"file:/mnt/born_digital/$1");
		this.getStagingManager()
				.removeURLPattern("^file:/Z:/born_digital/(.*)");
		if (this.getStagingManager().getURLPatternMap().size() > 0) {
			fail("Did not remove url staging pattern as requested.");
		}
	}

}
