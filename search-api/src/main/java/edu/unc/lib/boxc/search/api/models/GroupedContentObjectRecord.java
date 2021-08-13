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
package edu.unc.lib.boxc.search.api.models;

import java.util.List;

/**
 * Record containing aggregated content object records
 *
 * @author bbpennel
 */
public interface GroupedContentObjectRecord extends ContentObjectRecord {

    /**
     * @return ContentObjectRecord to be used as the representative record in place of the aggregate
     */
    ContentObjectRecord getRepresentative();

    /**
     * @return All items contained in this grouping
     */
    List<ContentObjectRecord> getItems();

    /**
     * @return Total count of items in this grouping, whether or not they were retrieved
     */
    Long getItemCount();

    /**
     * @return identifier for this group
     */
    String getGroupId();
}