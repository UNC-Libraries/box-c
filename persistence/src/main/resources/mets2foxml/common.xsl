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
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:m="http://www.loc.gov/METS/" xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:common="http://cdr.unc.edu/common/" xmlns:f="info:fedora/fedora-system:def/foxml#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:private="http://example.com/donotusetheseoutsideofthisxsltdoc"
    xmlns:rels="info:fedora/fedora-system:def/relations-external#"
    xmlns:fmodel="info:fedora/fedora-system:def/model#"
    xmlns:ir="http://cdr.unc.edu/definitions/1.0/base-model.xml#"
    xmlns:acl="http://cdr.unc.edu/definitions/acl" xmlns:premis="info:lc/xmlns/premis-v2"
    xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd"
    exclude-result-prefixes="common">
    <xsl:import href="epdcx2mods.xsl"/>

    <!-- METS to FOXML DOCUMENTATION
        OVERVIEW
        This stylesheet contains common keys, utilities and templates that are useful
        for processing METS and creating FOXML.  It creates an interface between
        content model-specific processing of METS and the general ingest pipeline.
        
        METS conversion starts with the root document template in this file.
        The root template records some PID information and then delegates
        processing of the top METS:div element in the first METS:structMap
        element to content model-specific templates, via the "contentModel" mode.
        
        HOW TO HANDLE DIVS FOR YOUR MODEL
        Content model stylesheets must implement at least one template that matches
        METS:div in mode "contentModel".  These templates should only match METS:div
        elements that are in a structure map with the corresponding content model in
        the TYPE attribute, for example:
        
        <xsl:template match="m:div[ ancestor::m:structMap/@TYPE = 'ScribeBook' ]"
            mode="contentModel">
            
        In this example the content model is specified by the ScribeBook type.
        The "FileSystem" content model is the default and will be applied when
        no TYPE is specified for a structure map.  The "FileSystem" model also 
        has sensible defaults for METS:div elements that have no TYPE, creating
        Containers and Files or References.
        
        MAKE FOXML WITH THE COMMON TEMPLATE
        Each content model captures METS:div element processing to create RDF,
        data streams and object properties.  However, content model templates
        should call the common "common:make-foxml" template to output the end
        product.  If the METS for a content model nests divs within divs then
        parent div processing should also apply the "contentModel" template
        to the child divs.
        
        Parents may pass arbitrary parameters to the child "contentModel" template.
        Useful parameters include a label or slug that give a consistent name to
        children in a certain role.   
        
        STRUCTURES CAN EMBED OBJECTS WITH ARBITRARY CONTENT MODELS
        In some cases a content model structure may contain an object that is from a
        a different content model.  In METS these structure maps contain a 
        "reference" div that is linked to another div by means of a smLink in 
        the structLink element.  The div referred to by the reference div is
        defined in a separate structure map with a different TYPE.
        
        PLACEMENT OF SIP WITHIN PARENT OBJECT
        DEFAULT IS ADD WITHIN Container:  The top-level div in any METS may specify order.  If no order is
            specified, then the div is appended to the list of existing siblings.
            If order is specified, then the div is inserted at that position
            among it's siblings.  These aspects are not handled by this XSL.
        
        ADD WITHIN NON-FILESYSTEM ROLE:  This is a complex case and probably 
            won't come up for some time.  However, in theory the top structure
            map could reflect the parent's content model and consist of only a
            single div, which is a placeholder reference to a div in another
            content model.  This way the parent's model has a chance to process
            the reference (passing a slug or label) before the top object itself
            is processed..  Like I said, this is for future reference.        
        -->

    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:param name="output.directory" required="yes"/>
    <xsl:param name="ownerURI" required="yes"/>

    <xsl:param name="pids" as="node()">
        <pids>
            <pid>test:1</pid>
            <pid>test:2</pid>
            <pid>test:3</pid>
            <pid>test:4</pid>
            <pid>test:5</pid>
            <pid>test:6</pid>
            <pid>test:7</pid>
            <pid>test:8</pid>
            <pid>test:9</pid>
            <pid>test:10</pid>
            <pid>test:11</pid>
            <pid>test:12</pid>
            <pid>test:13</pid>
            <pid>test:14</pid>
            <pid>test:15</pid>
            <pid>test:21</pid>
            <pid>test:22</pid>
            <pid>test:23</pid>
            <pid>test:24</pid>
            <pid>test:25</pid>
            <pid>test:26</pid>
            <pid>test:27</pid>
            <pid>test:28</pid>
            <pid>test:29</pid>
            <pid>test:30</pid>
            <pid>test:31</pid>
            <pid>test:32</pid>
            <pid>test:33</pid>
            <pid>test:34</pid>
            <pid>test:35</pid>
        </pids>
    </xsl:param>

    <xsl:param name="allowAnyIndexing" required="no" select="'yes'"/>

    <!-- TODO: revise the prefix that indicates a URL is a DA pointer -->
    <xsl:variable name="darkArchiveURLStartsWith" select="'file://digitalarchive.lib.unc.edu/'"/>

    <xsl:variable name="cdr-model">info:fedora/cdr-model:</xsl:variable>

    <xsl:variable name="max-label-length" select="200"/>
    <xsl:variable name="max-slug-length" select="50"/>

    <xsl:variable name="predicates">
        <rdf-predicates>
            <ir:hasSurrogate/>
            <ir:hasAlphabeticalOrder/>
            <ir:defaultWebObject/>
            <!--<ir:refersTo/> is not translated to foxml directly. -->
        </rdf-predicates>
    </xsl:variable>

    <!-- KEYS -->
    <xsl:key name="fileid" match="/m:mets/m:fileSec/m:fileGrp/m:file" use="@ID"/>
    <xsl:key name="filegrpid" match="/m:mets/m:fileSec/m:fileGrp" use="@ID"/>
    <xsl:key name="nestedfilegrpid" match="/m:mets/m:fileSec/m:fileGrp/m:fileGrp" use="@ID"/>
    <xsl:key name="fileuse" match="//m:file" use="@USE | parent::m:fileGrp/@USE"/>
    <!--<xsl:key name="fileuse" match="/m:mets/m:fileSec/m:fileGrp/m:file"
        use="@USE | parent::m:fileGrp/@USE"/>-->
    <xsl:key name="dmdid" match="/m:mets/m:dmdSec" use="@ID"/>
    <xsl:key name="amdid" match="/m:mets/m:amdSec/m:*" use="@ID"/>
    <xsl:key name="divid" match="/m:mets/m:structMap//m:div" use="@ID"/>
    <xsl:key name="smLinkFrom" match="/m:mets/m:structLink/m:smLink" use="@xlink:from"/>


    <!-- FUNCTIONS AND VARIABLES -->

    <!-- This variable captures div-pid assignments.  It does not assign pids 
        to divs that are references to other divs, i.e. are in the 
        xlink:from attribute of an smLink.  Use common:get-pid(m:div) to retrieve
        the proper pid for a div, even if it is a reference. -->
    <xsl:variable name="div2pid">
        <xsl:for-each
            select="/m:mets/m:structMap/descendant::m:div[not(@TYPE = 'Reference' or @TYPE = 'Bag')]">
            <xsl:variable name="pos" select="position()"/>
            <xsl:variable name="output" select="concat($output.directory,'/',generate-id(),'.foxml')"/>
            <object>
                <xsl:attribute name="DIVID" select="generate-id()"/>
                <xsl:choose>
                	<xsl:when test="contains(@CONTENTIDS, 'info:fedora/')">
                		<xsl:variable name="pass1" select="substring-after(@CONTENTIDS,'info:fedora/')"/>
                		<xsl:choose>
                			<xsl:when test="contains($pass1,' ')">
                				<xsl:attribute name="PID" select="substring-before($pass1, ' ')"/>
                			</xsl:when>
                			<xsl:otherwise>
                				<xsl:attribute name="PID" select="$pass1"/>
                			</xsl:otherwise>
                		</xsl:choose>
                	</xsl:when>
                	<xsl:otherwise>
                		<xsl:attribute name="PID" select="$pids//pid[position() = $pos]"/>
                	</xsl:otherwise>
                </xsl:choose>
                
                <xsl:attribute name="OUTPUT" select="$output"/>
                <xsl:if test="parent::m:structMap or parent::m:div[@TYPE = 'Bag']">
                    <xsl:attribute name="TOP">yes</xsl:attribute>
                    <xsl:attribute name="sipOrder" select="count(preceding-sibling::m:div) + 1"/>
                    <xsl:if test="@ORDER">
                        <xsl:attribute name="designatedOrder" select="@ORDER"/>
                    </xsl:if>
                </xsl:if>
                <xsl:attribute name="LABEL">
                    <xsl:apply-templates select="." mode="private:generate-label"/>
                </xsl:attribute>
            </object>
        </xsl:for-each>
    </xsl:variable>

    <!-- TODO: How to generate path slugs for filesystem objects -->
    <xsl:function name="common:generate-div-slug">
        <xsl:param name="div"/>
        <xsl:variable name="inputstr">
            <xsl:apply-templates select="$div" mode="private:generate-label"/>
        </xsl:variable>
        <xsl:value-of select="common:generate-slug($inputstr)"/>
    </xsl:function>

    <xsl:function name="common:generate-slug">
        <xsl:param name="inputstr"/>
        <xsl:variable name="pass1" select="replace($inputstr,'[^a-zA-Z0-9_\-\.]','_')"/>
        <xsl:variable name="pass2" select="replace($pass1,'_+','_')"/>
        <xsl:variable name="pass3" select="substring($pass2,0,$max-slug-length)"/>
        <xsl:variable name="pass4" select="replace($pass3,'^_','')"/>
        <xsl:variable name="pass5" select="replace($pass4,'_$','')"/>
        <xsl:value-of select="$pass5"/>
    </xsl:function>

    <xsl:function name="common:get-label">
        <xsl:param name="div"/>
        <xsl:value-of select="$div2pid/object[@DIVID = generate-id($div)]/@LABEL"/>
    </xsl:function>

    <xsl:template match="m:div" mode="common:generate-label">
        <xsl:value-of select="$div2pid/object[@DIVID = generate-id(.)]/@LABEL"/>
    </xsl:template>

    <xsl:template match="m:div" mode="private:generate-label">
        <xsl:variable name="files">
            <xsl:apply-templates select="." mode="common:get-files-for-div"/>
        </xsl:variable>
        <!--<xsl:message>files is <xsl:copy-of select="$files"/></xsl:message>-->
        <xsl:variable name="href" select="$files/m:file[1]/m:FLocat/@xlink:href"/>
        <!--<xsl:message>href is <xsl:value-of select="$href"/></xsl:message>-->
        <xsl:variable name="longLabel">
            <xsl:choose>
                <xsl:when test="@LABEL">
                    <xsl:value-of select="@LABEL"/>
                    <xsl:choose>
                        <xsl:when test="exists(@ORDERLABEL)">
                            <xsl:value-of select="@ORDERLABEL"/>
                        </xsl:when>
                        <!--<xsl:when test="exists(@ORDER)"> <xsl:value-of select="@ORDER"/></xsl:when>-->
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="@DMDID">
                	<xsl:variable name="mdWrap" select="key('dmdid',@DMDID)/m:mdWrap"/>
                	<xsl:choose>
                		<xsl:when test="$mdWrap[@OTHERMDTYPE='EPDCX']">
                			<xsl:call-template name="getEPDCXTitle">
                				<xsl:with-param name="xmlData" select="$mdWrap/m:xmlData"/>
                			</xsl:call-template>
                			<xsl:value-of
                        		select="$mdWrap/m:xmlData/mods:mods/mods:titleInfo/mods:title"/>
                		</xsl:when>
                		<xsl:when test="$mdWrap[@MDTYPE='MODS'] or $mdWrap[not(exists(@MDTYPE))]">
                			<xsl:value-of
                        		select="$mdWrap/m:xmlData/mods:mods/mods:titleInfo/mods:title"/>
                		</xsl:when>
                	</xsl:choose>
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
                    <!-- TODO add file name extraction for irods URLs -->
                </xsl:when>
                <xsl:when test="@ID">
                    <xsl:value-of select="@ID"/>
                </xsl:when>
                <!-- <xsl:when test="@TYPE = 'File'">
                    TODO: grab the file name from the URL
                    if starts with file: or irods: get whatever is after the last \ or /
                    if starts with http: or https: go with "Untitled File 124"
                    </xsl:when> -->
                <xsl:otherwise>
                    <xsl:value-of select="Untitled "/>
                    <xsl:if test="@TYPE">
                        <xsl:value-of select="concat(@TYPE,' ')"/>
                    </xsl:if>
                    <xsl:choose>
                        <xsl:when test="exists(@ORDERLABEL)">
                            <xsl:value-of select="@ORDERLABEL"/>
                        </xsl:when>
                        <!--<xsl:when test="exists(@ORDER)"><xsl:value-of select="@ORDER"/></xsl:when>-->
                        <!--<xsl:otherwise><xsl:value-of select="count(preceding-sibling::m:div)+1"/></xsl:otherwise>-->
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="substring($longLabel,0,$max-label-length)"/>
    </xsl:template>

    <xsl:template mode="private:generate-acl" match="m:div">
        <xsl:param name="allowIndexing" required="yes"/>
        <xsl:variable name="adms" select="key('amdid',@ADMID)"/>
        <xsl:if test="count($adms) &gt; 0">
            <xsl:if test="$adms/m:mdWrap/m:xmlData/acl:accessControl">
                <xsl:variable name="acl" select="$adms/m:mdWrap/m:xmlData/acl:accessControl"/>
                <xsl:if test="$acl/@acl:embargo-until">
                    <xsl:element name="embargo-until"
                        namespace="http://cdr.unc.edu/definitions/acl#"><xsl:attribute
                            name="datatype" namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            >http://www.w3.org/2001/XMLSchema#dateTime</xsl:attribute><xsl:value-of
                            select="$acl/@acl:embargo-until"/>T00:00:00</xsl:element>
                </xsl:if>
                <xsl:if test="$acl/@acl:inherit and ($acl/@acl:inherit eq 'false')">
                    <xsl:element name="inheritPermissions"
                        namespace="http://cdr.unc.edu/definitions/acl#">false</xsl:element>
                    <!--<xsl:attribute name="datatype"
                            namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                            >http://www.w3.org/2001/XMLSchema#boolean</xsl:attribute>-->
                </xsl:if>
                <xsl:for-each select="$acl/acl:grant">
                    <!-- ignore all unrecognized roles!! -->
                    <xsl:if
                        test="@acl:group and string-length(@acl:group) &gt; 0">
                        <xsl:element name="{@acl:role}"
                            namespace="http://cdr.unc.edu/definitions/roles#">
                            <xsl:value-of select="@acl:group"/>
                        </xsl:element>
                    </xsl:if>
                </xsl:for-each>
            </xsl:if>
        </xsl:if>
        <xsl:choose>
            <xsl:when test="$adms/m:mdWrap/m:xmlData/acl:accessControl/@acl:discoverable and ($adms/m:mdWrap/m:xmlData/acl:accessControl/@acl:discoverable eq 'false')">
                <ir:allowIndexing>no</ir:allowIndexing>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$allowIndexing">
                        <ir:allowIndexing>yes</ir:allowIndexing>
                    </xsl:when>
                    <xsl:otherwise>
                        <ir:allowIndexing>no</ir:allowIndexing>
                    </xsl:otherwise>
                </xsl:choose>                
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="$adms/m:mdWrap/m:xmlData/acl:accessControl/@acl:published and ($adms/m:mdWrap/m:xmlData/acl:accessControl/@acl:published eq 'false')">
        	<ir:isPublished>no</ir:isPublished>
        </xsl:if>
    </xsl:template>

    <!--<xsl:variable name="defaultContentModel" select="'FileSystem'"/>-->

    <!-- Returns the PID for a given div, if it is a smLink'ed div, then
        it returns the pid for the single object these divs represent. -->
    <xsl:function name="common:get-pid">
        <xsl:param name="div"/>
        <!-- find real div reference first through smLink -->
        <xsl:variable name="realdivID">
            <xsl:choose>
                <!-- this a smLinked div -->
                <xsl:when test="$div/@TYPE = 'Reference'">
                    <xsl:variable name="smLinkToID"
                        select="$div/ancestor::m:mets/m:structLink/m:smLink[@xlink:arcrole eq 'http://cdr.unc.edu/definitions/1.0/base-model.xml#refersTo' and @xlink:from eq concat('#',$div/@ID)]/@xlink:to"/>
                    <xsl:value-of
                        select="generate-id($div/ancestor::m:mets/m:structMap/m:div[@ID eq substring($smLinkToID,2)])"
                    />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="generate-id($div)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="$div2pid/object[@DIVID eq $realdivID]/@PID"/>
    </xsl:function>

    <xsl:function name="common:get-pid-resource">
        <xsl:param name="div"/>info:fedora/<xsl:value-of select="common:get-pid($div)"/>
    </xsl:function>

    <xsl:function name="common:get-output">
        <xsl:param name="div"/>
        <xsl:value-of select="$div2pid/object[@DIVID eq generate-id($div)]/@OUTPUT"/>
    </xsl:function>

    <xsl:function name="common:get-datastream-id">
        <xsl:param name="file"/>
        <xsl:param name="prefix"/>
        <xsl:variable name="urlparts" select="tokenize($file/m:FLocat/@xlink:href,'/')"/>
        <xsl:value-of select="concat($prefix,$urlparts[position() = last()])"/>
    </xsl:function>

    <xsl:function name="common:get-datastream-resource">
        <xsl:param name="div"/>
        <xsl:param name="file"/>
        <xsl:param name="prefix"/>
        <xsl:variable name="dsid" select="common:get-datastream-id($file, $prefix)"/>
        <xsl:value-of select="concat('info:fedora/',common:get-pid($div),'/',$dsid)"/>
    </xsl:function>

    <!--    <xsl:function name="common:get-object-slug">
        <xsl:param name="div"/>
        <xsl:choose>
            <xsl:when test="exists($div/@ID)">
                <xsl:value-of select="$div/@ID"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="generate-id($div)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>-->

    <xsl:function name="common:get-file-use">
        <xsl:param name="file"/>
        <xsl:choose>
            <xsl:when test="exists($file/@USE)">
                <xsl:value-of select="$file/@USE"/>
            </xsl:when>
            <xsl:when test="exists($file/parent::m:fileGrp/@USE)">
                <xsl:value-of select="$file/parent::m:fileGrp/@USE"/>
            </xsl:when>
            <xsl:otherwise>MASTER</xsl:otherwise>
        </xsl:choose>
    </xsl:function>


    <!-- START OF TEMPLATES -->

    <xsl:template match="/">
        <xsl:variable name="topMap" select="/m:mets/m:structMap[1]"/>
        <xsl:variable name="topDiv" select="$topMap/m:div[1]"/>

        <!-- delegate divs to content model processing -->
        <xsl:apply-templates select="$topDiv" mode="contentModel"/>

        <result>
            <!-- Add type and model of top div (the slot) -->
            <!-- TODO: might delegate generating this block to content models -->
            <container>
                <xsl:if test="exists($topDiv/@ORDER)">
                    <xsl:attribute name="order" select="$topDiv/@ORDER"/>
                </xsl:if>
                <!-- an ir:contains is already assumed here -->
            </container>
            <objects>
                <xsl:copy-of select="$div2pid"/>
            </objects>
        </result>
    </xsl:template>

    <xsl:template match="m:div" mode="contentModel">
        <!-- <xsl:message terminate="yes" xml:space="preserve">
            No content model support for this type of structMap and div:
                structMap/@TYPE = '<xsl:value-of select="ancestor::m:structMap/@TYPE"/>'
                div/@TYPE = '<xsl:value-of select="@TYPE"/>'
        </xsl:message> -->
    </xsl:template>

    <xsl:template match="m:div" mode="common:get-files-by-use">
        <xsl:param name="uses"/>
        <xsl:variable name="linkedFiles">
            <xsl:apply-templates select="." mode="common:get-files-for-div"/>
        </xsl:variable>
        <xsl:variable name="useFiles" select="key('fileuse',$uses/file/@USE)"/>
        <!-- The following XPath does an intersection of the two sets. -->
        <xsl:copy-of select="$linkedFiles[ count(.|$useFiles) = count($useFiles) ]"/>
    </xsl:template>

    <xsl:template match="m:div" mode="common:get-files-for-div">
        <xsl:variable name="fileMatches" select="key('fileid',m:fptr/@FILEID)"/>
        <xsl:variable name="fileGrpMatch" select="key('filegrpid',m:fptr/@FILEID)/m:file"/>
        <xsl:variable name="nestedFileGrpMatch"
            select="key('nestedfilegrpid',m:fptr/@FILEID)/m:file"/>
        <xsl:variable name="files"
            select="insert-before($fileMatches, 0, insert-before($fileGrpMatch, 0, $nestedFileGrpMatch))"/>
        <xsl:copy-of select="$files"/>
    </xsl:template>


    <!-- Template for FOXML output -->
    <xsl:template match="m:div" mode="common:write-foxml">
        <xsl:param name="contentModelType" required="yes"/>
        <xsl:param name="slug" required="yes"/>
        <xsl:param name="allowIndexing" required="no" select="$allowAnyIndexing"/>

        <!-- These parameters have suitable defaults. -->
        <xsl:param name="label" select="''"/>
        <xsl:param name="rdfStatements"/>
        <xsl:param name="datastreams"/>

        <xsl:result-document href="{common:get-output(.)}">
            <f:digitalObject>
                <xsl:attribute name="VERSION" select="'1.1'"/>
                <xsl:attribute name="PID" select="common:get-pid(.)"/>
                <f:objectProperties>
                    <f:property NAME="info:fedora/fedora-system:def/model#state" VALUE="Active"/>
                    <!-- <f:property NAME="info:fedora/fedora-system:def/model#ownerId" VALUE="{$owner}"/> -->
                    <f:property NAME="info:fedora/fedora-system:def/model#label">
                        <xsl:attribute name="VALUE">
                            <xsl:choose>
                                <xsl:when test="string-length($label) = 0">
                                    <xsl:value-of select="common:get-label(.)"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="substring($label,0,$max-label-length)"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                    </f:property>

                    <!-- TODO: This model property should take content model PID, which we don't 
                        have yet.  We probably need a triple store lookup to resolve CM PIDs.
                        Make an ingest filter. -->
                    <!--<f:property NAME="info:fedora/fedora-system:def/model#contentModel">
                        <xsl:attribute name="VALUE" select="$contentModel"/>
                    </f:property>-->
                </f:objectProperties>

                <xsl:variable name="allRdfStatements">                    
                    <ir:slug>
                        <xsl:value-of select="$slug"/>
                    </ir:slug>
                    <xsl:copy-of select="$rdfStatements"/>
                    <xsl:apply-templates select="." mode="private:generate-acl">
                        <xsl:with-param name="allowIndexing" select="$allowIndexing"/>
                    </xsl:apply-templates>
                </xsl:variable>

                <!--<xsl:message>all rdf: <xsl:copy-of select="$allRdfStatements"/></xsl:message>-->

                <!-- Create RELS-EXT datastream -->
                <xsl:call-template name="rels-ext-datastream">
                    <xsl:with-param name="div" select="."/>
                    <xsl:with-param name="contentModelType" select="$contentModelType"/>
                    <xsl:with-param name="statements" select="$allRdfStatements"/>
                </xsl:call-template>

                <!-- Create MD_ADMINISTRATIVE if needed -->
                <xsl:apply-templates select="." mode="MD_ADMINISTRATIVE"/>

                <!-- Create MD_DESCRIPTIVE if needed -->
                <xsl:apply-templates select="." mode="MD_DESCRIPTIVE"/>

                <!-- Note: MD_EVENTS will only be created by ingest processing. -->

                <xsl:copy-of select="$datastreams"/>
            </f:digitalObject>
        </xsl:result-document>
    </xsl:template>


    <!-- RELS-EXT DATASTREAM PROCESSING -->
    <!-- Adds general RELS-EXT relationships as well as passed statements -->
    <xsl:template name="rels-ext-datastream">
        <xsl:param name="div" required="yes"/>
        <xsl:param name="contentModelType" required="yes"/>
        <xsl:param name="statements"/>
        <f:datastream ID="RELS-EXT" CONTROL_GROUP="M" VERSIONABLE="false">
            <f:datastreamVersion MIMETYPE="text/xml"
                LABEL="Fedora Object-to-Object Relationship Metadata">
                <xsl:attribute name="ID">RELS-EXT.0</xsl:attribute>
                <f:contentDigest DIGEST="none" TYPE="MD5"/>
                <f:xmlContent>
                    <rdf:RDF>
                        <rdf:Description>
                            <xsl:attribute name="rdf:about" select="common:get-pid-resource($div)"/>
                            <ir:owner rdf:resource="{$ownerURI}"/>
                            <fmodel:hasModel rdf:resource="{$contentModelType}"/>
                            <xsl:copy-of select="$statements"/>
                            <!-- Add structLinks with xlink arc pattern -->
                            <xsl:for-each
                                select="$div/ancestor::m:mets/m:structLink/m:smLink[@xlink:from eq concat('#',$div/@ID)]">
                                <xsl:message select="."/>
                                <xsl:variable name="predicate" select="string(./@xlink:arcrole)"/>
                                <xsl:if
                                    test="$predicates/rdf-predicates/*[concat(string(namespace-uri()), local-name()) eq $predicate]">
                                    <xsl:variable name="pred"
                                        select="$predicates/rdf-predicates/*[concat(string(namespace-uri()), local-name()) eq $predicate]"/>
                                    <!--<xsl:message select="$pred"/>-->
                                    <xsl:variable name="ns" select="string(namespace-uri($pred))"/>
                                    <xsl:variable name="lname" select="local-name($pred)"/>
                                    <xsl:variable name="objectPID"
                                        select="common:get-pid(key('divid',substring(./@xlink:to,2)))"/>
                                        <xsl:choose>
                                            <xsl:when test="$lname eq 'hasAlphabeticalOrder'">
                                                <xsl:element namespace="{$ns}" name="sortOrder">
                                                    <xsl:attribute name="resource"
                                                        namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                                        select="concat($ns,'alphabetical')"/>
                                                </xsl:element>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:element namespace="{$ns}" name="{$lname}">
                                                    <xsl:attribute name="resource"
                                                        namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                                        select="concat('info:fedora/',$objectPID)"/>
                                                </xsl:element>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                </xsl:if>
                            </xsl:for-each>
                        </rdf:Description>
                    </rdf:RDF>
                </f:xmlContent>
            </f:datastreamVersion>
        </f:datastream>
    </xsl:template>

    <!-- Administrative metadata processing common to all divs -->
    <xsl:template match="m:div" mode="MD_ADMINISTRATIVE">
        <!-- Try to find administrative metadata -->
        <xsl:variable name="admin" select="key('amdid',@ADMID)/m:digiprovMD"/>
        <xsl:choose>
            <xsl:when test="exists($admin/m:mdWrap/m:xmlData)">
                <f:datastream ID="MD_ADMINISTRATIVE" STATE="A" CONTROL_GROUP="M" VERSIONABLE="true">
                    <f:datastreamVersion LABEL="Administrative Metadata" MIMETYPE="text/xml"
                        ID="MD_ADMINISTRATIVE.0">
                        <f:contentDigest DIGEST="none" TYPE="MD5"/>
                        <f:xmlContent>
                            <xsl:copy-of select="$admin/m:mdWrap/m:xmlData/*"/>
                        </f:xmlContent>
                    </f:datastreamVersion>
                </f:datastream>
            </xsl:when>
            <xsl:when test="exists($admin/m:mdRef/@xlink:href)">
                <f:datastream ID="MD_ADMINISTRATIVE" STATE="A" CONTROL_GROUP="M" VERSIONABLE="true">
                    <f:datastreamVersion LABEL="Administrative Metadata" MIMETYPE="text/xml"
                        ID="MD_ADMINISTRATIVE.0">
                        <f:contentDigest DIGEST="none" TYPE="MD5"/>
                        <f:contentLocation REF="{$admin/m:mdRef/@xlink:href}" TYPE="URL"/>
                    </f:datastreamVersion>
                </f:datastream>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- Adds descriptive metadata common to all described objects -->
    <xsl:template match="m:div" mode="MD_DESCRIPTIVE">
        <xsl:if test="exists(@DMDID)">
            <f:datastream ID="MD_DESCRIPTIVE" STATE="A" CONTROL_GROUP="M" VERSIONABLE="true">
                <f:datastreamVersion LABEL="Descriptive Metadata" MIMETYPE="text/xml"
                    ID="MD_DESCRIPTIVE.0">
                    <f:contentDigest DIGEST="none" TYPE="MD5"/>
                    <xsl:variable name="dmdSec" select="/m:mets/m:dmdSec[@ID eq current()/@DMDID]"/>
                    <xsl:choose>
                        <xsl:when test="exists($dmdSec/m:mdWrap)">
                            <f:xmlContent>
                                <xsl:choose>
                                    <xsl:when test="$dmdSec/m:mdWrap[@MDTYPE='MODS']">
                                        <xsl:copy-of select="$dmdSec/m:mdWrap/m:xmlData/*"/>
                                    </xsl:when>
                                    <xsl:when test="$dmdSec/m:mdWrap[@OTHERMDTYPE='EPDCX']">
                                        <!-- Transform eprint dc to mods -->
                                        <xsl:call-template name="epdcx2mods">
                                            <xsl:with-param name="xmlData" select="$dmdSec/m:mdWrap[@OTHERMDTYPE='EPDCX']/m:xmlData"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:copy-of select="$dmdSec/m:mdWrap/m:xmlData/*"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </f:xmlContent>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:variable name="url" select="$dmdSec/m:mdRef/@xlink:href"/>
                            <f:contentLocation REF="{$url}" TYPE="URL"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </f:datastreamVersion>
            </f:datastream>
        </xsl:if>
    </xsl:template>

    <!-- TODO: need better datastreamVersion ids or omit them -->
    <!-- Adds arbitrary xml data to a named datastream -->
    <xsl:template name="make-managed-xml-datastream">
        <xsl:param name="xmldata" required="yes"/>
        <xsl:param name="label" required="yes"/>
        <xsl:param name="id" required="yes"/>
        <xsl:param name="versionable" required="yes"/>
        <f:datastream ID="{$id}" STATE="A" CONTROL_GROUP="M" VERSIONABLE="{$versionable}">
            <f:datastreamVersion LABEL="{$label}" MIMETYPE="text/xml" ID="{$id}.0">
                <f:contentDigest DIGEST="none" TYPE="MD5"/>
                <f:xmlContent>
                    <xsl:copy-of select="$xmldata"/>
                </f:xmlContent>
            </f:datastreamVersion>
        </f:datastream>
    </xsl:template>
    
    

    <!-- Adds arbitrary xml data to a named datastream -->
    <xsl:template name="make-url-xml-datastream">
        <xsl:param name="url" required="yes"/>
        <xsl:param name="label" required="yes"/>
        <xsl:param name="id" required="yes"/>
        <xsl:param name="versionable" required="yes"/>
        <f:datastream ID="{$id}" STATE="A" CONTROL_GROUP="M" VERSIONABLE="{$versionable}">
            <f:datastreamVersion LABEL="{$label}" MIMETYPE="text/xml" ID="{$id}.0">
                <f:contentDigest DIGEST="none" TYPE="MD5"/>
                <f:contentLocation REF="{$url}" TYPE="URL"/>
            </f:datastreamVersion>
        </f:datastream>
    </xsl:template>

    <!-- TODO: need better datastreamVersion ids or omit them -->
    <!-- makes a Fedora managed (binary) datastream, href-style -->
    <xsl:template name="make-datastream">
        <xsl:param name="id" required="yes"/>
        <xsl:param name="alt_ids" required="no" select="''"/>
        <xsl:param name="label" required="yes"/>
        <xsl:param name="mimetype" required="yes"/>
        <xsl:param name="url" required="yes"/>
        <xsl:param name="versionable" required="yes"/>
        <xsl:param name="checksum" required="no"/>
        <xsl:variable name="controlGroup">
            <xsl:choose>
                <!-- file is in the dark archive -->
                <xsl:when test="starts-with($url,
                    $darkArchiveURLStartsWith)"
                    >E</xsl:when>
                <!-- file is local to the SIP -->
                <xsl:when test="starts-with($url,
                    'file:///')">M</xsl:when>
                <xsl:otherwise>M</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <f:datastream ID="{$id}" STATE="A" CONTROL_GROUP="{$controlGroup}"
            VERSIONABLE="{$versionable}">
            <xsl:if test="$controlGroup = 'E'">
                <!-- TODO support capture of original locations in FLocat USE ORIGINAL -->
                <xsl:attribute name="ALT_IDS" select="$url"/>
            </xsl:if>
            <f:datastreamVersion LABEL="{$label}" MIMETYPE="{$mimetype}" ID="{$id}.0"
                ALT_IDS="{$alt_ids}">
                <xsl:choose>
                    <!-- FIXME: debug server side issues with supplying checksums for irods staged files -->
                    <xsl:when test="exists($checksum) and string-length($checksum) &gt; 0">
                        <f:contentDigest DIGEST="{$checksum}" TYPE="MD5"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <f:contentDigest DIGEST="none" TYPE="MD5"/>
                    </xsl:otherwise>
                </xsl:choose>
                <f:contentLocation REF="{$url}" TYPE="URL"/>
            </f:datastreamVersion>
        </f:datastream>
    </xsl:template>

    <!-- 
    <xsl:template match="m:div[@TYPE eq 'Simple']" mode="add-content-model-relationships">
        <xsl:choose>
            <xsl:when test="parent::m:div/@TYPE = 'Collection'">
                <rels:isMemberOf>
                    <xsl:attribute name="rdf:resource" select="common:get-pid-resource(parent::m:div)"
                    />
                </rels:isMemberOf>
            </xsl:when>
            <xsl:when test="parent::m:div/@TYPE = 'Container'">
                <rels:isMemberOf>
                    <xsl:attribute name="rdf:resource" select="common:get-pid-resource(parent::m:div)"
                    />
                </rels:isMemberOf>
            </xsl:when>
            <xsl:otherwise/>
        </xsl:choose>

        <xsl:variable name="linkedFiles" select="key('fileid',m:fptr/@FILEID)"/>

        <xsl:for-each select="m:fptr">
            <xsl:variable name="file" select="key('fileid',@FILEID)"/>
            <xsl:variable name="use">
                <xsl:choose>
                    <xsl:when test="exists($file/@USE)">
                        <xsl:value-of select="$file/@USE"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$file/parent::m:fileGrp/@USE"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$use eq 'MASTER'">
                    <ir:hasMasterFile>
                        <xsl:attribute name="rdf:resource"
                            select="common:get-datastream-resource(parent::m:div, $file,'IN_')"/>
                    </ir:hasMasterFile>
                </xsl:when>
                <xsl:when test="$use eq 'SERVICE'">
                    <ir:hasServiceFile>
                        <xsl:attribute name="rdf:resource"
                            select="common:get-datastream-resource(parent::m:div, $file,'IN_')"/>
                    </ir:hasServiceFile>
                </xsl:when>
                <xsl:when test="$use eq 'SURROGATE'">
                    <ir:hasSurrogate>
                        <xsl:attribute name="rdf:resource"
                            select="common:get-datastream-resource(parent::m:div, $file,'IN_')"/>
                    </ir:hasSurrogate>
                    <xsl:if test="starts-with($file/@MIMETYPE, 'image')">
                        <ir:hasThumbnail>
                            <xsl:attribute name="rdf:resource"
                                select="common:get-datastream-resource(parent::m:div, $file,'IN_')"/>
                        </ir:hasThumbnail>
                    </xsl:if>
                </xsl:when>
                <xsl:when test="$use eq 'INDEX_TEXT'">
                    <ir:hasIndexText>
                        <xsl:attribute name="rdf:resource"
                            select="common:get-datastream-resource(parent::m:div, $file,'IN_')"/>
                    </ir:hasIndexText>
                </xsl:when>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

Datastream processing specific to the Container content models 
<xsl:template match="m:div[@TYPE eq 'Collection' or @TYPE eq 'Container']"
        mode="content-model-datastreams">
        <xsl:apply-templates select="." mode="described-object-datastreams"/>
        <xsl:variable name="containerUses">
            <file USE="SURROGATE"/>
        </xsl:variable>
        <xsl:variable name="linkedFiles" select="key('fileid',m:fptr/@FILEID)"/>
        <xsl:variable name="useFiles" select="key('fileuse',$containerUses/file/@USE)"/>-->
    <!-- The following select does an intersection of the two sets.
        <xsl:apply-templates select="$linkedFiles[ count(.|$useFiles) = count($useFiles) ]"
            mode="file-to-datastream"/>
    </xsl:template>
        -->

</xsl:transform>
