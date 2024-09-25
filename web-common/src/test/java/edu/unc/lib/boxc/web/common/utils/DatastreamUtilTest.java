package edu.unc.lib.boxc.web.common.utils;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.DatastreamType.THUMBNAIL_LARGE;
import static edu.unc.lib.boxc.model.api.DatastreamType.THUMBNAIL_SMALL;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author bbpennel
 *
 */
public class DatastreamUtilTest {

    private final static String ENDPOINT_URL = "services/api/";

    private final static String ORIGINAL_DS = ORIGINAL_FILE.getId() + "|image/jpg|image|jpg|555||";
    private final static String ORIGINAL_INDEXABLE = ORIGINAL_FILE.getId() + "|application/pdf|doc.pdf|pdf|5555||";
    private final static String FITS_DS = TECHNICAL_METADATA.getId() + "|text/xml|fits.xml|xml|5555||";
    private final static String THUMB_SMALL_DS = THUMBNAIL_SMALL.getId() + "|image/png|small|png|3333||";
    private final static String THUMB_LARGE_DS = THUMBNAIL_LARGE.getId() + "|image/png|small|png|10000||";

    @BeforeEach
    public void setup() {
        DatastreamUtil.setDatastreamEndpoint(ENDPOINT_URL);
    }

    @Test
    public void testGetOriginalFileUrl() {
        PID pid = makePid();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_DS));

        String url = DatastreamUtil.getOriginalFileUrl(mdObj);
        assertEquals("content/" + pid.getId(), url);
    }

    @Test
    public void testGetOriginalFileIndexableUrl() {
        PID pid = makePid();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_INDEXABLE));

        String url = DatastreamUtil.getOriginalFileUrl(mdObj);
        assertEquals("indexablecontent/" + pid.getId(), url);
    }

    @Test
    public void testGetDatastreamUrl() {
        PID pid = makePid();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(FITS_DS));

        String url = DatastreamUtil.getDatastreamUrl(mdObj, TECHNICAL_METADATA.getId());
        assertEquals("indexablecontent/" + pid.getId() + "/techmd_fits", url);
    }

    @Test
    public void testConstructThumbnailSmall() {
        PID pid = makePid();
        var id = pid.getId();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(id);
        var jp2Datastream = JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|||" + id + "|1200x1200";
        mdObj.setDatastream(asList(ORIGINAL_DS, THUMB_SMALL_DS, THUMB_LARGE_DS, jp2Datastream));

        var ownerId = DatastreamUtil.getThumbnailOwnerId(mdObj);
        assertEquals(id, ownerId);
        var url = DatastreamUtil.constructThumbnailUrl(ownerId, "small");
        assertEquals(ENDPOINT_URL + "thumb/" + ownerId + "/small", url);
    }

    @Test
    public void testConstructThumbnailUrlForPrimaryObject() {
        PID primaryObjPid = makePid();
        var primaryObjId = primaryObjPid.getId();

        PID pid = makePid();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        var jp2Datastream = JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|||" + primaryObjId + "|1200x1200";
        mdObj.setDatastream(asList(ORIGINAL_DS,
                THUMB_SMALL_DS + primaryObjPid.getId(), THUMB_LARGE_DS + primaryObjPid.getId(), jp2Datastream));

        var id = DatastreamUtil.getThumbnailOwnerId(mdObj);
        assertEquals(primaryObjId, id);
        var url = DatastreamUtil.constructThumbnailUrl(id, "large");
        assertEquals(ENDPOINT_URL + "thumb/" + primaryObjPid.getId() + "/large", url);
    }

    @Test
    public void testGetThumbnailUrlNoThumbs() {
        PID pid = makePid();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(List.of(ORIGINAL_DS));

        var id = DatastreamUtil.getThumbnailOwnerId(mdObj);
        assertNull(id);
        assertNull(DatastreamUtil.constructThumbnailUrl(id));
    }
}
