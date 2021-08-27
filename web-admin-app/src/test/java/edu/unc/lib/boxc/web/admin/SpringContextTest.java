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
package edu.unc.lib.boxc.web.admin;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/solr-search-context.xml",
        "file:src/main/webapp/WEB-INF/uiapp-servlet.xml",
        "file:src/main/webapp/WEB-INF/service-context.xml",
        "file:src/main/webapp/WEB-INF/access-fedora-context.xml" })
public class SpringContextTest {
    @BeforeClass
    public static void setup() {
        System.setProperty("acl.properties.uri", "acl-config.properties");
    }

    @Test
    public void testLoadSpringContext() {
        // nothing to do here
    }
}
