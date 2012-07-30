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
