package edu.unc.lib.boxc.web.common.auth;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.IP_PRINC_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.web.common.auth.RemoteUserUtil.REMOTE_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author bbpennel
 */
public class PatronPrincipalProviderTest {

    @TempDir
    public Path tmpFolder;
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

    @BeforeEach
    public void setup() throws Exception {
        initMocks(this);
        provider = new PatronPrincipalProvider();

        configFile = tmpFolder.resolve("patronConfig.json").toFile();

        provider.setPatronGroupConfigPath(configFile.getAbsolutePath());
        patronConfig = new ArrayList<>();
    }

    private void addPatronConfig(String id, String name, String ipInclude) {
        Map<String, String> info = new HashMap<>();
        if (id != null) {
            info.put("principal", id);
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

    @Test
    public void missingPrincipalTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(null, TEST_GROUP_NAME, TEST_IP2);
            serializeConfigAndInit();
        });
    }

    @Test
    public void invalidPrincipalTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig("some:group:here", TEST_GROUP_NAME, TEST_IP2);
            serializeConfigAndInit();
        });
    }

    @Test
    public void missingPrincipalNameTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, null, TEST_IP2);
            serializeConfigAndInit();
        });
    }

    @Test
    public void missingPrincipalIpTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, null);
            serializeConfigAndInit();
        });
    }

    @Test
    public void missingPrincipalInvalidIpTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, "what.is.this.thing");
            try {
                serializeConfigAndInit();
            } catch (JsonMappingException e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    public void missingPrincipalUnclosedRangeTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2 + "-");
            try {
                serializeConfigAndInit();
            } catch (JsonMappingException e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    public void missingPrincipalBlankRangeStartTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, "-" + TEST_IP2);
            try {
                serializeConfigAndInit();
            } catch (JsonMappingException e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    public void missingPrincipalInvalidRangeEndTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2 + "-what.is.this.thing");
            try {
                serializeConfigAndInit();
            } catch (JsonMappingException e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    public void missingPrincipalInvalidNumberRangePartsTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP2 + "-" + TEST_IP2 + "-" + TEST_IP2);
            try {
                serializeConfigAndInit();
            } catch (JsonMappingException e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    public void missingPrincipalEndBeforeStartTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, TEST_IP3 + "-" + TEST_IP2);
            try {
                serializeConfigAndInit();
            } catch (JsonMappingException e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    public void missingPrincipalIpTooBigTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addPatronConfig(TEST_GROUP_ID, TEST_GROUP_NAME, "500.0.0.1");
            try {
                serializeConfigAndInit();
            } catch (JsonMappingException e) {
                throw (Exception) e.getCause();
            }
        });
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
        assertEquals(expected.length, princs.size(), msg);
        assertTrue(princs.containsAll(Arrays.asList(expected)), msg);
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
