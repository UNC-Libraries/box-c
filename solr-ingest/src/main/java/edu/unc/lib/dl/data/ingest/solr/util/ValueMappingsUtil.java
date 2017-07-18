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
package edu.unc.lib.dl.data.ingest.solr.util;

import java.util.Collection;

import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class ValueMappingsUtil {

    public static String getResourceType(Collection<String> contentModels) {
        if (contentModels.contains(ContentModelHelper.Model.COLLECTION.getPID().getURI())) {
            return "Collection";
        }
        if (contentModels.contains(ContentModelHelper.Model.AGGREGATE_WORK.getPID().getURI())) {
            return "Aggregate";
        }
        if (contentModels.contains(ContentModelHelper.Model.CONTAINER.getPID().getURI())) {
            return "Folder";
        }
        if (contentModels.contains(ContentModelHelper.Model.SIMPLE.getPID().getURI())) {
            return "Item";
        }
        return null;
    }
}
