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
package edu.unc.lib.boxc.search.api.facets;

/**
 * A hierarchical facet which supports cut off operations
 * @author bbpennel
 */
public interface CutoffFacet extends HierarchicalFacet {

    /**
     * @return The hierarchy node with the highest tier value
     */
    CutoffFacetNode getHighestTierNode();

    /**
     * @return highest tier value held by a node in this facet
     */
    int getHighestTier();

    /**
     * @return lower cutoff tier, objects at tiers below this value should not be returned by this facet.
     */
    Integer getCutoff();

    /**
     * @param cutoff Cutoff value to set
     */
    void setCutoff(Integer cutoff);

    /**
     * @return Tier of values which should be returned in a facet result, as compared to a search result.
     */
    Integer getFacetCutoff();

}