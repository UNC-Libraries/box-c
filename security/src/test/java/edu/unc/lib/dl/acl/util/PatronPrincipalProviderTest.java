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
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.IP_PRINC_NAMESPACE;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.RemoteUserUtil.REMOTE_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author bbpennel
 */
public class PatronPrincipalProviderTest {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private File configFile;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private List<Map<String, String>> patronConfig;

    private PatronPrincipalProvider provider;

    private static final String TEST_IP = "192.168.150.16";
    private static final String TEST_IP2 = "192.168.155.27";
    private static final String TEST_IP3 = "192.168.160.101";

    private static final String USERNAME = "username";
    private static final String TEST_GROUP_ID = IP_PRINC_NAMESPACE + "test_grp";
    private static final String TEST_GROUP_NAME = "Test Group";
    private static final String TEST_GROUP_ID2 = IP_PRINC_NAMESPACE + "other_grp";
    private static final String TEST_GROUP_NAME2 = "Other Group";

    @Before
    public void setup() throws Exception {
        initMocks(this);
        provider = new PatronPrincipalProvider();

        tmpFolder.create();
        configFile = tmpFolder.newFile("patronConfig.json");

        provider.setPatronGroupConfigPath(configFile.getAbsolutePath());
        patronConfig = new ArrayList<>();
    }

    private void addPatronConfig(String id, String name, String ipInclude) {
        Map<String, String> info = new HashMap<>();
        if (id != null) {
            info.put("id", id);
        }
        if (name != null) {
            info.put("name", name);
        }
        if (ipInclude != null) {
            info.put("ipInclude", ipInclude);
        }
        patronConfig.add(info);
    }

    private void serializeConfigAndInit() throws Exception {
        objectMapper.writeValue(configFile, patronConfig);
        provider.init();
    }

    @Test
    public void emptyConfigTest() throws Exception {
        serializeConfigAndInit();

        assertTrue(provider.getConfiguredPatronPrincipals().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalIdTest() throws Exception {
        addPatronConfig(null, TEST_GROUP_NAME, TEST_IP2);
        serializeConfigAndInit();
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidPrincipalIdTest() throws Exception {
        addPatronConfig("some:group:here", TEST_GROUP_NAME, TEST_IP2);
        serializeConfigAndInit();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalNameTest() throws Exception {
        addPatronConfig(TEST_GROUP_ID, null, TEST_IP2);
        serializeConfigAndInit();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalIpTest() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, null);
        serializeConfigAndInit();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalInvalidIpTest() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, "what.is.this.thing");
        try {
            serializeConfigAndInit();
        } catch (JsonMappingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalUnclosedRangeTest() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2 + "-");
        try {
            serializeConfigAndInit();
        } catch (JsonMappingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalBlankRangeStartTest() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, "-" + TEST_IP2);
        try {
            serializeConfigAndInit();
        } catch (JsonMappingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalInvalidRangeEndTest() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2 + "-what.is.this.thing");
        try {
            serializeConfigAndInit();
        } catch (JsonMappingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrincipalInvalidNumberRangePartsTest() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2 + "-" + TEST_IP2 + "-" + TEST_IP2);
        try {
            serializeConfigAndInit();
        } catch (JsonMappingException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void getPrincipalsPublic() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP));
        assertContainsPrincipals(princs, PUBLIC_PRINC);
    }

    @Test
    public void getPrincipalsAuthenticated() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(true, TEST_IP));
        assertContainsPrincipals(princs, PUBLIC_PRINC, AUTHENTICATED_PRINC);
    }

    @Test
    public void getPrincipalsWithNoConfiguredGroups() throws Exception {
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP));
        assertContainsPrincipals(princs, PUBLIC_PRINC);
    }

    @Test
    public void getPrincipalsIpMatch() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP));
        assertContainsPrincipals(princs, PUBLIC_PRINC, TEST_GROUP_ID);
    }

    @Test
    public void getPrincipalsIpStartOfRange() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP + "-" + TEST_IP2);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP));
        assertContainsPrincipals(princs, PUBLIC_PRINC, TEST_GROUP_ID);
    }

    @Test
    public void getPrincipalsIpEndOfRange() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP + "-" + TEST_IP2);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP2));
        assertContainsPrincipals(princs, PUBLIC_PRINC, TEST_GROUP_ID);
    }

    @Test
    public void getPrincipalsIpMiddleOfRange() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP + "-" + TEST_IP3);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP2));
        assertContainsPrincipals(princs, PUBLIC_PRINC, TEST_GROUP_ID);
    }

    @Test
    public void getPrincipalsIpOutOfRange() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP + "-" + TEST_IP2);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP3));
        assertContainsPrincipals(princs, PUBLIC_PRINC);
    }

    @Test
    public void getPrincipalsIpInList() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP + "," + TEST_IP3 + "," + TEST_IP2);
        serializeConfigAndInit();

        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP)),
                PUBLIC_PRINC, TEST_GROUP_ID);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP2)),
                PUBLIC_PRINC, TEST_GROUP_ID);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP3)),
                PUBLIC_PRINC, TEST_GROUP_ID);
    }

    @Test
    public void getPrincipalsIpNotInList() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP + "," + TEST_IP3);
        serializeConfigAndInit();

        List<String> princs = provider.getPrincipals(mockRequest(false, TEST_IP2));
        assertContainsPrincipals(princs, PUBLIC_PRINC);
    }

    @Test
    public void getPrincipalsIpMixedFormat() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP + "-" + TEST_IP2 + "," + TEST_IP3);
        serializeConfigAndInit();

        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP)),
                PUBLIC_PRINC, TEST_GROUP_ID);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP2)),
                PUBLIC_PRINC, TEST_GROUP_ID);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP3)),
                PUBLIC_PRINC, TEST_GROUP_ID);
        // Just outside of range
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, "192.168.155.30")),
                PUBLIC_PRINC);
        // Before range
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, "192.168.150.14")),
                PUBLIC_PRINC);
        // After range
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, "192.168.200.1")),
                PUBLIC_PRINC);
    }

    @Test
    public void getPrincipalsMultipleGroups() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP);
        addPatronConfig(TEST_GROUP_ID2, TEST_GROUP_NAME2, TEST_IP2);
        serializeConfigAndInit();

        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP)),
                PUBLIC_PRINC, TEST_GROUP_ID);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP2)),
                PUBLIC_PRINC, TEST_GROUP_ID2);
    }

    @Test
    public void getPrincipalsOverlappingGroups() throws Exception {
        addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2);
        addPatronConfig(TEST_GROUP_ID2, TEST_GROUP_NAME2, TEST_IP + "-" + TEST_IP3);
        serializeConfigAndInit();

        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP)),
                PUBLIC_PRINC, TEST_GROUP_ID2);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP2)),
                PUBLIC_PRINC, TEST_GROUP_ID, TEST_GROUP_ID2);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, TEST_IP3)),
                PUBLIC_PRINC, TEST_GROUP_ID2);
        assertContainsPrincipals(provider.getPrincipals(mockRequest(false, "192.168.200.1")),
                PUBLIC_PRINC);
    }

    private void assertContainsPrincipals(List<String> princs, String... expected) {
        String msg = "Expected [" + String.join(",", expected) + "] received " + princs;
        assertEquals(msg, expected.length, princs.size());
        assertTrue(msg, princs.containsAll(Arrays.asList(expected)));
    }

    private HttpServletRequest mockRequest(boolean isAuthenticated, String ipAddr) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        if (isAuthenticated) {
            when(request.getHeader(REMOTE_USER)).thenReturn(USERNAME);
        }
        if (ipAddr != null) {
            when(request.getHeader(PatronPrincipalProvider.FORWARDED_FOR_HEADER)).thenReturn(ipAddr);
        }
        return request;
    }
}
