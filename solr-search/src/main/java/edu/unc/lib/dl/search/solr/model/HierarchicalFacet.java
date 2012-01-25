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

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Class which stores a single hierarchical facet entry.  Handles the interpretation of a specialized 
 * string format to populate the different parts of the facet:
 * 		<tier>|<search value>(|<display value>)
 * 		or
 * 		<tier>|<search value>/<search value>/<search value>...(|<display value>/<display value>...)
 * If "/" characters appear between search values, they are considered to be hierarchical tiers leading
 * up to the final search value.  Each tier of the hierarchy is stored as a HierarchicalFacetTier.
 * 
 * This object is used for both requests and responses.
 * @author bbpennel
 */
public class HierarchicalFacet extends GenericFacet {
	private static final Logger LOG = LoggerFactory.getLogger(HierarchicalFacet.class);
	private List<HierarchicalFacetTier> facetTiers;
	private Integer cutoffTier = null;
	private static SearchSettings searchSettings;
	
	public HierarchicalFacet(){
	}
	
	/**
	 * Default constructor which takes the name of the facet and the string representing this hierarchical 
	 * facet entry.
	 * @param fieldName name of the facet to which this entry belongs.
	 * @param facetString string from which the attributes of the hierarchical facet will be interpreted.
	 */
	public HierarchicalFacet(String fieldName, String facetString){
		this.count = 0;
		this.fieldName = fieldName;
		this.value = facetString;
		
		this.facetTiers = unpackTiers(facetString);
	}
	
	public HierarchicalFacet(FacetField.Count countObject){
		this(countObject, countObject.getName());
	}
	
	public HierarchicalFacet(FacetField.Count countObject, String fieldName){
		this.count = countObject.getCount();
		this.fieldName = fieldName;
		this.value = countObject.getName();
		try {
			this.facetTiers = unpackTiers(countObject.getName());
		} catch (InvalidHierarchicalFacetException e){
			LOG.error("Failed to instantiate HierarchicalFacet", e);
		}
	}
	
	public HierarchicalFacet(String fieldName, List<HierarchicalFacetTier> tiers, int count){
		this.fieldName = fieldName;
		this.facetTiers = tiers;
		this.count = count;
		sortTiers();
		this.value = facetTiers.get(facetTiers.size() - 1).getValue();
	}
	
	public HierarchicalFacet(HierarchicalFacet facet){
		this.fieldName = facet.getFieldName();
		this.count = facet.getCount();
		this.cutoffTier = facet.getCutoffTier();
		this.facetTiers = new ArrayList<HierarchicalFacetTier>();
		for (HierarchicalFacetTier tier: facet.getFacetTiers()){
			facetTiers.add(new HierarchicalFacetTier(tier));
		}
	}
	
	public static ArrayList<HierarchicalFacetTier> createFacetTierList(ArrayList<String> tierStrings){
		ArrayList<HierarchicalFacet.HierarchicalFacetTier> tierList = new ArrayList<HierarchicalFacet.HierarchicalFacetTier>();
		for (String tierString: tierStrings){
			if (tierString != null && tierString.length() > 0){
				try {
					tierList.add(new HierarchicalFacet.HierarchicalFacetTier(tierString));
				} catch (InvalidHierarchicalFacetException e) {
					LOG.error("Failed to create hierarchical facet tier", e);
				}
			}
		}
		return tierList;
	}
	
	public List<HierarchicalFacetTier> unpackTiers(String facetString){
		if (facetString == null || facetString.length() == 0)
			return null;
		String facetTiers[] = facetString.split(searchSettings.facetTierDelimiter);
		List<HierarchicalFacetTier> facetTierList = new ArrayList<HierarchicalFacetTier>();
		for (String facetTierString: facetTiers){
			HierarchicalFacetTier facetTierEntry = new HierarchicalFacetTier(facetTierString); 
			facetTierList.add(facetTierEntry);
		}
		return facetTierList;
	}
	
	/**
	 * Constructs a list of all the hierarchical tiers contained within a starting facet tier.  This
	 * takes place if the facet tier's search value contains any / characters, as this indicates that
	 * the value is a path rather than a single entry.
	 * @param facetTier Starting facet tier from which the list is derived.
	 * @return A list containing all the individual facet tiers that were constructed by splitting the 
	 * search value of facetTier.
	 */
	public List<HierarchicalFacetTier> unpackTiers(HierarchicalFacetTier facetTier){
		List<HierarchicalFacetTier> facetTierList = new ArrayList<HierarchicalFacetTier>();
		String facetTiers[] = facetTier.getSearchValue().split(searchSettings.facetTierDelimiter);
		String facetTierDisplays[] = facetTier.getDisplayValue().split(searchSettings.facetTierDelimiter);
		if (facetTiers.length == 1){
			facetTierList.add(facetTier);
			return facetTierList;
		} else {
			for (int i=0; i<facetTiers.length; i++){
				HierarchicalFacetTier facetTierEntry = 
					new HierarchicalFacetTier(facetTier.getTier() - facetTiers.length + i + 1, facetTiers[i], facetTierDisplays[i]);
				facetTierList.add(facetTierEntry);
			}
		}
		return facetTierList;
	}
	
	public void addTier(String facetValue){
		if (facetTiers == null){
			facetTiers = new ArrayList<HierarchicalFacetTier>();
		}
		int highestTier = getHighestTier();
		HierarchicalFacetTier newTier = new HierarchicalFacetTier(highestTier + 1, facetValue, null);
		this.facetTiers.add(newTier);
	}
	
	public void addTier(String searchValue, String displayValue){
		if (facetTiers == null){
			facetTiers = new ArrayList<HierarchicalFacetTier>();
		}
		int highestTier = getHighestTier();
		HierarchicalFacetTier newTier = new HierarchicalFacetTier(highestTier + 1, searchValue, displayValue);
		this.facetTiers.add(newTier);
	}
	
	public int getHighestTier(){
		if (facetTiers.size() == 0)
			return 0;
		if (this.facetTiers.get(this.facetTiers.size()-1).searchValue.equals("*"))
			return this.facetTiers.get(this.facetTiers.size()-1).tier - 1;
		return this.facetTiers.get(this.facetTiers.size()-1).tier;
	}
	
	public String getPackedHeadTier(){
		//Returns the deepest tier with a full search value path leading up to it.
		return "";
	}
	
	public String getPackedSearchValue(){
		StringBuffer output = new StringBuffer();
		boolean first = true;
		for (HierarchicalFacetTier facetTier: this.facetTiers){
			if (first)
				first = false;
			else output.append(searchSettings.facetTierDelimiter);
			output.append(facetTier.getTier()).append(searchSettings.facetSubfieldDelimiter).append(facetTier.getSearchValue());
		}
		return output.toString();
	}
	
	public String getHighestTierDisplayValue(){
		if (this.facetTiers.get(this.facetTiers.size()-1).searchValue.equals("*")){
			if (this.facetTiers.size() == 1)
				return null;
			return this.facetTiers.get(this.facetTiers.size()-2).displayValue; 
		}
		return this.facetTiers.get(this.facetTiers.size()-1).displayValue;
	}
	
	public HierarchicalFacetTier getFacetTier(){
		if (this.facetTiers == null || this.facetTiers.size() == 0)
			return null;
		return this.facetTiers.get(this.facetTiers.size()-1);
	}
	
	public HierarchicalFacetTier getFacetTier(int index){
		if (this.facetTiers == null || this.facetTiers.size() == 0 || index > this.facetTiers.size())
			return null;
		return this.facetTiers.get(index);
	}
	
	public String getDisplayValue(){
		return getHighestTierDisplayValue();
	}
	
	public String getDisplayValue(String searchValue){
		HierarchicalFacetTier tier = getTier(searchValue);
		if (tier == null)
			return null;
		return tier.getDisplayValue();
	}
	
	/**
	 * Returns the search key, not including the tier number, of the 
	 * highest tier.
	 * @return
	 */
	public String getSearchKey(){
		if (this.facetTiers == null || this.facetTiers.size() == 0)
			return null;
		return this.facetTiers.get(this.facetTiers.size()-1).getSearchValue();
	}
	
	/**
	 * Returns the tier number and search key of the highest tier.
	 * <tier#>,<search key>
	 */
	public String getSearchValue(){
		if (this.facetTiers == null || this.facetTiers.size() == 0)
			return null;
		return this.facetTiers.get(this.facetTiers.size()-1).getIdentifier();
	}
	
	public String getSearchValue(String searchValue){
		HierarchicalFacetTier tier = getTier(searchValue);
		if (tier == null)
			return null;
		return tier.getIdentifier();
	}
	
	public HierarchicalFacetTier getTier(String searchValue){
		for (HierarchicalFacetTier tier: this.facetTiers){
			if (tier.getSearchValue().equals(searchValue))
				return tier;
		}
		return null;
	}
	
	private void sortTiers(){
		for (int i=0; i < facetTiers.size() - 1; i++){
			HierarchicalFacetTier tier = facetTiers.get(i);
			if (tier.getTier() - 1 != i){
				HierarchicalFacetTier tierSwap = facetTiers.get(tier.getTier()-1);
				facetTiers.set(tier.getTier()-1, tier);
				facetTiers.set(i, tierSwap);
			}
		}
	}
	
	/**
	 * Class containing a single tier of a hierarchical facet.  Stores the display value, search 
	 * value, and tier number of the tier.
	 * @author bbpennel
	 *
	 */
	public static class HierarchicalFacetTier {
		private String displayValue;
		private String searchValue;
		private int tier;
		
		public HierarchicalFacetTier(int tier, String searchValue, String displayValue){
			this.tier = tier;
			this.searchValue = searchValue;
			this.displayValue = displayValue;
		}
		
		/**
		 * Constructor which accepts a string representation of a hierarchical facet, structured:
		 * 	<tier>|<search value>(|<display value>)
		 * If no display value is specified, then the display value is set to the search value.
		 * Search values and tier values are required, otherwise an InvalidHierarchicalFacetException 
		 * is thrown.
		 * @param facetString string representation of a hierarchical facet.
		 * @throws InvalidHierarchicalFacetException
		 */
		public HierarchicalFacetTier(String facetString) throws InvalidHierarchicalFacetException {
			String facetComponents[] = facetString.split(searchSettings.getFacetSubfieldDelimiter(), 3);
			if (facetComponents.length > 0){
				try {
					this.tier = Integer.parseInt(facetComponents[0]);
				} catch (Exception e){
					throw new InvalidHierarchicalFacetException("Invalid tier value.");
				}
			}
			if (facetComponents.length > 1){
				this.searchValue = facetComponents[1];
			} else {
				//If there isn't a search value
				throw new InvalidHierarchicalFacetException("No search value provided.");
			}
			if (facetComponents.length > 2){
				this.displayValue = facetComponents[2];
			} else {
				this.displayValue = this.searchValue;
			}
		}
		
		public HierarchicalFacetTier(HierarchicalFacetTier facetTier) throws InvalidHierarchicalFacetException {
			this.displayValue = facetTier.displayValue;
			this.searchValue = facetTier.searchValue;
			this.tier = facetTier.tier;
		}

		public String getDisplayValue() {
			return displayValue;
		}

		public void setDisplayValue(String displayValue) {
			this.displayValue = displayValue;
		}

		public String getSearchValue() {
			return searchValue;
		}

		public void setSearchValue(String searchValue) {
			this.searchValue = searchValue;
		}

		public int getTier() {
			return tier;
		}

		public void setTier(int tier) {
			this.tier = tier;
		}
		
		public String getValue(){
			return tier + searchSettings.facetSubfieldDelimiter + searchValue + searchSettings.facetSubfieldDelimiter + displayValue;
		}
		
		public String getIdentifier(){
			return tier + searchSettings.facetSubfieldDelimiter + searchValue;
		}
	}

	public List<HierarchicalFacetTier> getFacetTiers() {
		return facetTiers;
	}

	public void setFacetTiers(List<HierarchicalFacetTier> facetTiers) {
		this.facetTiers = facetTiers;
	}

	public Integer getCutoffTier() {
		return cutoffTier;
	}

	public void setCutoffTier(Integer cutoffTier) {
		this.cutoffTier = cutoffTier;
	}

	public SearchSettings getSearchSettings() {
		return searchSettings;
	}

	@Autowired
	public void setSearchSettings(SearchSettings searchSettings) {
		HierarchicalFacet.searchSettings = searchSettings;
	}
}
