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
package edu.unc.lib.dl.acl.filter;

import static edu.unc.lib.dl.acl.filter.StoreUserAccessControlFilter.FORWARDING_ROLE;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;

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

    @Before
    public void init() {
        initMocks(this);
        filter = new StoreUserAccessControlFilter();
        filter.setRetainGroupsThreadStore(true);

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
    public void testAuthenticatedUser() throws Exception {
        when(request.getRemoteUser()).thenReturn("user");
        when(request.getHeader("mail")).thenReturn("user@example.com");

        filter.doFilter(request, response, filterChain);

        assertEquals("user", GroupsThreadStore.getUsername());
        assertEquals("user@example.com", GroupsThreadStore.getEmail());

        AccessGroupSet accessGroups = GroupsThreadStore.getAgentPrincipals().getPrincipals();
        verify(request).setAttribute("accessGroupSet", accessGroups);

        assertTrue("Public must be assigned", accessGroups.contains(PUBLIC_PRINC));
        assertTrue("Authenticated must be assigned", accessGroups.contains(AUTHENTICATED_PRINC));
        assertTrue("User principal must be assigned", accessGroups.contains("unc:onyen:user"));
    }

    @Test
    public void testUserWithGroups() throws Exception {
        when(request.getRemoteUser()).thenReturn("user");
        when(request.getHeader("mail")).thenReturn("user@example.com");
        when(request.getHeader(SHIBBOLETH_GROUPS_HEADER)).thenReturn("one;two");

        filter.doFilter(request, response, filterChain);

        assertEquals("user", GroupsThreadStore.getUsername());
        assertEquals("user@example.com", GroupsThreadStore.getEmail());

        AccessGroupSet accessGroups = GroupsThreadStore.getAgentPrincipals().getPrincipals();
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
