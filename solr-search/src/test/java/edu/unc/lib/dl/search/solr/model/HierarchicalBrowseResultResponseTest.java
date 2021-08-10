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
package edu.unc.lib.dl.search.solr.model;

import static edu.unc.lib.boxc.model.api.ResourceType.Folder;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;


public class HierarchicalBrowseResultResponseTest extends Assert {

    private List<ContentObjectRecord> getMetadataObjects() {
        BriefObjectMetadataBean md1 = new BriefObjectMetadataBean();
        md1.setId("48aeb594-6d95-45e9-bb20-dd631ecc93e9");
        md1.setResourceType(Folder.name());
        Map<String, Long> countMap = new HashMap<>();
        countMap.put("child", 0L);
        md1.setCountMap(countMap);

        BriefObjectMetadataBean md2 = new BriefObjectMetadataBean();
        md2.setId("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9");
        md1.setResourceType(Folder.name());
        countMap = new HashMap<>();
        countMap.put("child", 2L);
        md2.setCountMap(countMap);


        return new ArrayList<>(asList((ContentObjectRecord)md1, (ContentObjectRecord)md2));
    }

    @Test
    public void removeContainersWithoutContents() {
        HierarchicalBrowseResultResponse resp = new HierarchicalBrowseResultResponse();
        resp.setResultList(getMetadataObjects());

        resp.removeContainersWithoutContents();

        assertEquals(1, resp.getResultList().size());
        assertEquals("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9", resp.getResultList().get(0).getId());
    }

    @Test
    public void retainDirectMatches() {
        HierarchicalBrowseResultResponse resp = new HierarchicalBrowseResultResponse();
        resp.setResultList(getMetadataObjects());
        resp.setMatchingContainerPids(Arrays.asList("48aeb594-6d95-45e9-bb20-dd631ecc93e9"));

        resp.removeContainersWithoutContents();

        assertEquals(2, resp.getResultList().size());
        assertEquals("48aeb594-6d95-45e9-bb20-dd631ecc93e9", resp.getResultList().get(0).getId());
        assertEquals("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9", resp.getResultList().get(1).getId());
    }

    @Test
    public void removeContentsDirectMatches() {
        HierarchicalBrowseResultResponse resp = new HierarchicalBrowseResultResponse();
        resp.setResultList(getMetadataObjects());
        resp.setMatchingContainerPids(Arrays.asList("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9"));

        resp.removeContainersWithoutContents();

        assertEquals(1, resp.getResultList().size());
        assertEquals("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9", resp.getResultList().get(0).getId());
    }
}
