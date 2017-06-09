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
public interface HierarchicalFacetNode extends Cloneable {
    /**
     * Display value for the current node
     * @return
     */
    public String getDisplayValue();
    /**
     * Raw search value, the key for this facet node without all the extra syntax needed to search for it
     * @return
     */
    public String getSearchKey();
    /**
     * Value used to perform a search for items matching this facet value, with all necessary syntax
     * @return
     */
    public String getSearchValue();
    /**
     * Full value representing this facet node.  Generally this is the fully formatted value the node is derived from.
     * @return
     */
    public String getFacetValue();

    /**
     * Value used to restrict facet results to just the children of this facet node
     * @return
     */
    public String getPivotValue();
    /**
     * Value used to restrict search results to items matching this node, but not its children
     * @return
     */
    public String getLimitToValue();

    public Object clone();
}
