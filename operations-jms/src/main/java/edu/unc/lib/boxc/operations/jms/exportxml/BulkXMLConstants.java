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
package edu.unc.lib.boxc.operations.jms.exportxml;

import java.util.EnumSet;
import java.util.Set;

import edu.unc.lib.boxc.model.api.DatastreamType;

/**
 * Constants used with bulk metadata import/export documents
 *
 * @author bbpennel
 */
public class BulkXMLConstants {
    public final static String BULK_MD_TAG = "bulkMetadata";
    public final static String OBJECT_TAG = "object";
    public final static String DATASTREAM_TAG = "datastream";
    public final static String TYPE_ATTR = "type";
    public final static String PID_ATTR = "pid";
    public final static String PARENT_ID_ATTR = "parent";
    public final static String MIMETYPE_ATTR = "mimetype";
    public final static String MODIFIED_ATTR = "lastModified";
    public final static String OPERATION_ATTR = "operation";
    public final static String OPER_UPDATE_ATTR = "update";

    public final static Set<DatastreamType> DEFAULT_DS_TYPES = EnumSet.of(DatastreamType.MD_DESCRIPTIVE);
    public final static Set<DatastreamType> EXPORTABLE_DS_TYPES = EnumSet.of(
            DatastreamType.MD_DESCRIPTIVE, DatastreamType.MD_DESCRIPTIVE_HISTORY, DatastreamType.MD_EVENTS,
            DatastreamType.TECHNICAL_METADATA, DatastreamType.TECHNICAL_METADATA_HISTORY);
    public final static Set<DatastreamType> UPDATEABLE_DS_TYPES = EnumSet.of(DatastreamType.MD_DESCRIPTIVE);

    private BulkXMLConstants() {
    }
}
