<?xml version="1.0" encoding="UTF-8"?>
<!--

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

-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:m="http://www.loc.gov/METS/">
    <sch:ns uri="http://www.loc.gov/METS/" prefix="m"/>
    <sch:ns uri="info:lc/xmlns/premis-v2" prefix="p"/>
    <sch:ns uri="http://www.w3.org/1999/xlink" prefix="xlink"/>
    <sch:ns uri="http://cdr.unc.edu/definitions/acl" prefix="acl"/>
        
    <sch:title>Simple Content Model Validation</sch:title>
    
    <sch:let name="profileUrl" value="'http://cdr.unc.edu/METS/profiles/Simple'"/>
    
    <xsl:key name="fileid" match="/m:mets/m:fileSec/m:fileGrp/m:file" use="@ID"/>
    <xsl:key name="filegrpid" match="/m:mets/m:fileSec/m:fileGrp" use="@ID"/>
    <xsl:key name="nestedfilegrpid" match="/m:mets/m:fileSec/m:fileGrp/m:fileGrp" use="@ID"/>
    <xsl:key name="fileuse" match="/m:mets/m:fileSec/m:fileGrp/m:file" use="@USE | parent::m:fileGrp/@USE"/>
    <xsl:key name="dmdid" match="/m:mets/m:dmdSec" use="@ID"/>
    <xsl:key name="amdid" match="/m:mets/m:amdSec" use="@ID"/>
    <xsl:key name="divid" match="/m:mets/m:structMap//m:div" use="@ID"/>
    
    <sch:pattern>
        <sch:title>METS element validation</sch:title>
        <sch:rule context="m:mets">
            <sch:assert test="@PROFILE = $profileUrl">The mets MUST indicate the simple content model.</sch:assert>
        </sch:rule>
    </sch:pattern>
    
    <sch:pattern>
        <sch:title>METS metsHdr validation</sch:title>
        <sch:rule context="m:mets">
        	<sch:assert test="m:metsHdr">The mets MUST include a metsHdr element.</sch:assert>
        </sch:rule>
        <sch:rule context="m:metsHdr">
            <sch:assert test="matches(@CREATEDATE,'^(\d\d\d\d)(-(\d\d)(-(\d\d))?)?([T| ]?(\d\d):(\d\d)(:((\d\d)(\.(\d+))?)?)?(Z|([\+\-]\d\d:\d\d)|([A-Z]{3}))?)?$')">The metsHdr CREATEDATE MUST be valid ISO 8601. (<sch:value-of select="@CREATEDATE"/>)</sch:assert>
            <sch:assert test="m:agent[@ROLE = 'CREATOR']">The metsHdr MUST have at least one agent with ROLE of CREATOR.</sch:assert>
        </sch:rule>
    </sch:pattern>
    
    <sch:pattern>
        <sch:title>METS dmdSec validation</sch:title>
        <sch:rule context="m:dmdSec">
            <sch:assert test="m:mdWrap[@MDTYPE='MODS'] or m:mdRef[@LOCTYPE='URL' and @MDTYPE='MODS']">Any dmdSec MUST either contain or link to MODS xml.</sch:assert>
        </sch:rule>
    </sch:pattern>
    
    <sch:pattern>
        <sch:title>METS rightsMD validation</sch:title>
        <sch:rule context="m:rightsMD">
            <sch:assert test="m:mdWrap[@MDTYPE='OTHER'] and count(m:mdWrap/m:xmlData/acl:accessControl) = 1 and count(m:mdWrap/m:xmlData/*) = 1">rightsMD MUST have type OTHER and wrap one and only one accessControl element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    
    <sch:pattern>
        <sch:title>METS fileSec validation</sch:title>
        <sch:rule context="m:file">
            <sch:assert test="not(parent::m:fileGrp/@USE)">fileGrp elements MUST not have a USE attribute.</sch:assert>
            <sch:assert test="not(@USE) or @USE = 'Master'">The specified USE (<sch:value-of select="@USE"/>) MUST be Master.</sch:assert>
            <sch:assert test="@MIMETYPE">file elements MUST have a MIMETYPE attribute</sch:assert>
            <sch:assert test="string-length(@MIMETYPE) &gt; 2">MIMETYPE attribute cannot be empty.</sch:assert>
            <sch:assert test="not(@CHECKSUMTYPE) or @CHECKSUMTYPE = 'MD5'">If a file element has a CHECKSUMTYPE, it must be MD5.</sch:assert>
            <sch:assert test="not(@CHECKSUMTYPE) or @CHECKSUM">If a file element has a CHECKSUMTYPE, it must also have a CHECKSUM.</sch:assert>
            <sch:assert test="count(m:FLocat) = 1">A file element must have one and only one FLocat element, see file ID '<sch:value-of select="@ID"/>'.</sch:assert>
        </sch:rule>
        <sch:rule context="m:FLocat">
            <sch:assert test="@LOCTYPE = 'URL' or @LOCTYPE = 'OTHER'">File locations MUST be specified as a URL.</sch:assert>
            <sch:assert test="not(contains(@xlink:href,':')) or starts-with(@xlink:href,'http:') or starts-with(@xlink:href,'https:') or starts-with(@xlink:href,'file:') or starts-with(@xlink:href,'irods:') or starts-with(@xlink:href,'tag:')">File locations must be relative or start with a protocol prefix of 'http:', 'https:', 'file:', 'tag:', or 'irods:'.</sch:assert>
        </sch:rule>
    </sch:pattern>
    
    <sch:pattern>
        <sch:title>Structure Map Validation (structMap)</sch:title>
        <sch:rule context="m:structMap">
            <sch:assert test="not(@TYPE) or @TYPE='Basic'">The specified structMap TYPE (<sch:value-of select="@TYPE"/>) MUST be blank or 'Basic'</sch:assert>
        </sch:rule>
        <sch:rule context="m:div[ @TYPE = 'Folder' or @TYPE = 'Aggregate Work' or (not(@TYPE) and (count(m:div) &gt; 0 or count(m:fptr) = 0)) ]">
            <sch:let name="fileMatches" value="key('fileid',m:fptr/@FILEID)"/>
            <sch:let name="fileGrpMatch" value="key('filegrpid',m:fptr/@FILEID)/m:file"/>
            <sch:let name="nestedFileGrpMatch" value="key('nestedfilegrpid',m:fptr/@FILEID)/m:file"/>
            <sch:let name="allFiles" value="insert-before($fileMatches, 0, insert-before($fileGrpMatch, 0, $nestedFileGrpMatch))"/>      
            <sch:assert test="@LABEL or @DMDID">Folders and Aggregate Works MUST have a label or DMDID pointing to MODS with a title.</sch:assert>
            <sch:assert test="count($allFiles) = 0 ">Folders and Aggregate Works MUST NOT reference a file.</sch:assert>
        </sch:rule>
        <sch:rule context="m:div[ @TYPE = 'Collection']">
            <sch:let name="fileMatches" value="key('fileid',m:fptr/@FILEID)"/>
            <sch:let name="fileGrpMatch" value="key('filegrpid',m:fptr/@FILEID)/m:file"/>
            <sch:let name="nestedFileGrpMatch" value="key('nestedfilegrpid',m:fptr/@FILEID)/m:file"/>
            <sch:let name="allFiles" value="insert-before($fileMatches, 0, insert-before($fileGrpMatch, 0, $nestedFileGrpMatch))"/>      
            <!--<sch:assert test="@DMDID">Collections MUST have a MODS record.</sch:assert>-->
            <sch:assert test="count($allFiles) = 0 ">Collections MUST NOT reference a file.</sch:assert>
        </sch:rule>
        <sch:rule context="m:div[ @TYPE = 'File' or (not(@TYPE) and (count(m:div) = 0 and count(m:fptr) = 1)) ]">
            <sch:let name="fileMatches" value="key('fileid',m:fptr/@FILEID)"/>
            <sch:let name="fileGrpMatch" value="key('filegrpid',m:fptr/@FILEID)/m:file"/>
            <sch:let name="nestedFileGrpMatch" value="key('nestedfilegrpid',m:fptr/@FILEID)/m:file"/>
            <sch:let name="allFiles" value="insert-before($fileMatches, 0, insert-before($fileGrpMatch, 0, $nestedFileGrpMatch))"/>            
            <sch:assert test="count($allFiles[@USE = 'Master' or not(@USE)]) = 1">File divs MUST have one file with a USE of Master or no USE attribute. (found <sch:value-of select="count($allFiles)"/> METS file elements)</sch:assert>
        </sch:rule>
        <sch:rule context="m:div">
            <sch:assert test="not(@TYPE) or contains('Bag,Collection,Folder,AdminUnit,Work,Aggregate Work,File', @TYPE)">The specified TYPE (<sch:value-of select="@TYPE"/>) MUST be one of Folder, Aggregate Work, or File.</sch:assert>
        </sch:rule>
        <sch:rule context="m:div[ not(@TYPE) and (count(m:fptr) &gt; 0 and count(m:div) &gt; 0) ]">
            <sch:assert test="false">A div without a TYPE cannot contain both other divs and fptrs.</sch:assert>
        </sch:rule>
    </sch:pattern>
    
    <sch:pattern>
        <sch:title>METS elements not allowed</sch:title>
        <sch:rule context="m:behaviorSec">
            <sch:assert test="false">The behaviorSec element cannot be used in this profile.</sch:assert>
        </sch:rule>
        <sch:rule context="m:techMD">
            <sch:assert test="false">The techMD element cannot be used in this profile.</sch:assert>
        </sch:rule>
    </sch:pattern>
    
    <sch:pattern>
        <sch:title>METS structLink Section</sch:title>
        <sch:let name="referenceURI" value="'http://cdr.unc.edu/definitions/1.0/base-model.xml#refersTo'"/>
        <sch:let name="predicates"
            value="'http://cdr.unc.edu/definitions/1.0/base-model.xml#hasSurrogate
            		http://cdr.unc.edu/definitions/1.0/base-model.xml#hasAlphabeticalOrder
            		http://cdr.unc.edu/definitions/1.0/base-model.xml#defaultWebObject
            		http://cdr.unc.edu/definitions/1.0/base-model.xml#hasSupplemental'"/>
        <sch:rule context="m:structLink/m:smLink">
            <sch:assert test="@xlink:from">The smLink element MUST have an xlink:from attribute.</sch:assert>
            <sch:assert test="@xlink:arcrole">The smLink element MUST have an xlink:arcrole attribute.</sch:assert>
            <sch:assert test="@xlink:to">The smLink element MUST have an xlink:to attribute.</sch:assert>
            <sch:assert test="count(key('divid',substring(@xlink:from,2))) &gt; 0">The xlink:from attribute must refer to a div ID.</sch:assert>
            <sch:assert test="count(key('divid',substring(@xlink:to,2))) &gt; 0">The xlink:to attribute must refer to a div ID.</sch:assert>
            <sch:assert test="contains($predicates,@xlink:arcrole)">The smLink element may ONLY have a supported xlink:arcrole, found <sch:value-of select="@xlink:arcrole"/></sch:assert>
        </sch:rule>
    </sch:pattern>
    
</sch:schema>