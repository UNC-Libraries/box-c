/*
    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

/*

Elements are defined as objects.

title: name of the element for display and form naming
elementTitle: name of the element with namespace (in the form namespace:elementName
repeatable: is the element repeatable (not currently implemented)
type: 'none' means the element does not have any form field, 'text' means the element has a text entry field, 'selection' means the element has a drop-down list, 'textarea' means the element has a multi-row textarea
singleton: only one of the element can be in the XML (not currently implemented)
values: array of strings representing options for the selection type
attributes: array of attribute objects representing the attributes of the element
elements: array of element objects representing the subelements of the element
strict_order: subelements must match the provided ordering

*/

var Title = {
	title : 'title',
	elementTitle : 'mods:title',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var SubTitle = {
	title : 'subTitle',
	elementTitle : 'mods:subTitle',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var PartNumber = {
	title : 'partNumber',
	elementTitle : 'mods:partNumber',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var PartName = {
	title : 'partName',
	elementTitle : 'mods:partName',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var NonSort = {
	title : 'nonSort',
	elementTitle : 'mods:nonSort',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var TitleInfo = {
	title : 'titleInfo',
	elementTitle : 'mods:titleInfo',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ID_attr, xlink_attr, xmllang_attr, script_attr, transliteration_attr, titleInfo_type_attr, authority_attr, authorityURI_attr, valueURI_attr, displayLabel_attr, supplied_attr, usage_attr, altRepGroup_attr, nameTitleGroup_attr ],
	elements : [ Title, SubTitle, PartNumber, PartName, NonSort ]
};

var NamePart = {
	title : 'namePart',
	elementTitle : 'mods:namePart',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ namePart_type_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var DisplayForm = {
	title : 'displayForm',
	elementTitle : 'mods:displayForm',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Affiliation = {
	title : 'affiliation',
	elementTitle : 'mods:affiliation',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var RoleTerm = {
	title : 'roleTerm',
	elementTitle : 'mods:roleTerm',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Role = {
	title : 'role',
	elementTitle : 'mods:role',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ],
	elements : [ RoleTerm ]
};

var Description = {
	title : 'description',
	elementTitle : 'mods:description',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Name = {
	title : 'name',
	elementTitle : 'mods:name',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ID_attr, xlink_attr, xmllang_attr, script_attr, transliteration_attr, name_type_attr, authority_attr, authorityURI_attr, valueURI_attr, displayLabel_attr, usage_attr, altRepGroup_attr, nameTitleGroup_attr ],
	elements : [ NamePart, DisplayForm, Affiliation, Role, Description ]
};

var TypeOfResource = {
	title : 'typeOfResource',
	elementTitle : 'mods:typeOfResource',
	repeatable : true,
	type : 'selection',
	singleton : false,
	values : ['','text', 'cartographic', 'notated music', 'sound recording-musical', 'sound recording-nonmusical', 'sound recording', 'still image', 'moving image', 'three dimensional object', 'software', 'multimedia mixed material'],
        attributes : [ collection_attr, manuscript_attr, displayLabel_attr, usage_attr, altRepGroup_attr ],
	elements: [ ]
};


var Genre = {
	title : 'genre',
	elementTitle : 'mods:genre',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, genre_authority_attr, authorityURI_attr, valueURI_attr, genre_type_attr, displayLabel_attr, usage_attr, altRepGroup_attr],
	elements : [ ]
};

var PlaceTerm = {
	title : 'placeTerm',
	elementTitle : 'mods:placeTerm',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ placeTerm_type_attr, placeTerm_authority_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Place = {
	title : 'place',
	elementTitle : 'mods:place',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ supplied_attr ],
	elements : [ PlaceTerm ]
};

var Publisher = {
	title : 'publisher',
	elementTitle : 'mods:publisher',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ supplied_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var DateIssued = {
	title : 'dateIssued',
	elementTitle : 'mods:dateIssued',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var DateCreated = {
	title : 'dateCreated',
	elementTitle : 'mods:dateCreated',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var DateCaptured = {
	title : 'dateCaptured',
	elementTitle : 'mods:dateCaptured',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var DateValid = {
	title : 'dateValid',
	elementTitle : 'mods:dateValid',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var DateModified = {
	title : 'dateModified',
	elementTitle : 'mods:dateModified',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var CopyrightDate = {
	title : 'copyrightDate',
	elementTitle : 'mods:copyrightDate',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var DateOther = {
	title : 'dateOther',
	elementTitle : 'mods:dateOther',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var Edition = {
	title : 'edition',
	elementTitle : 'mods:edition',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ supplied_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var Issuance = {
	title : 'issuance',
	elementTitle : 'mods:issuance',
	repeatable : true,
	type : 'selection',
	singleton : false,
	values: ['continuing', 'monographic', 'single unit', 'multipart monograph', 'serial', 'integrating resource'],
        attributes : [ ],
	elements : [ ]
};


var Frequency = {
	title : 'frequency',
	elementTitle : 'mods:frequency',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ frequency_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var OriginInfo = {
	title : 'originInfo',
	elementTitle : 'mods:originInfo',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, altRepGroup_attr ],
	elements : [ Place, Publisher, DateIssued, DateCreated, DateCaptured, DateValid, DateModified, CopyrightDate, DateOther, Edition, Issuance, Frequency ]
};


var LanguageTerm = {
	title : 'languageTerm',
	elementTitle : 'mods:languageTerm',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ languageTerm_type_attr, languageTerm_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var ScriptTerm = {
	title : 'scriptTerm',
	elementTitle : 'mods:scriptTerm',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ scriptTerm_type_attr, scriptTerm_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var Language = {
	title : 'language',
	elementTitle : 'mods:language',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ objectPart_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, usage_attr, altRepGroup_attr ],
	elements : [ LanguageTerm, ScriptTerm ]
};

var Form = {
	title : 'form',
	elementTitle : 'mods:form',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ form_authority_attr, authorityURI_attr, valueURI_attr, form_type_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var ReformattingQuality = {
	title : 'reformattingQuality',
	elementTitle : 'mods:reformattingQuality',
	repeatable : true,
	type : 'selection',
	singleton : false,
	values : [ '', 'access', 'preservation', 'replacement'],
        attributes : [ ],
	elements : [ ]
};

var InternetMediaType = {
	title : 'internetMediaType',
	elementTitle : 'mods:internetMediaType',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Phys_Extent = {
	title : 'extent',
	elementTitle : 'mods:extent',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ supplied_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var DigitalOrigin = {
	title : 'digitalOrigin',
	elementTitle : 'mods:digitalOrigin',
	repeatable : true,
	type : 'selection',
	singleton : false,
	values : ['', 'born digital', 'reformatted digital', 'digitized microfilm', 'digitized other analog'],
        attributes : [ ],
	elements : [ ]
};

var Physical_Description_Note = {
	title : 'note',
	elementTitle : 'mods:note',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, note_type_attr, ID_attr ],
	elements : [ ]
};

var PhysicalDescription = {
	title : 'physicalDescription',
	elementTitle : 'mods:physicalDescription',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, altRepGroup_attr ],
	elements : [ Form, ReformattingQuality, InternetMediaType, Phys_Extent, DigitalOrigin, Physical_Description_Note ]
};

var Abstract = {
	title : 'abstract',
	elementTitle : 'mods:abstract',
	repeatable : true,
	type : 'textarea',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, abstract_type_attr, shareable_attr, altRepGroup_attr ],
	elements : [ ]
};
var TableOfContents = {
	title : 'tableOfContents',
	elementTitle : 'mods:tableOfContents',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, tableOfContents_type_attr, shareable_attr, altRepGroup_attr ],
	elements : [ ]
};
var TargetAudience = {
	title : 'targetAudience',
	elementTitle : 'mods:targetAudience',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, targetAudience_authority_attr, authorityURI_attr, valueURI_attr, displayLabel_attr, altRepGroup_attr ],
	elements : [ ]
};
var Note = {
	title : 'note',
	elementTitle : 'mods:note',
	repeatable : true,
	type : 'textarea',
	singleton : false,
        attributes : [ ID_attr, xlink_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, primary_note_type_attr, altRepGroup_attr ],
	elements : [ ]
};

var Occupation = {
	title : 'occupation',
	elementTitle : 'mods:occupation',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ occupation_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Coordinates = {
	title : 'coordinates',
	elementTitle : 'mods:coordinates',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Projection = {
	title : 'projection',
	elementTitle : 'mods:projection',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Scale = {
	title : 'scale',
	elementTitle : 'mods:scale',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Cartographics = {
	title : 'cartographics',
	elementTitle : 'mods:cartographics',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ ],
	elements : [ Scale, Projection, Coordinates ]
};

var CitySection = {
	title : 'citySection',
	elementTitle : 'mods:citySection',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var ExtraterrestrialArea = {
	title : 'extraterrestrialArea',
	elementTitle : 'mods:extraterrestrialArea',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Area = {
	title : 'area',
	elementTitle : 'mods:area',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Island = {
	title : 'island',
	elementTitle : 'mods:island',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var City = {
	title : 'city',
	elementTitle : 'mods:city',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var County = {
	title : 'county',
	elementTitle : 'mods:county',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Territory = {
	title : 'territory',
	elementTitle : 'mods:territory',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var State = {
	title : 'state',
	elementTitle : 'mods:state',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Region = {
	title : 'region',
	elementTitle : 'mods:region',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Province = {
	title : 'province',
	elementTitle : 'mods:province',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Country = {
	title : 'country',
	elementTitle : 'mods:country',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Continent = {
	title : 'continent',
	elementTitle : 'mods:continent',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var HierarchicalGeographic = {
	title : 'hierarchicalGeographic',
	elementTitle : 'mods:hierarchicalGeographic',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ hierarchicalGeographic_authority_attr, authorityURI_attr, valueURI_attr ],
	elements : [ Continent, Country, Province, Region, State, Territory, County, City, Island, Area, ExtraterrestrialArea, CitySection ]
};

var Temporal = {
	title : 'temporal',
	elementTitle : 'mods:temporal',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ temporal_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr, temporal_encoding_attr, point_attr, keyDate_attr, temporal_qualifier_attr ],
	elements : [ ]
};

var Geographic = {
	title : 'geographic',
	elementTitle : 'mods:geographic',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ geographic_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var GeographicCode = {
	title : 'geographicCode',
	elementTitle : 'mods:geographicCode',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ geographicCode_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var Topic = {
	title : 'topic',
	elementTitle : 'mods:topic',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ topic_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var Subject = {
	title : 'subject',
	elementTitle : 'mods:subject',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ID_attr, xlink_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr, subject_authority_attr, authorityURI_attr, valueURI_attr, displayLabel_attr, usage_attr, altRepGroup_attr ],
	elements : [ Topic, Geographic, Temporal, TitleInfo, Name, GeographicCode, Genre, HierarchicalGeographic, Cartographics, Occupation ]
};

var Classification = {
	title : 'classification',
	elementTitle : 'mods:classification',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, classification_authority_attr, authorityURI_attr, valueURI_attr, edition_attr, displayLabel_attr, usage_attr, altRepGroup_attr ],
	elements : [ ]
};

var Identifier = {
	title : 'identifier',
	elementTitle : 'mods:identifier',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, identifier_type_attr, displayLabel_attr, invalid_attr, altRepGroup_attr ],
	elements : [ ]
};

var HoldingExternal = {
	title : 'holdingExternal',
	elementTitle : 'mods:holdingExternal',
	repeatable : false,
	type : 'textarea',
	singleton : false,
        attributes : [ ],
	elements : [ ]
};

var EnumerationAndChronology = {
	title : 'enumerationAndChronology',
	elementTitle : 'mods:enumerationAndChronology',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ unitType_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var CopyInformation_Note = {
	title : 'note',
	elementTitle : 'mods:note',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ ID_attr, xlink_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, ci_note_type_attr ],
	elements : [ ]
};

var ElectronicLocator = {
	title : 'electronicLocator',
	elementTitle : 'mods:electronicLocator',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ ],
	elements : [ ]
};

var ShelfLocator = {
	title : 'shelfLocator',
	elementTitle : 'mods:shelfLocator',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var SubLocation = {
	title : 'subLocation',
	elementTitle : 'mods:subLocator',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var CopyInformation_Form = {
	title : 'form',
	elementTitle : 'mods:form',
	repeatable : false,
	type : 'text',
	singleton : false,
        attributes : [ ci_form_authority_attr, ci_form_type_attr, authorityURI_attr, valueURI_attr, ID_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var CopyInformation = {
	title : 'copyInformation',
	elementTitle : 'mods:copyInformation',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ],
	elements : [ CopyInformation_Form, SubLocation, ShelfLocator, ElectronicLocator,  CopyInformation_Note, EnumerationAndChronology ]
};

var HoldingSimple = {
	title : 'holdingSimple',
	elementTitle : 'mods:holdingSimple',
	repeatable : false,
	type : 'text',
	singleton : false,
        attributes : [ ],
	elements : [ CopyInformation ]
};

var Url = {
	title : 'url',
	elementTitle : 'mods:url',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ dateLastAccessed_attr, displayLabel_attr, url_note_attr, url_access_attr, url_usage_attr ],
	elements : [ ]
};

var PhysicalLocation = {
	title : 'physicalLocation',
	elementTitle : 'mods:physicalLocation',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ physicalLocation_authority_attr, authorityURI_attr, valueURI_attr, displayLabel_attr, physicalLocation_type_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};


var Location = {
	title : 'location',
	elementTitle : 'mods:location',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, altRepGroup_attr ],
	elements : [ PhysicalLocation, ShelfLocator, Url, HoldingSimple, HoldingExternal ]
};

var AccessCondition = {
	title : 'accessCondition',
	elementTitle : 'mods:accessCondition',
	repeatable : true,
	type : 'textarea',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, accessCondition_type_attr, altRepGroup_attr ],
	elements : [ ]
};
var Part_Text = {
	title : 'text',
	elementTitle : 'mods:text',
	repeatable : true,
	type : 'textarea',
	singleton : false,
        attributes : [ xlink_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr, part_text_type_attr ],
	elements : [ ]
};
var Part_Date = {
	title : 'date',
	elementTitle : 'mods:date',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};
var List = {
	title : 'list',
	elementTitle : 'mods:list',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};
var Total = {
	title : 'total',
	elementTitle : 'mods:total',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ ],
	elements : [ ]
};
var End = {
	title : 'end',
	elementTitle : 'mods:end',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};
var Start = {
	title : 'start',
	elementTitle : 'mods:start',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};
var Part_Extent = {
	title : 'extent',
	elementTitle : 'mods:extent',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ unit_attr ],
	elements : [ Start, End, Total, List ],
	order_strict: true
};
var Detail_Title = {
	title : 'title',
	elementTitle : 'mods:title',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ detail_title_type_attr, level_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};
var Caption = {
	title : 'caption',
	elementTitle : 'mods:caption',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};
var Detail_Number = {
	title : 'number',
	elementTitle : 'mods:number',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};
var Detail = {
	title : 'detail',
	elementTitle : 'mods:detail',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ],
	elements : [ Detail_Number, Caption, Title ]
};

var Part = {
	title : 'part',
	elementTitle : 'mods:part',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ID_attr, part_type_attr, order_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr, displayLabel_attr, altRepGroup_attr ],
	elements : [ Detail, Part_Extent, Part_Date, Part_Text ]
};

var Extension = {
	title : 'extension',
	elementTitle : 'mods:extension',
	repeatable : true,
	type : 'textarea',
	singleton : false,
        attributes : [ displayLabel_attr ],
	elements : [ ]
};

var DescriptionStandard = {
	title : 'descriptionStandard',
	elementTitle : 'mods:descriptionStandard',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ descriptionStandard_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var LanguageOfCataloging = {
	title : 'languageOfCataloging',
	elementTitle : 'mods:languageOfCataloging',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ objectPart_attr, altRepGroup_attr, usage_attr, displayLabel_attr ],
	elements : [ LanguageTerm, ScriptTerm ]
};

var RecordOrigin = {
	title : 'recordOrigin',
	elementTitle : 'mods:recordOrigin',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var RecordIdentifier = {
	title : 'recordIdentifier',
	elementTitle : 'mods:recordIdentifier',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var RecordChangeDate = {
	title : 'recordChangeDate',
	elementTitle : 'mods:recordChangeDate',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var RecordCreationDate = {
	title : 'recordCreationDate',
	elementTitle : 'mods:recordCreationDate',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ encoding_attr, point_attr, keyDate_attr, qualifier_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var RecordContentSource = {
	title : 'recordContentSource',
	elementTitle : 'mods:recordContentSource',
	repeatable : true,
	type : 'text',
	singleton : false,
        attributes : [ recordContentSource_authority_attr, authorityURI_attr, valueURI_attr, lang_attr, xmllang_attr, script_attr, transliteration_attr ],
	elements : [ ]
};

var RecordInfo = {
	title : 'recordInfo',
	elementTitle : 'mods:recordInfo',
	repeatable : true,
	type : 'none',
	singleton : true,
        attributes : [ ID_attr, xlink_attr, displayLabel_attr, relatedItem_type_attr ],
	elements : [ RecordContentSource, RecordCreationDate, RecordChangeDate, RecordIdentifier, RecordOrigin, LanguageOfCataloging, DescriptionStandard ]
};

var RelatedItem = {
	title : 'relatedItem',
	elementTitle : 'mods:relatedItem',
	repeatable : true,
	type : 'none',
	singleton : false,
        attributes : [ ID_attr, xlink_attr, displayLabel_attr, relatedItem_type_attr ],
	elements : [ TitleInfo, Name, TypeOfResource, Genre, OriginInfo, Language, PhysicalDescription, Abstract, TableOfContents, TargetAudience, Note, Subject, Classification, Identifier, Location, AccessCondition, Part, Extension, RecordInfo ]
};

var Mods = {
	title : 'mods',
	elementTitle : 'mods:mods',
	repeatable : false,
	type : 'none',
	singleton : true,
        attributes : [  ],
	elements : [ TitleInfo, Name, TypeOfResource, Genre, OriginInfo, Language, PhysicalDescription, Abstract, TableOfContents, TargetAudience, Note, Subject, Classification, RelatedItem, Identifier, Location, AccessCondition, Part, Extension, RecordInfo ]
};

