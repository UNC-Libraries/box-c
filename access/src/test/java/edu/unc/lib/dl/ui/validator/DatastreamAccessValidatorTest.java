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
package edu.unc.lib.dl.ui.validator;

import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
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
		
		//DatastreamAccessValidator.filterDatastreams(metadata.getDatastream(), surrogateAccess, fileAccess, surrogateAccess);
		
		assertFalse(metadata.getDatastream().contains("DATA_FILE"));
		assertTrue(metadata.getDatastream().contains("THUMB_SMALL"));
		assertEquals(1, metadata.getDatastream().size());
	}
}
