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
package edu.unc.lib.boxc.search.solr.models;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;

/**
 *
 * @author bbpennel
 *
 */
public class ContentObjectSolrRecordTest extends Assert {

    @Test
    public void setRoleGroupsEmpty() {
        ContentObjectSolrRecord mdb = new ContentObjectSolrRecord();
        mdb.setRoleGroup(Arrays.asList(""));
        assertEquals(0, mdb.getGroupRoleMap().size());
        assertEquals(1, mdb.getRoleGroup().size());
    }

    @Test
    public void setRoleGroups() {
        ContentObjectSolrRecord mdb = new ContentObjectSolrRecord();
        mdb.setRoleGroup(Arrays.asList("curator|admin", "patron|public"));
        assertEquals(2, mdb.getGroupRoleMap().size());
        assertEquals(2, mdb.getRoleGroup().size());
    }

    @Test
    public void getParentCollectionNameAndId() {
        ContentObjectSolrRecord solrRecord = new ContentObjectSolrRecord();
        var parentString = "Best Collection|ee8604fb-6d12-4c42-a42f-7fa68679ccbb";
        solrRecord.setParentCollection(parentString);

        assertEquals(parentString, solrRecord.getParentCollection());
        assertEquals("Best Collection", solrRecord.getParentCollectionName());
        assertEquals("ee8604fb-6d12-4c42-a42f-7fa68679ccbb", solrRecord.getParentCollectionId());
    }
}
