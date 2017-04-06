/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.acl.fcrepo4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author bbpennel
 *
 */
public class ObjectPermissionEvaluatorTest {

	@Mock
	private ObjectACLFactory aclFactory;
	@Mock
	private PID pid;

	private ObjectPermissionEvaluator evaluator;

	private final static String PRINC_GRP1 = "group1";
	private final static String PRINC_GRP2 = "group2";

	private Set<String> principals;

	private Map<String, List<String>> objPrincRoles;

	@Before
	public void init() {
		initMocks(this);

		evaluator = new ObjectPermissionEvaluator();
		evaluator.setAclFactory(aclFactory);

		principals = new HashSet<>(Arrays.asList(PRINC_GRP1));
		objPrincRoles = new HashMap<>();
		when(aclFactory.getPrincipalRoles(any(PID.class))).thenReturn(objPrincRoles);
	}

	@Test
	public void hasStaffPermissionTest() throws Exception {
		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canManage.getPropertyString()));

		assertTrue(evaluator
				.hasStaffPermission(pid, principals, Permission.markForDeletion));
	}

	@Test
	public void hasStaffPermissionMultiplePrincipalsTest() throws Exception {
		principals.add(PRINC_GRP2);

		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canManage.getPropertyString()));

		assertTrue(evaluator
				.hasStaffPermission(pid, principals, Permission.editDescription));
	}

	@Test
	public void hasStaffPermissionNoRolesTest() throws Exception {

		assertFalse(evaluator
				.hasStaffPermission(pid, principals, Permission.markForDeletion));
	}

	@Test
	public void hasStaffPermissionDeniedTest() throws Exception {
		principals.add(PRINC_GRP2);

		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canManage.getPropertyString()));

		assertFalse(evaluator
				.hasStaffPermission(pid, principals, Permission.destroy));
	}

	@Test
	public void hasStaffPermissionMultipleRolesTest() throws Exception {
		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canView.getPropertyString(),
				UserRole.canManage.getPropertyString()));

		assertTrue(evaluator
				.hasStaffPermission(pid, principals, Permission.editDescription));
	}

	@Test(expected = IllegalArgumentException.class)
	public void hasStaffPermissionNullPermissionsTest() throws Exception {
		evaluator.hasStaffPermission(pid, principals, null);
	}

	@Test
	public void getPatronPrincipalsWithPermissionTest() throws Exception {
		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canViewMetadata.getPropertyString()));

		List<String> permittedPrincipals = evaluator
				.getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

		assertEquals(1, permittedPrincipals.size());
		assertTrue(permittedPrincipals.contains(PRINC_GRP1));
	}

	@Test
	public void getMultiplePatronPrincipalsWithPermissionTest() throws Exception {
		principals.add(PRINC_GRP2);

		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canViewMetadata.getPropertyString()));

		List<String> permittedPrincipals = evaluator
				.getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

		assertEquals(1, permittedPrincipals.size());
		assertTrue(permittedPrincipals.contains(PRINC_GRP1));
	}

	@Test
	public void getPatronPrincipalsWithPermissionMultipleRolesTest() throws Exception {
		principals.add(PRINC_GRP2);

		objPrincRoles.put(PRINC_GRP1, Arrays.asList(
				UserRole.canViewMetadata.getPropertyString(), UserRole.canViewOriginals.getPropertyString()));
		objPrincRoles.put(PRINC_GRP2, Arrays.asList(UserRole.canViewMetadata.getPropertyString()));

		List<String> permittedPrincipals = evaluator
				.getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

		assertEquals(2, permittedPrincipals.size());
		assertTrue(permittedPrincipals.contains(PRINC_GRP1));
		assertTrue(permittedPrincipals.contains(PRINC_GRP2));
	}

	@Test
	public void getNoPatronPrincipalsWithPermissionTest() throws Exception {
		principals = new HashSet<>();

		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canViewMetadata.getPropertyString()));

		List<String> permittedPrincipals = evaluator
				.getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

		assertEquals(0, permittedPrincipals.size());
	}

	@Test
	public void getPatronPrincipalsWithPermissionNoPatronRolesTest() throws Exception {
		objPrincRoles.put(PRINC_GRP1, Arrays.asList(UserRole.canManage.getPropertyString()));
		objPrincRoles.put(PRINC_GRP2, Arrays.asList(UserRole.canManage.getPropertyString()));

		List<String> permittedPrincipals = evaluator
				.getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

		assertEquals(0, permittedPrincipals.size());
	}

	@Test
	public void getPatronPrincipalsWithPermissionNoRolesTest() throws Exception {
		List<String> permittedPrincipals = evaluator
				.getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

		assertEquals(0, permittedPrincipals.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPatronPrincipalsWithPermissionNoPidTest() throws Exception {
		evaluator.getPatronPrincipalsWithPermission(null, principals, Permission.viewMetadata);
	}
}
