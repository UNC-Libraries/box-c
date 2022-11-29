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
