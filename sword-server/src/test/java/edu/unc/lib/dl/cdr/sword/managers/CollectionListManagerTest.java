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
package edu.unc.lib.dl.cdr.sword.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.swordapp.server.AuthCredentials;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.managers.CollectionListManagerImpl;

@Ignore
public class CollectionListManagerTest extends Assert {
    private static Logger log = Logger.getLogger(CollectionListManagerTest.class);

    private CollectionListManagerImpl manager;
    private SwordConfigurationImpl config;

    @Before
    public void setUp() throws Exception {
        manager = new CollectionListManagerImpl();

//        aclService = mock(AccessControlService.class);
//        ObjectAccessControlsBean objectACLs = mock(ObjectAccessControlsBean.class);
//        when(objectACLs.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
//        when(aclService.getObjectAccessControls(any(PID.class))).thenReturn(objectACLs);
//        manager.setAclService(aclService);
//
//        tripleStoreQueryService = mock(TripleStoreQueryService.class);

//        manager.setTripleStoreQueryService(tripleStoreQueryService);

        config = new SwordConfigurationImpl();
        config.setBasePath("https://localhost/services");
        config.setSwordPath("https://localhost/services/sword");
        config.setSwordVersion("1.3");
        config.setAdminDepositor("admin");
    }

    @Test
    public void listCollections() throws Exception {
        String pidString = "uuid:23425234532434";
        String url = "https://localhost/services/collection/" + pidString;
        IRI iri = new IRI(url);

//        Map<?,?> response = this.generateImmediateChildrenResponse(10);
//
//        when(tripleStoreQueryService.sendSPARQL(anyString())).thenReturn(response);

        Feed feed = manager.listCollectionContents(iri, new AuthCredentials("admin","",""), config);
        assertEquals(feed.getEntries().size(), 10);
        assertEquals(feed.getLinks().size(), 1);
        assertTrue(pidString.equals(feed.getId().toString()));
    }

    @Test
    public void listCollectionsSecondPage() throws Exception {
        log.debug("Second page test");
        String pidString = "uuid:23425234532434";
        String url = "https://localhost/services/collection/" + pidString + "/1";
        IRI iri = new IRI(url);

//        Map<?,?> response = this.generateImmediateChildrenResponse(10);
//
//        when(tripleStoreQueryService.sendSPARQL(endsWith(" 0"))).thenReturn(null);
//        when(tripleStoreQueryService.sendSPARQL(endsWith(" 10"))).thenReturn(response);

        Feed feed = manager.listCollectionContents(iri, new AuthCredentials("admin","",""), config);
        assertEquals(feed.getEntries().size(), 10);
        assertEquals(feed.getLinks().size(), 1);
        String nextLink = feed.getLink("next").getHref().toString();
        nextLink = nextLink.substring(nextLink.lastIndexOf("/")+1);
        assertTrue("2".equals(nextLink));
        assertTrue(pidString.equals(feed.getId().toString()));
    }

    private Map<String,Map<?,?>> generateImmediateChildrenResponse(int count){
        List<Map<?,?>> bindings = new ArrayList<>();
        for (int i=0; i<count; i++){
            Map<String,Map<String,String>> row = new HashMap<>();
            Map<String,String> pid = new HashMap<>();
            Map<String,String> slug = new HashMap<>();

            pid.put("value", "uuid:" + i);
            slug.put("value", "slug" + i);

            row.put("pid", pid);
            row.put("slug", slug);
            bindings.add(row);
        }
        Map<String,Map<?,?>> response = new HashMap<>();
        Map<String,List<?>> results = new HashMap<>();

        response.put("results", results);
        results.put("bindings", bindings);
        return response;
    }
}
