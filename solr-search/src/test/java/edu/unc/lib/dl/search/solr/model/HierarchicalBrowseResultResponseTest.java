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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.util.ContentModelHelper;

public class HierarchicalBrowseResultResponseTest extends Assert {

    private List<BriefObjectMetadata> getMetadataObjects() {
        BriefObjectMetadataBean md1 = new BriefObjectMetadataBean();
        md1.setId("uuid:test1");
        md1.setContentModel(Arrays.asList(ContentModelHelper.Model.CONTAINER.toString()));
        Map<String, Long> countMap = new HashMap<String, Long>();
        countMap.put("child", 0L);
        md1.setCountMap(countMap);

        BriefObjectMetadataBean md2 = new BriefObjectMetadataBean();
        md2.setId("uuid:test2");
        md2.setContentModel(Arrays.asList(ContentModelHelper.Model.CONTAINER.toString()));
        countMap = new HashMap<String, Long>();
        countMap.put("child", 2L);
        md2.setCountMap(countMap);


        return new ArrayList<BriefObjectMetadata>(Arrays.asList((BriefObjectMetadata)md1, (BriefObjectMetadata)md2));
    }

    @Test
    public void removeContainersWithoutContents() {
        HierarchicalBrowseResultResponse resp = new HierarchicalBrowseResultResponse();
        resp.setResultList(getMetadataObjects());

        resp.removeContainersWithoutContents();

        assertEquals(1, resp.getResultList().size());
        assertEquals("uuid:test2", resp.getResultList().get(0).getId());
    }

    @Test
    public void retainDirectMatches() {
        HierarchicalBrowseResultResponse resp = new HierarchicalBrowseResultResponse();
        resp.setResultList(getMetadataObjects());
        resp.setMatchingContainerPids(Arrays.asList("uuid:test1"));

        resp.removeContainersWithoutContents();

        assertEquals(2, resp.getResultList().size());
        assertEquals("uuid:test1", resp.getResultList().get(0).getId());
        assertEquals("uuid:test2", resp.getResultList().get(1).getId());
    }

    @Test
    public void removeContentsDirectMatches() {
        HierarchicalBrowseResultResponse resp = new HierarchicalBrowseResultResponse();
        resp.setResultList(getMetadataObjects());
        resp.setMatchingContainerPids(Arrays.asList("uuid:test2"));

        resp.removeContainersWithoutContents();

        assertEquals(1, resp.getResultList().size());
        assertEquals("uuid:test2", resp.getResultList().get(0).getId());
    }
}
