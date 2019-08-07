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
    xmlns:m="http://www.loc.gov/METS/" xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:common="http://cdr.unc.edu/common/" xmlns:basic="http://cdr.unc.edu/basic/"
    xmlns:f="info:fedora/fedora-system:def/foxml#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:rels="info:fedora/fedora-system:def/relations-external#"
    xmlns:fmodel="info:fedora/fedora-system:def/model#"
    xmlns:ir="http://cdr.unc.edu/definitions/1.0/base-model.xml#"
    xmlns:premis="info:lc/xmlns/premis-v2"
    xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd"
    exclude-result-prefixes="xsl common basic">
    <xsl:import href="common.xsl"/>

    <!-- TODO consolidate this into the main label generator in common!! -->
    <!--   <xsl:template match="m:div" mode="common:generate-file-label">
        <xsl:variable name="href" select="key('fileid',m:fptr/@FILEID)[1]/m:FLocat/@xlink:href"/>
        <xsl:variable name="longLabel">
            <xsl:choose>
                <xsl:when test="@LABEL">
                    <xsl:value-of select="@LABEL"/>
                    <xsl:choose>
                        <xsl:when test="exists(@ORDERLABEL)">
                            <xsl:value-of select="@ORDERLABEL"/>
                        </xsl:when>
                        <xsl:when test="exists(@ORDER)">
                            <xsl:value-of select="@ORDER"/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="@DMDID">
                    <xsl:value-of
                        select="key('dmdid',@DMDID)/m:mdWrap/m:xmlData/mods:mods/mods:titleInfo/mods:title"
                    />
                </xsl:when>
                <xsl:when test="starts-with($href, 'file:') or not(contains($href, ':'))">
                    <xsl:choose>
                        <xsl:when test="contains($href, '\')">
                            <xsl:value-of select="substring-after($href,'\')"/>
                        </xsl:when>
                        <xsl:when test="contains($href, '/')">
                            <xsl:value-of select="substring-after($href,'/')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$href"/>
                        </xsl:otherwise>
                    </xsl:choose>
                    <!- TODO add file name extraction for irods URLs 
                </xsl:when>
                <xsl:when test="@ID">
                    <xsl:value-of select="@ID"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="Untitled "/>
                    <xsl:if test="@TYPE">
                        <xsl:value-of select="concat(@TYPE,' ')"/>
                    </xsl:if>
                    <xsl:choose>
                        <xsl:when test="exists(@ORDERLABEL)">
                            <xsl:value-of select="@ORDERLABEL"/>
                        </xsl:when>
                        <xsl:when test="exists(@ORDER)">
                            <xsl:value-of select="@ORDER"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="count(preceding-sibling::m:div)+1"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="substring($longLabel,0,$max-label-length)"/>
    </xsl:template>
-->
    <xsl:template match="m:div" mode="basic:get-div-role">

        <!-- get source files -->
        <xsl:variable name="linkedFiles">
            <xsl:apply-templates select="." mode="common:get-files-for-div"/>
        </xsl:variable>
        <!--<xsl:variable name="linkedFiles" select="key('fileid',m:fptr/@FILEID)"/>-->
        <!--<xsl:variable name="useFiles"
            select="/m:mets/m:fileSec/m:fileGrp/m:file[not(@USE) or @USE = 'Master']"/>-->
        <!-- intersection of two sets -->
        <xsl:variable name="sourceFiles" select="$linkedFiles/m:file[not(@USE) or @USE = 'Master']"/>

        <!-- Role of this div, is it a Container, a File or a Reference -->
        <xsl:choose>

            <!-- Collection must be specified -->
            <xsl:when test="@TYPE='Collection'">Collection</xsl:when>

            <!-- Container is default when it contains more divs -->
            <xsl:when
                test="@TYPE='Folder' or
                ( not(@TYPE) and count($sourceFiles) = 0 )"
                >Container</xsl:when>

            <xsl:when test="@TYPE='AlphabetizedFolder'">Container</xsl:when>

            <!-- explicit File objects -->
            <xsl:when test="@TYPE='File'">Simple</xsl:when>

            <!-- image default based on a single source file and image mimetype -->
            <xsl:when
                test="not(@TYPE) and count($sourceFiles) = 1
                and starts-with($sourceFiles[1]/@MIMETYPE,'image/')"
                >Image</xsl:when>

            <!-- File by default based on a single source file unrecognized mimetype -->
            <xsl:when test="not(@TYPE) and count($sourceFiles) = 1">Simple</xsl:when>

            <xsl:when test="@TYPE='Disk'">Disk</xsl:when>

            <!-- Reference is default when the div doesn't contain anything. -->
            <xsl:when test="@TYPE='Reference'">Reference</xsl:when>
            <xsl:when test="@TYPE='Bag'">Bag</xsl:when>
            <xsl:when test="@TYPE='SWORD Object'">Aggregate</xsl:when>
            <xsl:when test="@TYPE='Aggregate Work'">Aggregate</xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">Unrecognized METS div TYPE (<xsl:value-of
                        select="@TYPE"/>)</xsl:message>
            </xsl:otherwise>

        </xsl:choose>
    </xsl:template>

    <!-- Note: FileSystem is the default content model, applied when 
        structure maps do not have a TYPE attribute. -->
    <!-- The FileSystem div template captures processing by content model. -->
    <xsl:template
        match="m:div[ ancestor::m:structMap/@TYPE='Basic' or 
        not(ancestor::m:structMap/@TYPE) or ancestor::m:structMap/@TYPE='LOGICAL' ]"
        mode="contentModel">
        <xsl:param name="assignedSlug" required="no"/>

        <xsl:variable name="role">
            <xsl:apply-templates select="." mode="basic:get-div-role"/>
        </xsl:variable>

        <!-- Role of this div, is it a Container, a File, Image or a Reference -->
        <xsl:choose>

            <xsl:when test="$role='Collection'">
                <xsl:call-template name="basic:Container">
                    <xsl:with-param name="div" select="."/>
                    <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                    <xsl:with-param name="isCollection" select="'yes'"/>
                </xsl:call-template>
            </xsl:when>

            <xsl:when test="$role='Container'">
                <xsl:choose>
                    <xsl:when test="@TYPE='AlphabetizedFolder'">
                        <xsl:call-template name="basic:Container">
                            <xsl:with-param name="div" select="."/>
                            <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                            <xsl:with-param name="sort" select="'alphabetical'"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="basic:Container">
                            <xsl:with-param name="div" select="."/>
                            <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            
            <xsl:when test="$role='Aggregate'">
            	<xsl:call-template name="basic:Container">
                    <xsl:with-param name="div" select="."/>
                    <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                    <xsl:with-param name="isAggregate" select="'yes'"/>
                </xsl:call-template>
            </xsl:when>

            <!-- Reference is default when the div doesn't contain anything. -->
            <xsl:when test="$role='Reference'">
                <xsl:call-template name="basic:Reference">
                    <xsl:with-param name="div" select="."/>
                    <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                </xsl:call-template>
            </xsl:when>

            <!-- Reference is default when the div doesn't contain anything. -->
            <xsl:when test="$role='Bag'">
                <xsl:call-template name="basic:Bag">
                    <xsl:with-param name="div" select="."/>
                    <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                </xsl:call-template>
            </xsl:when>

            <!-- Disk is only used when specified as div TYPE. -->
            <xsl:when test="$role='Disk'">
                <xsl:call-template name="basic:Disk">
                    <xsl:with-param name="div" select="."/>
                    <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                </xsl:call-template>
            </xsl:when>

            <!-- Simple template handles all other single source file types -->
            <xsl:otherwise>
                <xsl:call-template name="basic:Simple">
                    <xsl:with-param name="div" select="."/>
                    <xsl:with-param name="type" select="$role"/>
                    <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
                </xsl:call-template>
            </xsl:otherwise>

        </xsl:choose>
    </xsl:template>

    <!-- FOXML for Containers -->
    <xsl:template name="basic:Container">
        <xsl:param name="div"/>
        <xsl:param name="assignedSlug" required="no"/>
        <xsl:param name="sort" select="'none'"/>
        <xsl:param name="isCollection" required="no" select="false"/>
        <xsl:param name="isAggregate" required="no" select="false"/>

        <!-- output FOXML -->
        <xsl:apply-templates select="$div" mode="common:write-foxml">
            <xsl:with-param name="contentModelType"><xsl:value-of select="$cdr-model"
                />Container</xsl:with-param>
            <xsl:with-param name="div" select="$div"/>
            <xsl:with-param name="slug" select="common:generate-div-slug($div)"/>
            <!-- Default labels are fine for containers. <xsl:with-param name="label"/>-->
            <xsl:with-param name="rdfStatements">
                <xsl:for-each select="$div/m:div">
                    <ir:contains rdf:resource="info:fedora/{common:get-pid(.)}"/>
                </xsl:for-each>
                <xsl:if test="$sort = 'alphabetical'">
                    <ir:sortOrder>alphabetical</ir:sortOrder>
                </xsl:if>
                <xsl:if test="$isCollection eq 'yes'">
                    <fmodel:hasModel>
                        <xsl:attribute name="rdf:resource"><xsl:value-of select="$cdr-model"
                            />Collection</xsl:attribute>
                    </fmodel:hasModel>
                </xsl:if>
                <xsl:if test="$isAggregate eq 'yes'">
                    <fmodel:hasModel>
                        <xsl:attribute name="rdf:resource"><xsl:value-of select="$cdr-model"
                            />AggregateWork</xsl:attribute>
                    </fmodel:hasModel>
                </xsl:if>
            </xsl:with-param>
            <xsl:with-param name="datastreams">
                <!-- Create xml data stream for child order -->
                <xsl:call-template name="make-managed-xml-datastream">
                    <xsl:with-param name="id">MD_CONTENTS</xsl:with-param>
                    <xsl:with-param name="label">List of Contents</xsl:with-param>
                    <xsl:with-param name="xmldata">
                        <m:structMap>
                            <m:div TYPE="Container">
                                <xsl:choose>
                                    <xsl:when test="$sort = 'alphabetical'">
                                        <xsl:for-each select="$div/m:div">
                                            <xsl:sort
                                                select="replace(common:get-label(.),'[^a-zA-Z0-9]+','')"
                                                order="ascending"/>
                                            <m:div ID="{common:get-pid(.)}" ORDER="{position()}"/>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:variable name="maxAbsolute">
                                            <xsl:choose>
                                                <xsl:when
                                                  test="count($div/m:div[exists(@ORDER)]) &gt; 0">
                                                  <xsl:value-of select="max($div/m:div/@ORDER)"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                  <xsl:value-of select="0"/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:variable>
                                        <xsl:for-each select="$div/m:div[exists(@ORDER)]">
                                            <xsl:sort select="@ORDER" order="ascending"/>
                                            <m:div ID="{common:get-pid(.)}" ORDER="{@ORDER}"/>
                                        </xsl:for-each>
                                        <xsl:for-each select="$div/m:div[not(exists(@ORDER))]">
                                            <xsl:sort select="position()" order="ascending"/>
                                            <m:div ID="{common:get-pid(.)}"
                                                ORDER="{$maxAbsolute + position()}"/>
                                        </xsl:for-each>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </m:div>
                        </m:structMap>
                    </xsl:with-param>
                    <xsl:with-param name="versionable" select="'false'"/>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:apply-templates>

        <!-- Recursive processing of child divs -->
        <xsl:apply-templates select="$div/m:div" mode="contentModel"/>
    </xsl:template>

    <!-- Processing step for Bag -->
    <xsl:template name="basic:Bag">
        <xsl:param name="div"/>
        <xsl:param name="assignedSlug" required="no"/>
        <xsl:param name="sort" select="'none'"/>
        <!-- Recursive processing of child divs -->
        <xsl:apply-templates select="$div/m:div" mode="contentModel"/>
    </xsl:template>


    <!-- FOXML for Files -->
    <xsl:template name="basic:Simple">
        <xsl:param name="div"/>
        <xsl:param name="type" required="yes"/>
        <xsl:param name="assignedSlug" required="no"/>
        <!--        <xsl:variable name="label">
            <xsl:apply-templates select="$div" mode="common:generate-file-label"/>
        </xsl:variable>-->

        <!-- get source files -->
        <xsl:variable name="linkedFiles">
            <xsl:apply-templates select="." mode="common:get-files-for-div"/>
        </xsl:variable>

        <!-- FIXME source files are USE=MASTER or no USE specified is okay -->
        <!--<xsl:variable name="useFiles"
            select="/m:mets/m:fileSec/m:fileGrp/m:file[not(@USE) or @USE = 'Master']"/>-->
        <!-- intersection of two sets -->
        <xsl:variable name="sourceFiles" select="$linkedFiles/m:file[not(@USE) or @USE = 'Master']"/>

        <!--        <xsl:message>simple found this many sources: <xsl:value-of select="count($sourceFiles)"></xsl:value-of></xsl:message>-->
        <xsl:variable name="mimetype" select="$sourceFiles[1]/@MIMETYPE"/>

        <!-- output FOXML -->
        <xsl:apply-templates select="$div" mode="common:write-foxml">
            <xsl:with-param name="contentModelType">
                <xsl:value-of select="$cdr-model"/>
                <xsl:value-of select="$type"/>
            </xsl:with-param>
            <xsl:with-param name="div" select="$div"/>
            <xsl:with-param name="slug" select="common:generate-div-slug($div)"/>
            <!--<xsl:with-param name="label" select="$label"/>-->
            <!-- TODO: Default labels are fine for files? <xsl:with-param name="label"/>-->
            <xsl:with-param name="rdfStatements">
                <ir:defaultWebData rdf:resource="info:fedora/{common:get-pid(.)}/DATA_FILE"/>
                <ir:sourceData rdf:resource="info:fedora/{common:get-pid(.)}/DATA_FILE"/>
                <xsl:if test="starts-with($mimetype, 'text/')">
                    <ir:indexText rdf:resource="info:fedora/{common:get-pid(.)}/DATA_FILE"/>
                </xsl:if>
            </xsl:with-param>
            <xsl:with-param name="datastreams">
                <xsl:variable name="url" select="$sourceFiles[1]/m:FLocat/@xlink:href"/>
                <!--<xsl:message>url is: <xsl:value-of select="$url"/></xsl:message>-->
                <xsl:call-template name="make-datastream">
                    <xsl:with-param name="id">DATA_FILE</xsl:with-param>
                    <xsl:with-param name="alt_ids" select="$url"/>
                    <xsl:with-param name="label"
                        select="basic:get-datastream-label($sourceFiles[1], $url)"/>
                    <!-- TODO: how will we know mimetype? -->
                    <xsl:with-param name="mimetype" select="$mimetype"/>
                    <xsl:with-param name="url" select="$url"/>
                    <xsl:with-param name="versionable" select="'true'"/>
                    <!-- TODO add checksum here if available -->
                    <xsl:with-param name="checksum" select="$sourceFiles[1]/@CHECKSUM"/>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>

    <!-- Note: Disk is a METS structure for representing containers that also may have ISO images and 
        other archival disk characteristics. -->
    <xsl:template name="basic:Disk">
        <xsl:param name="assignedSlug" required="no"/>
        <xsl:param name="div" required="yes"/>


        <!-- get associate ISO image file, if any -->
        <xsl:variable name="linkedFiles">
            <xsl:apply-templates select="." mode="common:get-files-for-div"/>
        </xsl:variable>
        <!--<xsl:variable name="linkedFiles" select="key('fileid',$div/m:fptr/@FILEID)"/>-->
        <!--<xsl:variable name="useFiles" select="/m:mets/m:fileSec/m:fileGrp/m:file[@USE='DiskImage']"/>-->
        <xsl:variable name="imageFiles" select="$linkedFiles/m:file[@USE='DiskImage']"/>

        <!-- output FOXML -->
        <xsl:apply-templates select="$div" mode="common:write-foxml">
            <xsl:with-param name="contentModelType"><xsl:value-of select="$cdr-model"
                />Disk</xsl:with-param>
            <xsl:with-param name="div" select="$div"/>
            <xsl:with-param name="slug" select="common:generate-div-slug($div)"/>
            <!-- Default labels are fine for containers. <xsl:with-param name="label"/>-->

            <xsl:with-param name="rdfStatements">
                <xsl:if test="count($div/m:div) &gt; 0">
                    <fmodel:hasModel>
                        <xsl:attribute name="rdf:resource"><xsl:value-of select="$cdr-model"
                            />Container</xsl:attribute>
                    </fmodel:hasModel>
                </xsl:if>
                <xsl:if test="count($imageFiles) = 1">
                    <ir:sourceData rdf:resource="info:fedora/{common:get-pid(.)}/DATA_DISK_IMAGE"/>
                    <ir:diskImageData rdf:resource="info:fedora/{common:get-pid(.)}/DATA_DISK_IMAGE"
                    />
                </xsl:if>
                <xsl:for-each select="$div/m:div">
                    <ir:contains rdf:resource="info:fedora/{common:get-pid(.)}"/>
                </xsl:for-each>
            </xsl:with-param>


            <xsl:with-param name="datastreams">

                <!-- Create xml data stream for child order -->
                <xsl:if test="count($div/m:div) &gt; 0">
                    <xsl:call-template name="make-managed-xml-datastream">
                        <xsl:with-param name="id">MD_CONTENTS</xsl:with-param>
                        <xsl:with-param name="label">List of Contents</xsl:with-param>
                        <xsl:with-param name="xmldata">
                            <m:structMap>
                                <m:div TYPE="Container">
                                    <!-- preserve existing order values when available -->
                                    <xsl:variable name="foo" select="max($div/m:div/@ORDER)"/>
                                    <xsl:for-each select="$div/m:div[exists(@ORDER)]">
                                        <xsl:sort select="@ORDER" order="ascending"/>
                                        <m:div ID="{common:get-pid(.)}" ORDER="{@ORDER}"/>
                                    </xsl:for-each>
                                    <!-- append divs without order, numbers start from last designated order above -->
                                    <xsl:for-each select="$div/m:div[not(exists(@ORDER))]">
                                        <xsl:sort select="position()" order="ascending"/>
                                        <!-- TODO: the order here must start from the last order above -->
                                        <m:div ID="{common:get-pid(.)}" ORDER="{position()}"/>
                                    </xsl:for-each>
                                </m:div>
                            </m:structMap>
                        </xsl:with-param>
                        <xsl:with-param name="versionable" select="'false'"/>
                    </xsl:call-template>
                </xsl:if>

                <!-- create a datastream for the ISO if present -->
                <xsl:if test="count($imageFiles) = 1">
                    <xsl:variable name="mimetype" select="$imageFiles[1]/@MIMETYPE"/>
                    <xsl:variable name="url" select="$imageFiles[1]/m:FLocat/@xlink:href"/>
                    <xsl:call-template name="make-datastream">
                        <xsl:with-param name="id">DATA_DISK_IMAGE</xsl:with-param>
                        <xsl:with-param name="alt_ids" select="$url"/>
                        <xsl:with-param name="label" select="'Disk Image File'"/>
                        <xsl:with-param name="mimetype" select="$mimetype"/>
                        <xsl:with-param name="url" select="$url"/>
                        <xsl:with-param name="versionable" select="'false'"/>
                        <xsl:with-param name="checksum" select="$imageFiles[1]/@CHECKSUM"/>
                    </xsl:call-template>
                </xsl:if>

            </xsl:with-param>
        </xsl:apply-templates>

        <!-- Recursive processing of child divs -->
        <xsl:apply-templates select="$div/m:div" mode="contentModel"/>
    </xsl:template>

    <!-- FOXML delegation for References -->
    <!-- References are children that are defined in a separate structure map -->
    <xsl:template name="basic:Reference">
        <xsl:param name="div"/>
        <xsl:param name="assignedSlug" required="no"/>

        <!-- Use the structLink to find the referent div. -->
        <xsl:variable name="referent"
            select="key( 'divid', substring(key( 'smLinkFrom', concat('#',$div/@ID) )/@xlink:to, 2) )"/>

        <!-- Apply the contentModel template to the referent div. -->
        <xsl:apply-templates select="$referent" mode="contentModel">
            <xsl:with-param name="assignedSlug" select="$assignedSlug"/>
        </xsl:apply-templates>
    </xsl:template>

    <!-- TODO: Generates a label of the form, "Master Image" based on @USE and @MIMETYPE -->
    <!--    <xsl:function name="basic:get-datastream-label">
        <xsl:param name="file"/>
        
    </xsl:function>-->

    <xsl:function name="basic:get-datastream-label">
        <xsl:param name="file"/>
        <xsl:param name="href"/>
        <xsl:choose>
            <xsl:when test="starts-with($href, 'file:') or not(contains($href, ':'))">
                <xsl:choose>
                    <xsl:when test="contains($href, '\')">
                        <xsl:value-of select="replace($href,'(.*\\)*(.+)$','$2')"/>
                    </xsl:when>
                    <xsl:when test="contains($href, '/')">
                        <xsl:value-of select="replace($href,'(.*/)*(.+)$','$2')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$href"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="use" select="common:get-file-use($file)"/>
                <xsl:choose>
                    <xsl:when test="$use = 'MASTER'">Submitted </xsl:when>
                    <xsl:when test="$use = 'SERVICE'"/>
                    <xsl:when test="$use = 'INDEX_TEXT'">Index </xsl:when>
                    <xsl:when test="$use = 'SURROGATE'">Surrogate </xsl:when>
                    <xsl:otherwise/>
                </xsl:choose>
                <xsl:choose>
                    <xsl:when test="starts-with($file/@MIMETYPE,'image') and $use = 'SURROGATE'"
                        >Thumbnail Image</xsl:when>
                    <xsl:when test="contains($file/@MIMETYPE,'pdf')">PDF File</xsl:when>
                    <xsl:when test="contains($file/@MIMETYPE,'ms-powerpoint')">Powerpoint
                        File</xsl:when>
                    <xsl:when test="contains($file/@MIMETYPE,'ms-word')">Word File</xsl:when>
                    <xsl:otherwise>File</xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>


</xsl:stylesheet>
