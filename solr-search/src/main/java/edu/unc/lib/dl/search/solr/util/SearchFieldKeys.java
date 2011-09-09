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
package edu.unc.lib.dl.search.solr.util;

public class SearchFieldKeys {
	public static String ID = "ID";
	public static String ANCESTOR_PATH = "ANCESTOR_PATH";
	public static String ANCESTOR_NAMES = "ANCESTOR_NAMES";
	public static String FILE_ACCESS = "FILE_ACCESS";
	public static String RECORD_ACCESS = "RECORD_ACCESS";
	public static String SURROGATE_ACCESS = "SURROGATE_ACCESS";
	public static String RESOURCE_TYPE = "RESOURCE_TYPE";
	public static String RESOURCE_TYPE_SORT = "RESOURCE_TYPE_SORT";
	public static String DISPLAY_ORDER = "DISPLAY_ORDER";
	public static String CONTENT_TYPE = "CONTENT_TYPE";
	public static String DATASTREAM = "DATASTREAM";
	public static String PARENT_COLLECTION = "PARENT_COLLECTION";
	public static String TITLE = "TITLE";
	public static String OTHER_TITLES = "OTHER_TITLES";
	public static String ABSTRACT = "ABSTRACT";
	public static String KEYWORD = "KEYWORD";
	public static String SUBJECT = "SUBJECT";
	public static String LANGUAGE = "LANGUAGE";
	public static String CREATOR = "CREATOR";
	public static String NAME = "NAME";
	public static String DEPARTMENT = "DEPARTMENT";
	public static String CREATOR_TYPE = "CREATOR_TYPE";
	public static String DATE_CREATED = "DATE_CREATED";
	public static String DATE_ADDED = "DATE_ADDED";
	public static String DATE_UPDATED = "DATE_UPDATED";
	public static String TIMESTAMP = "TIMESTAMP";
	public static String FILESIZE = "FILESIZE";
	public static String DEFAULT_INDEX = "DEFAULT_INDEX";
	public static String TITLE_INDEX = "TITLE_INDEX";
	public static String CONTRIBUTOR_INDEX = "CONTRIBUTOR_INDEX";
	public static String SUBJECT_INDEX = "SUBJECT_INDEX";
	
	public SearchFieldKeys(){
		
	}
	
	public String getID() {
		return ID;
	}
	
	public String getANCESTOR_PATH() {
		return ANCESTOR_PATH;
	}
	
	public String getANCESTOR_NAMES() {
		return ANCESTOR_NAMES;
	}
	
	public String getFILE_ACCESS() {
		return FILE_ACCESS;
	}
	
	public String getRECORD_ACCESS() {
		return RECORD_ACCESS;
	}
	
	public String getSURROGATE_ACCESS() {
		return SURROGATE_ACCESS;
	}
	
	public String getRESOURCE_TYPE() {
		return RESOURCE_TYPE;
	}
	
	public static String getRESOURCE_TYPE_SORT() {
		return RESOURCE_TYPE_SORT;
	}

	public static void setRESOURCE_TYPE_SORT(String rESOURCE_TYPE_SORT) {
		RESOURCE_TYPE_SORT = rESOURCE_TYPE_SORT;
	}

	public String getDISPLAY_ORDER() {
		return DISPLAY_ORDER;
	}
	
	public String getCONTENT_TYPE() {
		return CONTENT_TYPE;
	}
	
	public String getDATASTREAM() {
		return DATASTREAM;
	}
	
	public String getPARENT_COLLECTION() {
		return PARENT_COLLECTION;
	}
	
	public String getTITLE() {
		return TITLE;
	}
	
	public String getABSTRACT() {
		return ABSTRACT;
	}
	
	public String getKEYWORD() {
		return KEYWORD;
	}
	
	public String getSUBJECT() {
		return SUBJECT;
	}
	
	public String getLANGUAGE() {
		return LANGUAGE;
	}
	
	public String getCREATOR() {
		return CREATOR;
	}
	
	public String getNAME() {
		return NAME;
	}
	
	public String getDEPARTMENT() {
		return DEPARTMENT;
	}
	
	public String getCREATOR_TYPE() {
		return CREATOR_TYPE;
	}
	
	public String getDATE_CREATED() {
		return DATE_CREATED;
	}
	
	public String getDATE_ADDED() {
		return DATE_ADDED;
	}
	
	public String getDATE_UPDATED() {
		return DATE_UPDATED;
	}
	
	public String getTIMESTAMP() {
		return TIMESTAMP;
	}
	
	public String getFILESIZE() {
		return FILESIZE;
	}
	
	public String getDEFAULT_INDEX() {
		return DEFAULT_INDEX;
	}
	
	public String getTITLE_INDEX() {
		return TITLE_INDEX;
	}
	
	public String getCONTRIBUTOR_INDEX() {
		return CONTRIBUTOR_INDEX;
	}
	
	public String getSUBJECT_INDEX() {
		return SUBJECT_INDEX;
	}

	public static String getOTHER_TITLES() {
		return OTHER_TITLES;
	}
	
	
}
