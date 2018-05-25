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
package edu.unc.lib.dl.acl.fcrepo4;

import static edu.unc.lib.dl.acl.fcrepo4.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.dl.acl.util.Permission.viewAccessCopies;
import static edu.unc.lib.dl.acl.util.Permission.viewHidden;
import static edu.unc.lib.dl.acl.util.Permission.viewOriginal;
import static edu.unc.lib.dl.model.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

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

    @Test(expected = IllegalArgumentException.class)
    public void testGetDatastreamPermissionNoName() {
        getPermissionForDatastream("");
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
