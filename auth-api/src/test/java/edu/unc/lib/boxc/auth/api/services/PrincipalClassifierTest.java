package edu.unc.lib.boxc.auth.api.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.services.PrincipalClassifier.classifyPrincipals;
import static edu.unc.lib.boxc.auth.api.services.PrincipalClassifier.getPatronPrincipals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
