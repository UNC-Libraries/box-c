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
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
    queryBinding="xslt2">
    <sch:ns uri="http://www.loc.gov/mods/v3" prefix="mods"/>
    <sch:ns uri="http://www.w3.org/1999/xlink" prefix="xlink"/>

    <sch:title>Object-Level MODS Descriptive Metadata Requirements</sch:title>

    <sch:let name="path" value="/edu/unc/lib/dl"/>
    <!--<sch:let name="path" value="/home/count0/workspace/cdla-common/src/main/resources/edu/unc/lib/dl"/>-->
    
    <sch:let name="langISO"
        value="document('/edu/unc/lib/dl/schematron/ISO-639-2_utf-8.xml')//value"/>
    <sch:let name="genreTGM" value="document('/edu/unc/lib/dl/schematron/tgm2.xml')//DESCRIPTOR"/>
    <sch:let name="genreSWAP"
        value="document('/edu/unc/lib/dl/schematron/genres_eprints_swap.xml')//value"/>

    <sch:pattern>
        <sch:title>Prerequisites</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="function-available('document')">The document() function is
            required.</sch:assert>
        </sch:rule>
    </sch:pattern>

    <sch:pattern>
        <sch:title>Title</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:titleInfo) &gt; 0">A title is required.</sch:assert>
        </sch:rule>
    </sch:pattern>

    <sch:pattern>
        <sch:title>Creator</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert
                test="count(mods:name[mods:role/mods:roleTerm/text() = 'creator']) &gt; 0">At
                least one creator name is required.</sch:assert>
        </sch:rule>
    </sch:pattern>

    <sch:pattern>
        <sch:title>Key Date</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:originInfo/mods:*[@keyDate='yes']) &gt; 0">A key date, such as date created,
                is required.</sch:assert>
        </sch:rule>
    </sch:pattern>

    <sch:pattern>
        <sch:title>Language</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:language) &gt; 0">A language is required.</sch:assert>
        </sch:rule>
    </sch:pattern>

<!--    <sch:pattern>
        <sch:title>Keywords</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:subject/mods:topic) &gt; 0">Keywords are
            required</sch:assert>
        </sch:rule>
    </sch:pattern>-->

    <!--<sch:pattern>
        <sch:title>Copyright</sch:title>
        do we want to require both of these? 
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:originInfo/mods:copyrightDate) &gt; 0">A copyright date
                is required.</sch:assert>
        </sch:rule>
         <sch:rule context="mods:mods"> this needs work, what copyright schema? premis? 
            <sch:assert test="count(mods:accessCondition/?:copyright) &gt; 0">A copyright statement is required.</sch:assert>
            </sch:rule>        
            </sch:pattern>-->

    <!--  We need to des
    <sch:pattern>
        <sch:title>Access Rights</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:accessCondition/mods:) &gt; 0">A title is required</sch:assert>
        </sch:rule>
    </sch:pattern>-->

    <sch:pattern>
        <sch:title>Genre Required</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:genre) &gt; 0">A genre is required</sch:assert>
        </sch:rule>
    </sch:pattern>

<!--    <sch:pattern>
        <sch:title>Extent</sch:title>
        <sch:rule context="mods:mods">
            <sch:assert test="count(mods:physicalDescription/mods:extent) &gt; 0">An extent is required</sch:assert>
        </sch:rule>
    </sch:pattern>-->

</sch:schema>
