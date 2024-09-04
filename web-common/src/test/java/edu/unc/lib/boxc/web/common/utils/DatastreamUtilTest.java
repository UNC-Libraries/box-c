package edu.unc.lib.boxc.web.common.utils;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.DatastreamType.THUMBNAIL_LARGE;
import static edu.unc.lib.boxc.model.api.DatastreamType.THUMBNAIL_SMALL;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;

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
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_DS, THUMB_SMALL_DS, THUMB_LARGE_DS));

        var id = DatastreamUtil.getThumbnailOwnerId(mdObj);
        assertEquals(pid.getId(), id);
        var url = DatastreamUtil.constructThumbnailUrl(id, "small");
        assertEquals(ENDPOINT_URL + "thumb/" + pid.getId() + "/small", url);
    }

    @Test
    public void testConstructThumbnailUrlForPrimaryObject() {
        PID primaryObjPid = makePid();

        PID pid = makePid();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_DS,
                THUMB_SMALL_DS + primaryObjPid.getId(), THUMB_LARGE_DS + primaryObjPid.getId()));

        var id = DatastreamUtil.getThumbnailOwnerId(mdObj);
        assertEquals(primaryObjPid.getId(), id);
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

    @Test
    public void testOriginalFileMimetypeMatches() {
        PID pid = makePid();
        ContentObjectSolrRecord mdObj = new ContentObjectSolrRecord();
        mdObj.setId(pid.getId());
        mdObj.setFileFormatType(asList("text/rtf", "application/pdf"));

        assertTrue( DatastreamUtil.originalFileMimetypeMatches(mdObj, "application/pdf"));
        assertFalse( DatastreamUtil.originalFileMimetypeMatches(mdObj, "image/png"));
    }
}
