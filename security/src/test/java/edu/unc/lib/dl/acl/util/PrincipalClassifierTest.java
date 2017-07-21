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
package edu.unc.lib.dl.acl.util;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.PrincipalClassifier.classifyPrincipals;
import static edu.unc.lib.dl.acl.util.PrincipalClassifier.getPatronPrincipals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class PrincipalClassifierTest {

    private static final String STAFF_PRINC = "staff";

    @Test
    public void classifyPrincipalsTest() {
        Set<String> principals = new HashSet<>(Arrays.asList(PUBLIC_PRINC,
                AUTHENTICATED_PRINC, STAFF_PRINC));

        Set<String> patronPrincipals = new HashSet<>();
        Set<String> staffPrincipals = new HashSet<>();
        classifyPrincipals(principals, patronPrincipals, staffPrincipals);

        assertEquals(2, patronPrincipals.size());
        assertTrue(patronPrincipals.contains(PUBLIC_PRINC));
        assertTrue(patronPrincipals.contains(AUTHENTICATED_PRINC));

        assertEquals(1, staffPrincipals.size());
        assertTrue(staffPrincipals.contains(STAFF_PRINC));
    }

    @Test
    public void classifyPrincipalsNoPatronsTest() {
        Set<String> principals = new HashSet<>(Arrays.asList(STAFF_PRINC));

        Set<String> patronPrincipals = new HashSet<>();
        Set<String> staffPrincipals = new HashSet<>();
        classifyPrincipals(principals, patronPrincipals, staffPrincipals);

        assertEquals(0, patronPrincipals.size());

        assertEquals(1, staffPrincipals.size());
        assertTrue(staffPrincipals.contains(STAFF_PRINC));
    }

    @Test
    public void getPatronPrincipalsTest() {
        Set<String> principals = new HashSet<>(Arrays.asList(PUBLIC_PRINC,
                AUTHENTICATED_PRINC, STAFF_PRINC));

        Set<String> patronPrincipals = getPatronPrincipals(principals);

        assertEquals(2, patronPrincipals.size());
        assertTrue(patronPrincipals.contains(PUBLIC_PRINC));
        assertTrue(patronPrincipals.contains(AUTHENTICATED_PRINC));
    }

    @Test
    public void getPatronPrincipalsNoPatronsTest() {
        Set<String> principals = new HashSet<>(Arrays.asList(STAFF_PRINC));

        Set<String> patronPrincipals = getPatronPrincipals(principals);

        assertEquals(0, patronPrincipals.size());
    }
}
