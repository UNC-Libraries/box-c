package edu.unc.lib.boxc.web.common.auth.filters;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.IP_PRINC_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.web.common.auth.HttpAuthHeaders.FORWARDED_GROUPS_HEADER;
import static edu.unc.lib.boxc.web.common.auth.HttpAuthHeaders.FORWARDED_MAIL_HEADER;
import static edu.unc.lib.boxc.web.common.auth.HttpAuthHeaders.SHIBBOLETH_GROUPS_HEADER;
import static edu.unc.lib.boxc.web.common.auth.filters.StoreUserAccessControlFilter.FORWARDING_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.common.auth.PatronPrincipalProvider;
import edu.unc.lib.boxc.web.common.auth.RemoteUserUtil;

/**
 *
 * @author bbpennel
 *
 */
public class StoreUserAccessControlFilterTest {

    private StoreUserAccessControlFilter filter;
    private AutoCloseable closeable;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private static final String TEST_IP = "192.168.150.16";
    private static final String TEST_IP_PRINC = IP_PRINC_NAMESPACE + "test_grp";
    private static final String CONFIG = "[{ \"principal\" : \"" + TEST_IP_PRINC
            + "\", \"name\" : \"Test Group\", \"ipInclude\" : \"" + TEST_IP + "\"}]";

    @TempDir
    public Path tmpFolder;
    private File configFile;
    private PatronPrincipalProvider patronProvider;


    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        configFile = tmpFolder.resolve("patronConfig.json").toFile();
        FileUtils.writeStringToFile(configFile, CONFIG, StandardCharsets.US_ASCII);

        patronProvider = new PatronPrincipalProvider();
        patronProvider.setPatronGroupConfigPath(configFile.getAbsolutePath());
        patronProvider.init();

        filter = new StoreUserAccessControlFilter();
        filter.setRetainGroupsThreadStore(true);
        filter.setPatronPrincipalProvider(patronProvider);

        when(request.getServletPath()).thenReturn("/path/to/resource");
    }

    @AfterEach
    public void cleanup() throws Exception {
        closeable.close();
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testDefaultGroupsAssigned() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertEquals("", GroupsThreadStore.getUsername());
        assertNull(GroupsThreadStore.getEmail());
        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        assertTrue(accessGroups.contains(PUBLIC_PRINC), "Public must be assigned");
        verify(request).setAttribute("accessGroupSet", accessGroups);
    }

    @Test
    public void testAuthenticatedUserFromHeader() throws Exception {
        when(request.getHeader(RemoteUserUtil.REMOTE_USER)).thenReturn("user");
        when(request.getRemoteUser()).thenReturn(null);
        when(request.getHeader("mail")).thenReturn("user@example.com");

        filter.doFilter(request, response, filterChain);

        assertEquals("user", GroupsThreadStore.getUsername());
        assertEquals("user@example.com", GroupsThreadStore.getEmail());

        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        verify(request).setAttribute("accessGroupSet", accessGroups);

        assertTrue(accessGroups.contains(PUBLIC_PRINC), "Public must be assigned");
        assertTrue(accessGroups.contains(AUTHENTICATED_PRINC), "Authenticated must be assigned");
        assertTrue(accessGroups.contains("unc:onyen:user"), "User principal must be assigned");
    }

    @Test
    public void testAuthenticatedUser() throws Exception {
        when(request.getRemoteUser()).thenReturn("user");
        when(request.getHeader("mail")).thenReturn("user@example.com");

        filter.doFilter(request, response, filterChain);

        assertEquals("user", GroupsThreadStore.getUsername());
        assertEquals("user@example.com", GroupsThreadStore.getEmail());

        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        verify(request).setAttribute("accessGroupSet", accessGroups);

        assertTrue(accessGroups.contains(PUBLIC_PRINC), "Public must be assigned");
        assertTrue(accessGroups.contains(AUTHENTICATED_PRINC), "Authenticated must be assigned");
        assertTrue(accessGroups.contains("unc:onyen:user"), "User principal must be assigned");
    }

    @Test
    public void testIpGroupUser() throws Exception {
        when(request.getHeader(PatronPrincipalProvider.FORWARDED_FOR_HEADER)).thenReturn(TEST_IP);

        filter.doFilter(request, response, filterChain);

        assertEquals("", GroupsThreadStore.getUsername());
        assertNull(GroupsThreadStore.getEmail());

        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        verify(request).setAttribute("accessGroupSet", accessGroups);

        assertEquals(2, accessGroups.size());
        assertTrue(accessGroups.contains(PUBLIC_PRINC), "Public must be assigned");
        assertTrue(accessGroups.contains(TEST_IP_PRINC), "IP Group must be assigned");
    }

    @Test
    public void testUserWithGroups() throws Exception {
        when(request.getRemoteUser()).thenReturn("user");
        when(request.getHeader("mail")).thenReturn("user@example.com");
        when(request.getHeader(SHIBBOLETH_GROUPS_HEADER)).thenReturn("one;two");

        filter.doFilter(request, response, filterChain);

        assertEquals("user", GroupsThreadStore.getUsername());
        assertEquals("user@example.com", GroupsThreadStore.getEmail());

        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        verify(request).setAttribute("accessGroupSet", accessGroups);

        assertTrue(accessGroups.contains(PUBLIC_PRINC), "Public must be assigned");
        assertTrue(accessGroups.contains(AUTHENTICATED_PRINC), "Authenticated must be assigned");
        assertTrue(accessGroups.contains("one"), "Group one must be assigned");
        assertTrue(accessGroups.contains("two"), "Group one must be assigned");
    }

    @Test
    public void testForwardedGroups() throws Exception {
        when(request.getRemoteUser()).thenReturn("forwarder");
        when(request.isUserInRole(FORWARDING_ROLE)).thenReturn(true);
        when(request.getHeader(FORWARDED_GROUPS_HEADER)).thenReturn("f1;f2");

        filter.doFilter(request, response, filterChain);

        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        assertFalse(accessGroups.contains(PUBLIC_PRINC), "Public was not forwarded, must not be assigned");
        assertTrue(accessGroups.contains("f1"), "Forwarded groups must be assigned");
        assertTrue(accessGroups.contains("f2"), "Forwarded groups must be assigned");
    }

    @Test
    public void testForwardedNoGroups() throws Exception {
        when(request.getRemoteUser()).thenReturn("forwarder");
        when(request.isUserInRole(FORWARDING_ROLE)).thenReturn(true);
        when(request.getHeader(FORWARDED_GROUPS_HEADER)).thenReturn("");

        filter.doFilter(request, response, filterChain);

        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        assertEquals(0, accessGroups.size());
    }

    @Test
    public void testForwardedEmail() throws Exception {
        when(request.getRemoteUser()).thenReturn("forwarder");
        when(request.isUserInRole(FORWARDING_ROLE)).thenReturn(true);
        when(request.getHeader(FORWARDED_MAIL_HEADER)).thenReturn("user@example.com");

        filter.doFilter(request, response, filterChain);

        assertEquals("user@example.com", GroupsThreadStore.getEmail());
    }

    @Test
    public void testIgnoreForwardedEmail() throws Exception {
        when(request.getRemoteUser()).thenReturn("forwarder");
        when(request.isUserInRole(FORWARDING_ROLE)).thenReturn(false);
        when(request.getHeader(FORWARDED_MAIL_HEADER)).thenReturn("user@example.com");

        filter.doFilter(request, response, filterChain);

        assertNull(GroupsThreadStore.getEmail());
    }

    @Test
    public void testMailOverrideForwarded() throws Exception {
        when(request.getRemoteUser()).thenReturn("forwarder");
        when(request.isUserInRole(FORWARDING_ROLE)).thenReturn(true);
        when(request.getHeader("mail")).thenReturn("realuser@example.com");
        when(request.getHeader(FORWARDED_MAIL_HEADER)).thenReturn("user@example.com");

        filter.doFilter(request, response, filterChain);

        assertEquals("realuser@example.com", GroupsThreadStore.getEmail());
    }
}
