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
package edu.unc.lib.dl.search.solr.model;

/**
 * 
 * @author bbpennel
 *
 */
public interface SearchFacet {

    /**
     * Returns the name of the field this facet is representing
     *
     * @return
     */
    public String getFieldName();

    /**
     * Returns the number of results matching this facet
     *
     * @return
     */
    public long getCount();

    /**
     * Returns the string value assigned to this facet
     *
     * @return
     */
    public String getValue();

    /**
     * Returns the value used for displaying the facets value to users
     *
     * @return
     */
    public String getDisplayValue();

    /**
     * Returns the value used for searching for this facet
     *
     * @return
     */
    public String getSearchValue();

    /**
     * Returns the value for limiting results to this facet, formatted such that
     * this facet type can parse it into a new SearchFacet. Used as the search
     * value which appears in API calls, but not necessarily the format expected
     * by the search index.
     *
     * @return
     */
    public String getLimitToValue();
}
