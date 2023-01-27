package edu.unc.lib.boxc.auth.api.services;

import static edu.unc.lib.boxc.auth.api.Permission.viewAccessCopies;
import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author bbpennel
 *
 */
public class DatastreamPermissionUtilTest {

    @Test
    public void testGetDatastreamPermissionByName() {
        assertEquals(viewOriginal, getPermissionForDatastream(ORIGINAL_FILE.getId()));
    }

    @Test
    public void testGetDatastreamPermissionNoName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> getPermissionForDatastream(""));
    }

    @Test
    public void testGetDatastreamPermissionUnknownType() {
        assertEquals(viewHidden, getPermissionForDatastream("unknown"));
    }

    @Test
    public void testGetDatastreamPermissionByType() {
        assertEquals(viewAccessCopies, getPermissionForDatastream(JP2_ACCESS_COPY));
    }
}
