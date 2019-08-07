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
package edu.unc.lib.dl.security.access.test;


import org.junit.Test;

public class UserSecurityProfileFilterTest {

    @Test
    public void filterTests() {
        /*UserSecurityProfileFilter filter = new UserSecurityProfileFilter();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        request.setServletPath("/test/");

        request.addHeader("isMemberOf", "");
        try {
            filter.doFilterInternal(request, response, chain);

            UserSecurityProfile user = (UserSecurityProfile)request.getSession().getAttribute("user");
            Assert.assertNotNull(user);

            Assert.assertTrue(user.getUserName().equals(""));
            Assert.assertNotNull(user.getAccessGroups());
            Assert.assertEquals(user.getAccessGroups().size(), 1);
            Assert.assertTrue(user.getAccessGroups().contains(AccessGroupConstants.PUBLIC_GROUP));
            Assert.assertNotNull(user.getDatastreamAccessCache());

            //////////////
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();

            request.setRemoteUser("testUser");
            request.addHeader("isMemberOf", "testGroup");

            filter.doFilterInternal(request, response, chain);
            user = (UserSecurityProfile)request.getSession().getAttribute("user");

            Assert.assertTrue("testUser".equals(user.getUserName()));
            Assert.assertEquals(user.getAccessGroups().size(), 2);

            ////////////
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();

            request.setRemoteUser("user");

            user = new UserSecurityProfile();
            user.setUserName("testUser");
            user.getDatastreamAccessCache().put("testObject", AccessType.FILE);
            request.getSession().setAttribute("user", user);

            filter.doFilterInternal(request, response, chain);
            user = (UserSecurityProfile)request.getSession().getAttribute("user");

            Assert.assertTrue("user".equals(user.getUserName()));
            Assert.assertEquals(user.getAccessGroups().size(), 1);
            Assert.assertEquals(user.getDatastreamAccessCache().size(), 0);

            /////////////
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();

            String groups = "public;unc:lib:cdr:test";
            request.setRemoteUser("testUser");
            request.addHeader("isMemberOf", groups);

            user = new UserSecurityProfile();
            user.setUserName("testUser");
            user.setAccessGroups(groups);
            user.getDatastreamAccessCache().put("testObject", AccessType.FILE);
            request.getSession().setAttribute("user", user);

            filter.doFilterInternal(request, response, chain);
            user = (UserSecurityProfile)request.getSession().getAttribute("user");

            Assert.assertTrue("testUser".equals(user.getUserName()));
            Assert.assertTrue(groups.equals(user.getIsMemeberOf()));
            Assert.assertEquals(user.getAccessGroups().size(), 2);
            Assert.assertEquals(user.getDatastreamAccessCache().size(), 1);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }*/
    }
}
