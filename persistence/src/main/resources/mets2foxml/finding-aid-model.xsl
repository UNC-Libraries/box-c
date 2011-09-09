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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:m="http://www.loc.gov/METS/"
    xmlns:util="http://foo/"
    xmlns:fa="info:cdr/xmlns/content-model/Finding-Aid-1.0#"
    xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd"
    exclude-result-prefixes="xsl m util">
    <xsl:import href="base-model.xsl"/>

    <xsl:variable name="findingAidEl">
        <fa:FindingAid/>
    </xsl:variable>
    
    <!-- This div template captures processing for this content model. -->
    <xsl:template
        match="m:div[ ancestor::m:structMap/@TYPE=namespace-uri($findingAidEl) ]"
        mode="contentModel">
        <!-- EADFindingAid is the only object type in the FindingAid model. -->        
        <!-- output FOXML -->
        <xsl:apply-templates select="." mode="util:write-foxml">
            <xsl:with-param name="contentModel">info:cdr/xmlns/content-model/Finding-Aid-1.0#FindingAid</xsl:with-param>
            <xsl:with-param name="div" select="."/>
            <xsl:with-param name="slug" select="util:get-object-slug(.)"/>
            <!-- TODO: Default labels are fine for files? <xsl:with-param name="label"/>-->
            <xsl:with-param name="datastreams">
                <xsl:variable name="eadfile" select="key('fileGrpid', m:fptr[1]/@FILEID)/m:file"/>
                <xsl:call-template name="make-url-xml-datastream">
                    <xsl:with-param name="id">DATA_EAD</xsl:with-param>
                    <xsl:with-param name="label">EAD XML File</xsl:with-param>
                    <xsl:with-param name="url" select="$eadfile/m:FLocat/@xlink:href"/>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>

</xsl:stylesheet>
