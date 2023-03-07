
package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.NestedServletException;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author snluong
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration("/download-image-it-servlet.xml")

public class DownloadImageControllerIT extends AbstractAPIIT {
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private SolrQueryLayerService solrSearchService;

    @Test
    public void testGetImageAtFullSize() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        Datastream datastream = mock(Datastream.class);

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        when(record.getDatastreamObject(filePid.getId())).thenReturn(datastream);
        when(datastream.getExtent()).thenReturn("800x1200");

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/full"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();

        assertEquals("image/jpeg", response.getContentType());
        assertEquals("attachment; filename=\"image_full.jpg\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testFullSizeAccessImageNoPermissions() throws Exception {
        PID filePid = makePid();
        ContentObjectSolrRecord record = mock(ContentObjectSolrRecord.class);
        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(record);
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(viewOriginal));

        MvcResult result = mvc.perform(get("/downloadImage/" + filePid.getId() + "/full"))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("", response.getContentAsString(), "Content must not be returned");
//        Throwable exception = assertThrows(NestedServletException.class, () -> mvc.perform(get("/downloadImage/" + filePid.getId() + "/full"))
//                .andExpect(status().isForbidden())
//                .andReturn());
        assertInstanceOf(AccessRestrictionException.class, result.getResolvedException().getClass());
    }
}
