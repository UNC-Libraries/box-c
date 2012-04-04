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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;

import edu.unc.lib.dl.util.TripleStoreQueryService;

public class AccessControlUtilsTest extends Assert {

	private TripleStoreQueryService tripleStoreQueryService;
	private AccessControlUtils acu;
	private PID rootPID;
	private AttributeFactory attributeFactory;

	@Before
	public void init() {
		rootPID = new PID("uuid:root");

		tripleStoreQueryService = mock(TripleStoreQueryService.class);
		when(tripleStoreQueryService.fetchByRepositoryPath("/Collections")).thenReturn(rootPID);

		acu = new AccessControlUtils();
		acu.setTripleStoreQueryService(tripleStoreQueryService);

		acu.setOnlyCacheReadPermissions(false);
		acu.setCacheDepth(2);
		acu.setCacheLimit(5);
		acu.setCacheResetTime(100000);
		attributeFactory = StandardAttributeFactory.getFactory();
		acu.setAttributeFactory(attributeFactory);

		acu.init();
	}

	@Test
	public void hasAccessTest() {
		PID targetPID = new PID("uuid:c");
		PID parentPID1 = rootPID;
		PID parentPID2 = new PID("uuid:b");

		Map<String, List<String>> targetTriples = new HashMap<String, List<String>>();
		targetTriples
				.put("http://cdr.unc.edu/definitions/roles#admin", new ArrayList<String>(Arrays.asList("testgroup")));

		Map<String, List<String>> parentTriples1 = new HashMap<String, List<String>>();
		parentTriples1.put("http://cdr.unc.edu/definitions/roles#patron", new ArrayList<String>(Arrays.asList("public")));

		Map<String, List<String>> parentTriples2 = new HashMap<String, List<String>>();
		parentTriples2.put("http://cdr.unc.edu/definitions/roles#curator",
				new ArrayList<String>(Arrays.asList("testgroup", "othergroup")));

		when(tripleStoreQueryService.lookupRepositoryAncestorPids(targetPID)).thenReturn(
				new ArrayList<PID>(Arrays.asList(new PID[] { parentPID1, parentPID2 })));
		when(tripleStoreQueryService.lookupRepositoryAncestorPids(parentPID2)).thenReturn(
				new ArrayList<PID>(Arrays.asList(new PID[] { parentPID1 })));
		when(tripleStoreQueryService.lookupRepositoryAncestorPids(parentPID1)).thenReturn(
				new ArrayList<PID>(Arrays.asList(new PID[] {})));
		when(tripleStoreQueryService.fetchAllTriples(parentPID1)).thenReturn(parentTriples1);
		when(tripleStoreQueryService.fetchAllTriples(parentPID2)).thenReturn(parentTriples2);
		when(tripleStoreQueryService.fetchAllTriples(targetPID)).thenReturn(targetTriples);

		List<String> userGroups = new ArrayList<String>(Arrays.asList("public", "testgroup"));

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

	@Test
	public void getPermissionGroupSetsWithResourceType() throws URISyntaxException {
		PID test = new PID("uuid:abfa9b72-ec02-47b1-9066-68ca3a1f64fb/DATA_FILE");
		System.out.println(test.getPid() + " | " + test.getURI());

		PID targetPID = new PID("uuid:c");
		Map<String, List<String>> targetTriples = new HashMap<String, List<String>>();
		targetTriples.put("http://cdr.unc.edu/definitions/roles#patron",
				new ArrayList<String>(Arrays.asList("testgroup")));

		when(tripleStoreQueryService.lookupRepositoryAncestorPids(targetPID)).thenReturn(
				new ArrayList<PID>(Arrays.asList(new PID[] {})));
		when(tripleStoreQueryService.fetchAllTriples(targetPID)).thenReturn(targetTriples);

		EvaluationResult result = acu.processCdrAccessControl(targetPID.getPid() + "/DATA_FILE", null, new URI(
				"http://www.w3.org/2001/XMLSchema#string"));
		assertEquals(1, ((com.sun.xacml.attr.BagAttribute) result.getAttributeValue()).size());
		Iterator<?> it = ((com.sun.xacml.attr.BagAttribute) result.getAttributeValue()).iterator();
		while (it.hasNext()) {
			com.sun.xacml.attr.StringAttribute value = (com.sun.xacml.attr.StringAttribute) it.next();
			assertTrue(value.getValue().equals("testgroup"));
		}

		// ORIGINAL
		Map<String, Set<String>> permissionGroupSets = acu.getPermissionGroupSets(targetPID, 1);
		assertEquals(permissionGroupSets.size(), 1);
		assertTrue(permissionGroupSets.containsKey("permitOriginalsRead"));
		assertEquals(permissionGroupSets.get("permitOriginalsRead").size(), 1);
		assertTrue(permissionGroupSets.get("permitOriginalsRead").contains("testgroup"));

		// METADATA
		permissionGroupSets = acu.getPermissionGroupSets(targetPID, 0);
		assertEquals(permissionGroupSets.size(), 1);
		assertTrue(permissionGroupSets.containsKey("permitMetadataRead"));
		assertEquals(permissionGroupSets.get("permitMetadataRead").size(), 1);
		assertTrue(permissionGroupSets.get("permitMetadataRead").contains("testgroup"));
	}
}
