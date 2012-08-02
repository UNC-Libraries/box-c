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

Attributes are defined as objects.

title: name of the attribute
type: text (text field) or selection (drop down list)
defaultValue: string to set attribute to by default
values: array of strings to be options of the selection

*/


var collection_attr = {
	title : 'collection',
	type : 'text',
	defaultValue : 'yes',
	values : []
}

var manuscript_attr = {
	title : 'manuscript',
	type : 'text',
	defaultValue : 'yes',
	values : []
}

var nameTitleGroup_attr = {
	title : 'nameTitleGroup',
	type : 'text',
	defaultValue : null,
	values : []
}

var altRepGroup_attr = {
	title : 'altRepGroup',
	type : 'text',
	defaultValue : null,
	values : []
}

var usage_attr = {
	title : 'usage',
	type : 'text',
	defaultValue : 'primary',
	values : []
}

var supplied_attr = {
	title : 'supplied',
	type : 'text',
	defaultValue : 'yes',
	values : []
}

var displayLabel_attr = {
	title : 'displayLabel',
	type : 'text',
	defaultValue : null,
	values : []
}

var valueURI_attr = {
	title : 'valueURI',
	type : 'text',
	defaultValue : null,
	values : []
}

var authorityURI_attr = {
	title : 'authorityURI',
	type : 'text',
	defaultValue : null,
	values : []
}

var genre_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}


var authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var transliteration_attr = {
	title : 'transliteration',
	type : 'text',
	defaultValue : null,
	values : []
}

var script_attr = {
	title : 'script',
	type : 'text',
	defaultValue : null,
	values : []
}

var xmllang_attr = {
	title : 'xml:lang',
	type : 'text',
	defaultValue : null,
	values : []
}

var lang_attr = {
	title : 'lang',
	type : 'text',
	defaultValue : null,
	values : []
}

var xlink_attr = {
	title : 'xlink',
	type : 'text',
	defaultValue : null,
	values : []
}

var ID_attr = {
	title : 'ID',
	type : 'text',
	defaultValue : null,
	values : []
}

var placeTerm_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','code', 'text']
}

var placeTerm_authority_attr = {
	title : 'authority',
	type : 'selection',
	defaultValue : null,
	values : ['','marcgac', 'marcountry', 'iso3166']
}

var languageTerm_authority_attr = {
	title : 'authority',
	type : 'selection',
	defaultValue : null,
	values : ['', 'iso639-2b', 'rfc3066', 'iso639-3', 'rfc4646']
}

var scriptTerm_authority_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','marcgac', 'marcountry', 'iso3166']
}

var targetAudience_authority_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','adolescent', 'adult', 'general', 'juvenile', 'preschool', 'specialized']
}

var titleInfo_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','abbreviated', 'translated', 'alternative', 'uniform']
}

var name_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','personal', 'corporate', 'conference', 'family']
}

var namePart_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','date', 'family', 'given', 'termsOfAddress']
}

var roleTerm_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','code', 'text']
}

var languageTerm_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','code', 'text']
}

var scriptTerm_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','code', 'text']
}

var genre_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','class', 'work type', 'style']
}

var part_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','volume','issue','chapter','section','paragraph','track']
}

var part_text_type_attr = {
	title : 'type',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

var detail_title_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['','part','volume','issue','chapter','section','paragraph','track']
}


var unit_attr = {
	title : 'unit',
	type : 'selection',
	defaultValue : null,
	values : ['','pages','minutes']
}

var level_attr = {
	title : 'level',
	type : 'text',
	defaultValue : null,
	values : []
}

var order_attr = {
	title : 'order',
	type : 'text',
	defaultValue : null,
	values : []
}


var encoding_attr = {
	title : 'encoding',
	type : 'selection',
	defaultValue : null,
	values : ['','iso8601'] // 'w3cdtf', 'iso8601','marc','edtf','temper'
}
var point_attr = {
	title : 'point',
	type : 'selection',
	defaultValue : null,
	values : ['','start','end']
}
var keyDate_attr = {
	title : 'keyDate',
	type : 'text',
	defaultValue : 'yes',
	values : []
}
var qualifier_attr = {
	title : 'qualifier',
	type : 'selection',
	defaultValue : null,
	values : ['','approximate','inferred','questionable']
}
var frequency_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}
var objectPart_attr = {
	title : 'objectPart',
	type : 'text',
	defaultValue : null,
	values : []
}

var subject_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var topic_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var geographic_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var temporal_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var geographicCode_authority_attr = {
	title : 'authority',
	type : 'selection',
	defaultValue : null,
	values : ['', 'marcgac', 'marccountry','iso3166']
}

var hierarchicalGeographic_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var occupation_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var physicalLocation_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}


var classification_authority_attr = {
	title : 'authority',
	type : 'selection',
	defaultValue : null,
	values : ['','accs','acmccs','agricola','agrissc','anscr','ardocs','asb','azdocs','bar','bcl','bcmc','bisacsh','bkl','bliss','blissc','blsrissc','cacodoc','cadocs','ccpgq','celex','chfbn','clc','clutscny','codocs','cslj','cstud',
'cutterec','ddc','dopaed','egedeklass','ekl','farl','farma','fcps','fiaf','finagri','flarch','fldocs','frtav','gadocs','gfdc','ghbs','iadocs','ifzs','inspec','ipc','jelc','kab','kfmod','kktb','knt','ksdocs','kssb','kuvacs','laclaw','ladocs',
'lcc','loovs','methepp','midocs','mmlcc','mf-class','modocs','moys','mpkkl','msc','msdocs','mu','naics','nasasscg','nbdocs','ncdocs','ncsclt','nhcp','nicem','niv','njb','nlm','nmdocs','no-ujur-cmr','no-ujur-cnip','no-ureal-ca','no-ureal-cb',
'no-ureal-cg','noterlyd','nvdocs','nwbib','nydocs','ohdocs','okdocs','oosk','ordocs','padocs','pssppbkj','rich','ridocs','rilm','rpb','rswk','rubbk','rubbkd','rubbkk','rubbkm','rubbkmv','rubbkn','rubbknp','rubbko','rubbks','rueskl','rugasnti',
'rvk','sbb','scdocs','sddocs','sdnb','sfb','siblcs','skb','smm','ssd','ssgn','sswd','stub','suaslc','sudocs','swank','taikclas','taykl','teatkl','txdocs','tykoma','ubtkl/2','udc','uef','undocs','upsylon','usgslcs','utk','utklklass','utklklassex',
'utdocs','veera','vsiso','wadocs','widocs','wydocs','ykl','z','zdbs']
}

var edition_attr = {
	title : 'edition',
	type : 'text',
	defaultValue : null,
	values : []
}


var form_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}
var form_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'material', 'technique']
}

var ci_form_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : []
}

var ci_form_type_attr = {
	title : 'type',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

var ci_note_type_attr = {
	title : 'type',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

var dateLastAccessed_attr = {
	title : 'type',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

var temporal_encoding_attr = {
	title : 'encoding',
	type : 'selection',
	defaultValue : null,
	values : ['', 'iso8601'] // '', 'w3cdtf','iso8601','marc','edtf','temper'
}

var temporal_qualifier_attr = {
	title : 'qualifier',
	type : 'selection',
	defaultValue : null,
	values : ['', 'approximate','inferred','questionable']
}


var note_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'condition', 'marks', 'medium', 'organization', 'physical description', 'physical details', 'presentation', 'script', 'support', 'technique']
}

var primary_note_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'accrual method','accrual policy','acquisition','action','additional physical form','admin','bibliographic history','bibliography','biographical/historical','citation/reference','conservation history','content',
'creation/production credits','date','exhibitions','funding','handwritten','language','numbering','date/sequential designation','original location','original version','ownership','performers','preferred citation','publications',
'reproduction','restriction','source characteristics','source dimensions','source identifier','source note','source type','statement of responsibility','subject completeness','system details','thesis','venue','version identification' ]
}

var tableOfContents_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'incomplete contents', 'partial contents']
}

var abstract_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'review', 'scope', 'content']
}

var relatedItem_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'preceding','succeeding','original','host','constituent', 'series','otherVersion','otherFormat','isReferencedBy','references','reviewOf']
}

var identifier_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'hdl','doi','isbn','isrc','ismn','issn','issue number','istc','lccn','local','matrix number','music number','music publisher','music plate','sici','uri','upc','videorecording identifier','stock number']
}

var accessCondition_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'restriction on access', 'use and reproduction']
}

var physicalLocation_type_attr = {
	title : 'type',
	type : 'selection',
	defaultValue : null,
	values : ['', 'current','discovery','former','creation']
}

var url_access_attr = {
	title : 'access',
	type : 'selection',
	defaultValue : null,
	values : ['', 'preview','raw object','object in context']
}

var url_note_attr = {
	title : 'note',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

var url_usage_attr = {
	title : 'usage',
	type : 'selection',
	defaultValue : null,
	values : ['', 'primary display', 'primary']
}

var unitType_attr = {
	title : 'unitType',
	type : 'selection',
	defaultValue : null,
	values : ['', '1','2','3']
}

var invalid_attr = {
	title : 'invalid',
	type : 'text',
	defaultValue : 'yes',
	values : []
}


var shareable_attr = {
	title : 'shareable',
	type : 'text',
	defaultValue : 'no',
	values : []
}

var recordContentSource_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

var descriptionStandard_authority_attr = {
	title : 'authority',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

var source_attr = {
	title : 'source',
	type : 'text',
	defaultValue : null,
	values : [ ]
}

