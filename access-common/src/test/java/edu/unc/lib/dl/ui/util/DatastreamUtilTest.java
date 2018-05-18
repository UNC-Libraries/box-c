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
package edu.unc.lib.dl.ui.util;

import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.dl.model.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.dl.model.DatastreamType.THUMBNAIL_SMALL;
import static edu.unc.lib.dl.test.TestHelper.makePid;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;

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

    @Before
    public void setup() {
        DatastreamUtil.setDatastreamEndpoint(ENDPOINT_URL);
    }

    @Test
    public void testGetOriginalFileUrl() {
        PID pid = makePid();
        BriefObjectMetadataBean mdObj = new BriefObjectMetadataBean();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_DS));

        String url = DatastreamUtil.getOriginalFileUrl(mdObj);
        assertEquals("content/" + pid.getId(), url);
    }

    @Test
    public void testGetOriginalFileIndexableUrl() {
        PID pid = makePid();
        BriefObjectMetadataBean mdObj = new BriefObjectMetadataBean();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_INDEXABLE));

        String url = DatastreamUtil.getOriginalFileUrl(mdObj);
        assertEquals("indexablecontent/" + pid.getId(), url);
    }

    @Test
    public void testGetDatastreamUrl() {
        PID pid = makePid();
        BriefObjectMetadataBean mdObj = new BriefObjectMetadataBean();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(FITS_DS));

        String url = DatastreamUtil.getDatastreamUrl(mdObj, TECHNICAL_METADATA.getId());
        assertEquals("indexablecontent/" + pid.getId() + "/techmd_fits", url);
    }

    @Test
    public void testGetThumbnailUrl() {
        PID pid = makePid();
        BriefObjectMetadataBean mdObj = new BriefObjectMetadataBean();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_DS, THUMB_SMALL_DS));

        String url = DatastreamUtil.getThumbnailUrl(mdObj, "small");
        assertEquals(ENDPOINT_URL + "thumb/" + pid.getId() + "/small", url);
    }

    @Test
    public void testGetThumbnailUrlForPrimaryObject() {
        PID primaryObjPid = makePid();

        PID pid = makePid();
        BriefObjectMetadataBean mdObj = new BriefObjectMetadataBean();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_DS, THUMB_SMALL_DS + primaryObjPid.getId()));

        String url = DatastreamUtil.getThumbnailUrl(mdObj, "small");
        assertEquals(ENDPOINT_URL + "thumb/" + primaryObjPid.getId() + "/small", url);
    }

    @Test
    public void testGetThumbnailUrlNoThumbs() {
        PID pid = makePid();
        BriefObjectMetadataBean mdObj = new BriefObjectMetadataBean();
        mdObj.setId(pid.getId());
        mdObj.setDatastream(asList(ORIGINAL_DS));

        String url = DatastreamUtil.getThumbnailUrl(mdObj, "small");
        assertEquals("", url);
    }
}
