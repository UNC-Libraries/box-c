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

import static edu.unc.lib.dl.acl.util.Permission.viewHidden;
import static edu.unc.lib.dl.model.DatastreamType.getByIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.model.DatastreamType;

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
     * @param dsName name of the datastream.  Required.
     * @return permission
     */
    public static Permission getPermissionForDatastream(String dsName) {
        if (StringUtils.isBlank(dsName)) {
            throw new IllegalArgumentException("A non-null datastream name must be provided");
        }

        DatastreamType datastream = getByIdentifier(dsName);
        // If the requested datastream is not a known type, consider it to be administrative
        if (datastream == null) {
            return viewHidden;
        }

        return getPermissionForDatastream(datastream);
    }

    /**
     * Determine the Permission which applies to accessing the specified datastream.
     *
     * @param datastream datastream type to check. Required.
     * @return permission
     */
    public static Permission getPermissionForDatastream(DatastreamType datastream) {
        Assert.notNull(datastream);

        return datastream.getAccessPermission();
    }
}
