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
package edu.unc.lib.boxc.indexing.solr.utils;

import edu.unc.lib.boxc.search.api.ContentCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author bbpennel
 */
public class ContentTypeUtils {
    private ContentTypeUtils() {
    }

    /**
     * @param category
     * @return String representation of the category portion of the contentType facet
     */
    public static String constructContentCategoryFacet(ContentCategory category) {
        return '^' + category.getJoined();
    }

    /**
     * @param category
     * @param extension
     * @return String representation of extension portion of contentType facet
     */
    public static String constructExtensionFacet(ContentCategory category, String extension) {
        StringBuilder contentType = new StringBuilder();
        contentType.append('/').append(category.name()).append('^');
        if (extension == null) {
            contentType.append("unknown,unknown");
        } else {
            contentType.append(extension).append(',').append(extension);
        }
        return contentType.toString();
    }

    /**
     * Construct content type facet values and add them to the provided facet list
     * @param category
     * @param extension
     * @param facetList
     */
    public static void addContentTypeFacets(ContentCategory category, String extension, Collection<String> facetList) {
        facetList.add(constructContentCategoryFacet(category));
        facetList.add(constructExtensionFacet(category, extension));
    }

    /**
     * @param category
     * @param extension
     * @return new list containing content type facet values
     */
    public static List<String> constructContentTypeFacets(ContentCategory category, String extension) {
        var facetList = new ArrayList<String>();
        addContentTypeFacets(category, extension, facetList);
        return facetList;
    }
}
