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
package edu.unc.lib.dl.util;

public class Constants {
	public static final String IR_PREFIX = "/ir/info/";
	public static final String DATA_PREFIX = "/ir/data/";
	public static final String DELETE_OBJECT_PREFIX = "/cdradmin/ir/admin/deleteobject";
	public static final String MOVE_OBJECT_PREFIX = "/cdradmin/ir/admin/moveobject";
	public static final String MOVE_TO_PARENT_PREFIX = "/cdradmin/ir/admin/movetoparent";
	public static final String UPDATE_OBJECT_PREFIX = "/cdradmin/ir/admin/updateobject";
	public static final String METS_INGEST_PREFIX = "/cdradmin/ir/submit/metsubmitbypid";
	public static final String XSL_PREFIX = "/ir/xsl/";
	public static final String DATASTREAM_REPORT_PREFIX = "/ir/xd/";
	public static final String SEARCHINDEX_PREFIX = "/ir/searchindex/";
	public static final String SEARCHREMOVE_PREFIX = "/ir/searchremove/";
	public static final String DS_PREFIX = "ds=";
	public static final String BROWSE_TRUE = "browse=true";
	public static final String COLLECTIONS = "/Collections";
	public static final String USER_DATA_PREFIX = "DATA_";

	// kludge for prototype
	public static final String IMAGE_VIEW_PREFIX = "/ir/imageview/";

	// Web Services
	public static final String NAMESPACE = "http://www.lib.unc.edu/dlservice/schemas";
	public static final String BASIC_QUERY_REQUEST = "BasicQueryRequest";
	public static final String COLLECTIONS_REQUEST = "CollectionsRequest";
	public static final String PATH_INFO_REQUEST = "PathInfoRequest";
	public static final String ID_QUERY_REQUEST = "IdQueryRequest";
	public static final String CONTAINER_QUERY = "ContainerQuery";
	public static final String DELETE_OBJECTS_REQUEST = "DeleteObjectsRequest";
	public static final String GET_BREADCRUMBS_AND_CHILDREN_REQUEST = "GetBreadcrumbsAndChildrenRequest";
	public static final String GET_DATA_REQUEST = "DataRequest";
	public static final String GET_CHILDREN_REQUEST = "GetChildrenRequest";
	public static final String GET_ALL_COLLECTION_PATHS_REQUEST = "GetAllCollectionPathsRequest";
	public static final String GET_FEDORA_DATA_REQUEST = "FedoraDataRequest";
	public static final String ADD_TO_SEARCH_REQUEST = "AddToSearchRequest";
	public static final String MOVE_OBJECT_REQUEST = "MoveObjectRequest";
	public static final String REINDEX_SEARCH_REQUEST = "ReindexSearchRequest";
	public static final String REMOVE_FROM_SEARCH_REQUEST = "RemoveFromSearchRequest";
	public static final String GET_FROM_XML_DATABASE_REQUEST = "GetFromXmlDbRequest";
	public static final String ADD_TO_XML_DATABASE_REQUEST = "AddToXmlDbRequest";
	public static final String REMOVE_FROM_XML_DATABASE_REQUEST = "RemoveFromXmlDbRequest";
	public static final String OVERVIEW_DATA_REQUEST = "OverviewDataRequest";
	public static final String UTF_8 = "UTF-8";
	public static final String ITEM_INFO_REQUEST = "ItemInfoRequest";
	public static final String IMAGE_VIEW_REQUEST = "ImageViewRequest";
	public static final String USER_GROUP_DAO = "UserGroupDAO";
	public static final String CREATE_COLLECTION_OBJECT = "CreateCollectionObject";
	public static final String MEDIATED_SUBMIT_INGEST_OBJECT = "MediatedSubmitIngestObject";
	public static final String METS_SUBMIT_INGEST_OBJECT = "MetsSubmitIngestObject";
	public static final String UPDATE_INGEST_OBJECT = "UpdateIngestObject";
	public static final String MD_DESCRIPTIVE = "MD_DESCRIPTIVE";
	public static final String MD_CONTENTS = "MD_CONTENTS";
	public static final String MD_EVENTS = "MD_EVENTS";
	public static final String MD_DC = "DC";
	public static final String DC_TITLE = "title";

	// Search
	public static final String SEARCH_COLLECTION = "collection";
	public static final String SEARCH_COLLECTION_TITLE = "collectionTitle";
	public static final String SEARCH_CONTENT_MODEL = "contentModel";
	public static final String SEARCH_CONTRIBUTOR = "contributor";
	public static final String SEARCH_CREATOR = "creator";
	public static final String SEARCH_DATASTREAM = "ds";
	public static final String SEARCH_DATE = "coveragetemporal";
	public static final String SEARCH_DATESTRING = "dateString";
	public static final String SEARCH_DISPLAY_DATE = "displayDate";
	public static final String SEARCH_DISPLAY_RESOURCE_TYPE = "displayResourceType";
	public static final String SEARCH_DESCRIPTION = "abstract";
	public static final String SEARCH_ID = "id";
	public static final String SEARCH_ISSUED = "issued";
	public static final String SEARCH_MIMETYPE = "mimetype";
	public static final String SEARCH_DS_1 = "ds1";
	public static final String SEARCH_DS_1_SIZE = "ds1size";
	public static final String SEARCH_DS_1_MIMETYPE = "ds1mimetype";
	public static final String SEARCH_TIMESTAMP = "timestamp";
	public static final String SEARCH_LOCATION = "coveragespatial";
	public static final String SEARCH_ORDER = "order";
	public static final String SEARCH_PARENT = "parent";
	public static final String SEARCH_PARENT_REPO_PATH = "parentRepoPath";
	public static final String SEARCH_PUBLISHER = "publisher";
	public static final String SEARCH_REPO_PATH = "repoPath";
	public static final String SEARCH_RESOURCE_TYPE = "resourceType";
	public static final String SEARCH_SORT_ORDER = "sortOrder";
	public static final String SEARCH_SUBJECT = "subject";
	public static final String SEARCH_TEXT = "text";
	public static final String SEARCH_THUMBNAIL = "preview";
	public static final String SEARCH_TITLE = "title";
	public static final String SEARCH_TYPE = "type";
	public static final String SEARCH_UNKNOWN_DATE = "0-0-0T0:0:0Z";
	public static final String SEARCH_URI = "uri";
	public static final String SEARCH_YEAR = "year";
	public static final String SEARCH_PAGES = "pages";
	public static final String SEARCH_LANGUAGE = "language";
	public static final String SEARCH_KEYWORD = "keyword";
	public static final String SEARCH_CHILD = "child";
	public static final String SEARCH_DATA_ = "DATA_";
	public static final String SEARCH_IS_COLLECTION = "isCollection";

	public static final String SEARCH_TYPE_COLLECTION = "collections"; 
	public static final String SEARCH_TYPE_FOLDER = "folder";
	public static final String SEARCH_TYPE_ITEM = "item";
	
	public static final String SEARCH_FIRST_CONSTITUENT = "firstConstituent";
	public static final String SEARCH_HAS_CONSTITUENT = "hasConstituent";
	public static final String SEARCH_IS_CONSTITUENT_OF = "isConstituentOf";
	public static final String SEARCH_LAST_CONSTITUENT = "lastConstituent";
	public static final String SEARCH_NEXT_CONSTITUENT = "nextConstituent";
	public static final String SEARCH_PREV_CONSTITUENT = "prevConstituent";

	public static final String SEARCH_SORT_ORDER_COLLECTION_VALUE = "100";
	public static final String SEARCH_SORT_ORDER_OTHER_VALUE = "200";
	public static final String SEARCH_IS_COLLECTION_TRUE = "1";
	public static final String SEARCH_IS_COLLECTION_FALSE = "0";
	public static final String SEARCH_RETURN_ALL_RESULTS = "true";
		
	public static final String CONTENT_MODEL_CONTAINER = "info:fedora/cdr-model:Container";
	public static final String CONTENT_MODEL_SIMPLE = "info:fedora/cdr-model:Simple";
	public static final String CONTENT_MODEL_DISK = "info:fedora/cdr-model:Disk";
	public static final String DISPLAY_FOLDER = "Folder";
	public static final String DISPLAY_FILE = "File";
	public static final String DISPLAY_DISK = "Disk";
	
	// Search kludge for prototype
	public static final String SEARCH_PDF_URL = "pdfUri";
	public static final String SEARCH_IMAGE_VIEW = "imageView";
	
	// Fedora resource index
	public static final String RI_PID_PREFIX = "info:fedora/";
	public static final String RI_CREATOR = "http://purl.org/dc/elements/1.1/creator";
	public static final String RI_CONTRIBUTOR = "http://purl.org/dc/elements/1.1/contributor";
	public static final String RI_COVERAGE = "http://purl.org/dc/elements/1.1/coverage";
	public static final String RI_DATE = "http://purl.org/dc/elements/1.1/date";
	public static final String RI_TITLE = "http://purl.org/dc/elements/1.1/title";
	public static final String RI_ISSUED = "http://purl.org/dc/elements/1.1/date";
	public static final String RI_SUBJECT = "http://purl.org/dc/elements/1.1/subject";
	public static final String RI_IDENTIFIER = "http://purl.org/dc/elements/1.1/identifier";
	public static final String RI_DESCRIPTION = "http://purl.org/dc/elements/1.1/description";
	public static final String RI_RIGHTS = "http://purl.org/dc/elements/1.1/rights";
	public static final String RI_SEARCHABLE = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#isSearchable";
	public static final String RI_THUMBNAIL = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#hasThumbnail";
	public static final String RI_TEXT = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#hasText";
	public static final String RI_RESULT_HEADER = "\"key\",\"value\"";
	//public static final String RI_UID = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#uid";
	public static final String RI_REPOSITORY_PATH = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#repositoryPath";
	public static final String RI_ORDER = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#order";
	public static final String RI_CONTENT_MODEL = "info:fedora/fedora-system:def/model#contentModel";
	public static final String RI_DATASTREAM = "info:fedora/fedora-system:def/view#hasDatastream";
	public static final String RI_DISSEMINATOR = "info:fedora/fedora-system:def/view#disseminates";
	public static final String RI_COLLECTION = "info:fedora/fedora-system:def/relations-external#isMemberOfCollection";
	public static final String RI_PUBLISHER = "http://purl.org/dc/elements/1.1/publisher";
	public static final String RI_TYPE = "http://purl.org/dc/elements/1.1/type";
	public static final String RI_IS_CONSTITUENT_OF = "info:fedora/fedora-system:def/relations-external#isConstituentOf";
	public static final String RI_HAS_CONSTITUENT = "info:fedora/fedora-system:def/relations-external#hasConstituent";
	public static final String RI_FIRST_CONSTITUENT = "info:fedora/fedora-system:def/relations-external#firstConstituent";
	public static final String RI_LAST_CONSTITUENT = "info:fedora/fedora-system:def/relations-external#lastConstituent";
	public static final String RI_NEXT_CONSTITUENT = "info:fedora/fedora-system:def/relations-external#nextConstituent";
	public static final String RI_PREV_CONSTITUENT = "info:fedora/fedora-system:def/relations-external#prevConstituent";
	public static final String RI_LABEL = "info:fedora/fedora-system:def/model#label";
	public static final String RI_DATASTREAM_LABEL = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#hasDatastreamLabel";

	public static final String RI_MODEL_COLLECTION = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#Collection";
	public static final String RI_HAS_MODEL = "info:fedora/fedora-system:def/model#hasModel";
	public static final String RI_MODEL_FOLDER = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#Folder";
	public static final String RI_MODEL_SIMPLE = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#Simple";
	public static final String RI_REPOSITORY_ROOT = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#repository-root";
	public static final String RI_IS_PART_OF = "info:fedora/fedora-system:def/relations-external#isPartOf";
	public static final String RI_FEDORA_OBJECT = "info:fedora/fedora-system:FedoraObject";
	public static final String RI_SLUG = "http://cdr.unc.edu/definitions/1.0/base-model.xml#slug";

	public static final String RI_ALLOW_INDEXING = "yes";

	// Content model values
	public static final String CONTENT_MODEL_PART = "Part";
	public static final String CONTENT_MODEL_COLLECTION = "Collection";
	public static final String CONTENT_MODEL_FOLDER = "Folder";
	public static final String CONTENT_MODEL_ITEM = "Item";

	// Resource Types
	public static final String RESOURCE_TYPE_JOURNAL_COLLECTION = "Journal Collection";
	public static final String RESOURCE_TYPE_JOURNAL_YEAR = "Journal Year";
	public static final String RESOURCE_TYPE_JOURNAL_ISSUE = "Journal Issue";
	public static final String RESOURCE_TYPE_JOURNAL_ARTICLE = "Journal Article";
	public static final String RESOURCE_TYPE_JOURNAL_CONTENTS = "Journal Contents";
	public static final String RESOURCE_TYPE_JOURNAL_FRONT_MATTER = "Journal Front Matter";
	public static final String RESOURCE_TYPE_JOURNAL_BACK_MATTER = "Journal Back Matter";

	public static final String RESOURCE_TYPE_CONFERENCE_COLLECTION = "Conference Collection";
	public static final String RESOURCE_TYPE_CONFERENCE_ITEM = "Conference Item";

	public static final String RESOURCE_TYPE_RESEARCH_COLLECTION = "Research Collection";
	public static final String RESOURCE_TYPE_RESEARCH_ITEM = "Research Item";

	public static final String RESOURCE_TYPE_SCHOLAR_COLLECTION = "scholar collection";
	public static final String RESOURCE_TYPE_PRESENTATION_SLIDES = "presentation slides";
	public static final String RESOURCE_TYPE_OVERVIEW_DOCUMENT = "overview document";

	// View Names
	public static final String VIEW_COLLECTIONS = "collections";
	public static final String VIEW_IMAGE_VIEW = "imageview";
	public static final String VIEW_ITEM = "item";
	public static final String VIEW_OBJECT = "object";
	public static final String VIEW_SEARCH = "search";
	public static final String VIEW_SEARCH_RESULTS = "searchresults";
	public static final String VIEW_JOURNAL_COLLECTION = "journalcollection";
	public static final String VIEW_JOURNAL_YEAR = "journalyear";
	public static final String VIEW_JOURNAL_ISSUE = "journalissue";
	public static final String VIEW_JOURNAL_ARTICLE = "journalarticle";
	public static final String VIEW_CONFERENCE_COLLECTION = "conferencecollection";
	public static final String VIEW_CONFERENCE_ITEM = "researchitem";
	public static final String VIEW_RESEARCH_COLLECTION = "researchcollection";
	public static final String VIEW_RESEARCH_ITEM = "researchitem";

	// Fedora
	public static final String PID = "pid";

	// Parsing
	public static final String FORWARD_SLASH = "/";
	public static final String EMPTY_STRING = "";
	public static final String DOUBLE_QUOTE = "\"";
	public static final String PIPE = "|";

	// XML DB
	public static final String XML_DB_ERROR_START = "<XmlDbError>";
	public static final String XML_DB_ERROR_END = "</XmlDbError>";
	public static final String XML_DB_NO_QUERY_TEXT = "No query found";
	public static final String XML_DB_BAD_QUERY_TEXT = "Query not allowed: ";
	public static final String XML_DB_NO_QUERY_RESPONSE = "No response to query: ";
	public static final String XML_DB_XUPDATE = "xupdate";
	public static final String XML_DB_CDATA_START = "<![CDATA[";
	public static final String XML_DB_CDATA_END = "]]>";

	// User Management types
	public static final String CREATE_USER = "CREATE_USER";
	public static final String CREATE_GROUP = "CREATE_GROUP";
	public static final String DELETE_USER = "DELETE_USER";
	public static final String DELETE_GROUP = "DELETE_GROUP";
	public static final String ADD_USER_TO_GROUP = "ADD_USER_TO_GROUP";
	public static final String REMOVE_USER_FROM_GROUP = "REMOVE_USER_FROM_GROUP";
	public static final String GET_USERS_IN_GROUP = "GET_USERS_IN_GROUP";
	public static final String GET_USERS_OUTSIDE_GROUP = "GET_USERS_OUTSIDE_GROUP";
	public static final String GET_USERS = "GET_USERS";
	public static final String GET_GROUPS = "GET_GROUPS";

	// group has objects; user has objects
	public static final String EXISTS = "EXISTS";
	public static final String NON_EMPTY = "NON_EMPTY";

	public static final String SUCCESS = "SUCCESS";
	public static final String FAILURE = "FAILURE";
	public static final String IN_PROGRESS_THREADED = "IN_PROGRESS_THREADED";
	
	public static final String UI_COLLECTIONS_TABLE_BEGIN = "<table id=\"collections\"><thead><tr><th>Title</th><th>Creator</th><th>Files</th><th>Last Updated</th></tr></thead><tbody>";
	public static final String UI_TABLE_EVEN_TR_BEGIN = "<tr";
	public static final String UI_TABLE_ODD_TR_BEGIN = "<tr";
	public static final String UI_TABLE_TR_END = ">";
	public static final String UI_TABLE_TITLE_BEGIN = "<td>";
	public static final String UI_TABLE_TITLE_ABSTRACT_BEGIN = "<span class=\"abstract expandable\">";
	public static final String UI_TABLE_CREATOR_BEGIN = "</span></td><td>";
	public static final String UI_TABLE_FILES_BEGIN = "</td><td>";
	public static final String UI_TABLE_LAST_UPDATED_BEGIN = "</td><td>";
	public static final String UI_TABLE_ROW_END = "</td></tr>";
	public static final String UI_TABLE_END = "</tbody></table>";
	public static final String UI_FOLDER_TABLE_BEGIN = "<table id=\"items\"><thead><tr><th>Title</th><th>Files</th><th>Last Updated</th></tr></thead><tbody>";
}
