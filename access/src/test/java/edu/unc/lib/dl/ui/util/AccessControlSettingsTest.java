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
package edu.unc.lib.dl.ui.util;

import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.security.access.AccessType;

public class AccessControlSettingsTest extends Assert {

	@Test
	public void getAccessType() throws Exception {
		Properties prop = new Properties();
		FileInputStream in = new FileInputStream("src/main/resources/accessControl.properties");
		prop.load(in);
		in.close();

		AccessControlSettings settings = new AccessControlSettings();
		settings.setProperties(prop);

		assertEquals(AccessType.FILE, settings.getAccessType("DATA_FILE"));
		assertEquals(AccessType.RECORD, settings.getAccessType("MD_DESCRIPTIVE"));
		assertEquals(AccessType.ADMIN, settings.getAccessType("RELS-EXT"));
	}
}
