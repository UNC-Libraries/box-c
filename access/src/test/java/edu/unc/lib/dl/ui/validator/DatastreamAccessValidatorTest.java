package edu.unc.lib.dl.ui.validator;

import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.ui.util.AccessControlSettings;

import static org.mockito.Mockito.*;

public class DatastreamAccessValidatorTest extends Assert {

	@Test
	public void filterBriefObjectRemoveDatastreams() throws Exception {
		Properties prop = new Properties();
		FileInputStream in = new FileInputStream("src/main/resources/accessControl.properties");
		prop.load(in);
		in.close();
		
		AccessControlSettings settings = new AccessControlSettings();
		settings.setProperties(prop);
		settings.setAdminGroup("admin");
		
		DatastreamAccessValidator dav = new DatastreamAccessValidator();
		dav.setAccessSettings(settings);
		
		AccessGroupSet surrogateAccess = mock(AccessGroupSet.class);
		when(surrogateAccess.containsAny(any(AccessGroupSet.class))).thenReturn(true);
		
		AccessGroupSet fileAccess = mock(AccessGroupSet.class);
		when(fileAccess.containsAny(any(AccessGroupSet.class))).thenReturn(false);
		
		
		BriefObjectMetadataBean metadata = new BriefObjectMetadataBean();
		metadata.setDatastream(new String[]{"DATA_FILE", "THUMB_SMALL"});
		
		DatastreamAccessValidator.filterDatastreams(metadata.getDatastream(), surrogateAccess, fileAccess, surrogateAccess);
		
		assertFalse(metadata.getDatastream().contains("DATA_FILE"));
		assertTrue(metadata.getDatastream().contains("THUMB_SMALL"));
		assertEquals(1, metadata.getDatastream().size());
	}
}
