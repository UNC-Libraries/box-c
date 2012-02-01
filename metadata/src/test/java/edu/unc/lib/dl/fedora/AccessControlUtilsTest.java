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

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.util.TripleStoreQueryService;

public class AccessControlUtilsTest extends Assert {

	@Test
	public void hasAccessTest() {
		PID targetPID = new PID("uuid:c");
		PID parentPID1 = new PID("uuid:a");
		PID parentPID2 = new PID("uuid:b");
		
		Map<String, List<String>> targetTriples = new HashMap<String, List<String>>();
		targetTriples.put("http://cdr.unc.edu/definitions/roles#admin",
				new ArrayList<String>(Arrays.asList(new String[] { "testgroup" })));
		
		Map<String, List<String>> parentTriples1 = new HashMap<String, List<String>>();
		parentTriples1.put("http://cdr.unc.edu/definitions/roles#patron",
				new ArrayList<String>(Arrays.asList(new String[] { "public" })));

		Map<String, List<String>> parentTriples2 = new HashMap<String, List<String>>();
		parentTriples2.put("http://cdr.unc.edu/definitions/roles#curator",
				new ArrayList<String>(Arrays.asList(new String[] { "testgroup", "othergroup" })));

		TripleStoreQueryService tripleStoreQueryService = mock(TripleStoreQueryService.class);
		when(tripleStoreQueryService.lookupRepositoryAncestorPids(targetPID)).thenReturn(Arrays.asList(new PID[]{parentPID1, parentPID2, targetPID}));
		when(tripleStoreQueryService.lookupRepositoryAncestorPids(parentPID2)).thenReturn(Arrays.asList(new PID[]{parentPID1, parentPID2}));
		when(tripleStoreQueryService.lookupRepositoryAncestorPids(parentPID1)).thenReturn(Arrays.asList(new PID[]{parentPID1}));
		when(tripleStoreQueryService.fetchAllTriples(parentPID1)).thenReturn(parentTriples1);
		when(tripleStoreQueryService.fetchAllTriples(parentPID2)).thenReturn(parentTriples2);
		when(tripleStoreQueryService.fetchAllTriples(targetPID)).thenReturn(targetTriples);
		when(tripleStoreQueryService.fetchByRepositoryPath("/Collections")).thenReturn(parentPID1);

		List<String> userGroups = new ArrayList<String>(Arrays.asList(new String[] { "public", "testgroup" }));

		AccessControlUtils acu = new AccessControlUtils();
		acu.setTripleStoreQueryService(tripleStoreQueryService);

		Properties accessControlProperties = new Properties();
		accessControlProperties.setProperty("http://cdr.unc.edu/definitions/roles#metadataOnlyPatron",
				"permitMetadataRead");
		accessControlProperties.setProperty("http://cdr.unc.edu/definitions/roles#patron",
				"permitMetadataRead permitOriginalsRead permitDerivativesRead");
		accessControlProperties.setProperty("http://cdr.unc.edu/definitions/roles#curator",
				"permitMetadataCreate permitMetadataRead permitMetadataUpdate permitOriginalsCreate permitOriginalsRead " +
				"permitOriginalsUpdate permitDerivativesCreate permitDerivativesRead permitDerivativesUpdate");
		accessControlProperties.setProperty("http://cdr.unc.edu/definitions/roles#admin",
				"permitMetadataCreate permitMetadataRead permitMetadataUpdate permitMetadataDelete permitOriginalsCreate " +
				"permitOriginalsRead permitOriginalsUpdate permitOriginalsDelete permitDerivativesCreate " +
				"permitDerivativesRead permitDerivativesUpdate permitDerivativesDelete");
		acu.setAccessControlProperties(accessControlProperties);
		
		acu.setOnlyCacheReadPermissions(false);
		acu.setCacheDepth(2);
		acu.setCacheLimit(5);
		acu.setCacheResetTime(100000);
		
		acu.init();

		assertTrue(acu.hasAccess(targetPID, userGroups, "http://cdr.unc.edu/definitions/roles#admin"));
		assertTrue(acu.hasAccess(targetPID, userGroups, "http://cdr.unc.edu/definitions/roles#curator"));
		assertTrue(acu.hasAccess(targetPID, userGroups, "http://cdr.unc.edu/definitions/roles#patron"));
		assertTrue(acu.hasAccess(targetPID, userGroups, "http://cdr.unc.edu/definitions/roles#metadataOnlyPatron"));
		assertTrue(acu.hasAccess(parentPID1, userGroups, "http://cdr.unc.edu/definitions/roles#patron"));
		assertFalse(acu.hasAccess(parentPID1, userGroups, "http://cdr.unc.edu/definitions/roles#curator"));
		assertTrue(acu.hasAccess(parentPID2, userGroups, "http://cdr.unc.edu/definitions/roles#curator"));
		assertTrue(acu.hasAccess(parentPID2, userGroups, "http://cdr.unc.edu/definitions/roles#patron"));
		assertTrue(acu.hasAccess(parentPID2, userGroups, "http://cdr.unc.edu/definitions/roles#metadataOnlyPatron"));
		assertFalse(acu.hasAccess(parentPID2, userGroups, "http://cdr.unc.edu/definitions/roles#admin"));
		
	}
}
