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

import static edu.unc.lib.dl.acl.util.Permission.viewAccessCopies;
import static edu.unc.lib.dl.acl.util.Permission.viewHidden;
import static edu.unc.lib.dl.acl.util.Permission.viewMetadata;
import static edu.unc.lib.dl.acl.util.Permission.viewOriginal;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.JPEG_2000;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.LARGE_THUMBNAIL;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.ORIGINAL_FILE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.SMALL_THUMBNAIL;

import org.apache.commons.lang3.StringUtils;

import edu.unc.lib.dl.acl.util.Permission;

/**
 * Helper methods for determining permissions of datastreams.
 *
 * @author bbpennel
 *
 */
public class DatastreamPermissionUtil {
    private DatastreamPermissionUtil() {
    }

    /**
     * Determine the Permission which applies to accessing the specified datastream.
     *
     * @param datastream name of the datastream.  Required.
     * @return permission
     */
    public static Permission getPermissionForDatastream(String datastream) {
        if (StringUtils.isBlank(datastream)) {
            throw new IllegalArgumentException("A non-null datastream name must be provided");
        }

        if (ORIGINAL_FILE.equals(datastream)) {
            return viewOriginal;
        }
        if (SMALL_THUMBNAIL.equals(datastream)) {
            return viewMetadata;
        }
        if (LARGE_THUMBNAIL.equals(datastream)) {
            return viewMetadata;
        }
        if (JPEG_2000.equals(datastream)) {
            return viewAccessCopies;
        }
        // All other datastreams are considered administrative
        return viewHidden;
    }
}
