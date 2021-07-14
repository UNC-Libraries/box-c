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
package edu.unc.lib.boxc.web.common.auth.filters;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.IP_PRINC_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.web.common.auth.filters.StoreUserAccessControlFilter.FORWARDING_ROLE;
import static edu.unc.lib.dl.httpclient.HttpClientUtil.FORWARDED_GROUPS_HEADER;
import static edu.unc.lib.dl.httpclient.HttpClientUtil.FORWARDED_MAIL_HEADER;
import static edu.unc.lib.dl.httpclient.HttpClientUtil.SHIBBOLETH_GROUPS_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.fcrepo.model.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.common.auth.PatronPrincipalProvider;
import edu.unc.lib.boxc.web.common.auth.RemoteUserUtil;
import edu.unc.lib.boxc.web.common.auth.filters.StoreUserAccessControlFilter;

/**
 *
 * @author bbpennel
 *
 */
public class StoreUserAccessControlFilterTest {

    private StoreUserAccessControlFilter filter;

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

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private File configFile;
    private PatronPrincipalProvider patronProvider;


    @Before
    public void init() throws Exception {
        initMocks(this);
        tmpFolder.create();
        configFile = tmpFolder.newFile("patronConfig.json");
        FileUtils.writeStringToFile(configFile, CONFIG, StandardCharsets.US_ASCII);

        patronProvider = new PatronPrincipalProvider();
        patronProvider.setPatronGroupConfigPath(configFile.getAbsolutePath());
        patronProvider.init();

        filter = new StoreUserAccessControlFilter();
        filter.setRetainGroupsThreadStore(true);
        filter.setPatronPrincipalProvider(patronProvider);

        when(request.getServletPath()).thenReturn("/path/to/resource");
    }

    @After
    public void cleanup() {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testDefaultGroupsAssigned() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertEquals("", GroupsThreadStore.getUsername());
        assertNull(GroupsThreadStore.getEmail());
        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        assertTrue("Public must be assigned", accessGroups.contains(PUBLIC_PRINC));
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

        assertTrue("Public must be assigned", accessGroups.contains(PUBLIC_PRINC));
        assertTrue("Authenticated must be assigned", accessGroups.contains(AUTHENTICATED_PRINC));
        assertTrue("User principal must be assigned", accessGroups.contains("unc:onyen:user"));
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

        assertTrue("Public must be assigned", accessGroups.contains(PUBLIC_PRINC));
        assertTrue("Authenticated must be assigned", accessGroups.contains(AUTHENTICATED_PRINC));
        assertTrue("User principal must be assigned", accessGroups.contains("unc:onyen:user"));
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
        assertTrue("Public must be assigned", accessGroups.contains(PUBLIC_PRINC));
        assertTrue("IP Group must be assigned", accessGroups.contains(TEST_IP_PRINC));
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

        assertTrue("Public must be assigned", accessGroups.contains(PUBLIC_PRINC));
        assertTrue("Authenticated must be assigned", accessGroups.contains(AUTHENTICATED_PRINC));
        assertTrue("Group one must be assigned", accessGroups.contains("one"));
        assertTrue("Group one must be assigned", accessGroups.contains("two"));
    }

    @Test
    public void testForwardedGroups() throws Exception {
        when(request.getRemoteUser()).thenReturn("forwarder");
        when(request.isUserInRole(FORWARDING_ROLE)).thenReturn(true);
        when(request.getHeader(FORWARDED_GROUPS_HEADER)).thenReturn("f1;f2");

        filter.doFilter(request, response, filterChain);

        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        assertFalse("Public was not forwarded, must not be assigned", accessGroups.contains(PUBLIC_PRINC));
        assertTrue("Forwarded groups must be assigned", accessGroups.contains("f1"));
        assertTrue("Forwarded groups must be assigned", accessGroups.contains("f2"));
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
