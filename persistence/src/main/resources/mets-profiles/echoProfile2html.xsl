<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:p="http://www.loc.gov/METS_Profile/" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="p xs" version="2.0">

	<xsl:import href="xmlverbatim_mets.xsl"/>

	<xs:annotation>
		<xs:documentation> Creator: Thomas Habing, Grainger Engineering Library Information Center,
			University of Illinois at Urbana-Champaign. thabing@uiuc.edu Date: 2006-04-28
			Description: Transform XML METS Profiles into HTML for viewing in a web browser. This
			style sheet has been tested with both METS_Profile documents for version 1.1 and 1.2 of
			the schema. The Appendix is not rendered by this stylesheet. History: None so far.
		</xs:documentation>
	</xs:annotation>

	<xsl:output method="html" indent="no" doctype-public="-//W3C//DTD HTML 4.01//EN"
		doctype-system="HTTP://WWW.W3.ORG/TR/HTML4/STRICT.DTD"/>

	<xsl:param name="MAX_LINK_LEN" select="50"/>

	<xsl:template match="/">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="p:METS_Profile|METS_Profile">

		<html>
			<head>
				<title>
					<xsl:value-of select="p:title|title"/>
				</title>
				<link rel="stylesheet" type="text/css" href="profile2html.css"/>
				<script type="text/javascript" src="profile2html.js"/>
			</head>
			<body style="margin:2em">
				<h1>
					<xsl:apply-templates select="p:title/@ID|title/@ID"/>
					<xsl:value-of select="p:title|title"/>
				</h1>
				<ul style="border:1px solid black">
					<li>
						<a href="#TOC_Abstract">Abstract</a>
					</li>
					<li>
						<a href="#TOC_Related_Profiles">Related Profiles</a>
					</li>
					<li>
						<a href="#TOC_Extension_Schema">Extension Schema</a>
					</li>
					<li>
						<a href="#TOC_Description_Rules">Description Rules</a>
					</li>
					<li>
						<a href="#TOC_Controlled_Vocabularies">Controlled Vocabularies</a>
					</li>
					<li>
						<a href="#TOC_Structural_Requirements">Structural Requirements</a>
						<ul>
							<li>
								<a href="#TOC_METS_Root_Element">METS Root Element</a>
							</li>
							<li>
								<a href="#TOC_METS_Header">METS Header</a>
							</li>
							<li>
								<a href="#TOC_Description_Metadata_Section">Description Metadata
									Section</a>
							</li>
							<li>
								<a href="#TOC_Administrative_Metadata_Section">Administrative
									Metadata Section</a>
							</li>
							<li>
								<a href="#TOC_Content_File_Section">Content File Section</a>
							</li>
							<li>
								<a href="#TOC_Structural_Map">Structural Map</a>
							</li>
							<li>
								<a href="#TOC_Structural_Map_Linking">Structural Map Linking</a>
							</li>
							<li>
								<a href="#TOC_Behavior_Section">Behavior Section</a>
							</li>
							<li>
								<a href="#TOC_Multiple_Sections">Multiple Sections</a>
							</li>
						</ul>
					</li>
					<li>
						<a href="#TOC_Technical_Requirements">Technical Requirements</a>
						<ul>
							<li>
								<a href="#TOC_Content_Files">Content Files</a>
							</li>
							<li>
								<a href="#TOC_Behavior_Files">Behavior Files</a>
							</li>
							<li>
								<a href="#TOC_Metadata_Files">Metadata Files</a>
							</li>
						</ul>
					</li>
					<li>
						<a href="#TOC_Tools">Tools</a>
					</li>
					<li>
						<a href="#TOC_Appendices">Appendices</a>
						<ul>
							<xsl:for-each select="p:Appendix|Appendix">
								<li>
									<a><xsl:attribute name="href">#APPENDIX_<xsl:value-of
												select="@NUMBER"/></xsl:attribute>Appendix
											<xsl:value-of select="@NUMBER"/><xsl:if test="@LABEL">
											<xsl:value-of select="': '"/>
											<xsl:value-of select="@LABEL"/>
										</xsl:if></a>
								</li>
							</xsl:for-each>
						</ul>
					</li>

				</ul>
				<h2>
					<xsl:apply-templates select="p:URI/@ID|URI/@ID"/>
					<xsl:value-of select="p:URI/@LOCTYPE|URI/@LOCTYPE"/>: <a>
						<xsl:if
							test="p:URI/@LOCTYPE='URL' or p:URI/@LOCTYPE='PURL' or URI/@LOCTYPE='URL' or URI/@LOCTYPE='PURL'">
							<xsl:attribute name="href">
								<xsl:value-of select="p:URI|URI"/>
							</xsl:attribute>
						</xsl:if>
						<xsl:value-of select="p:URI|URI"/>
					</a>
				</h2>
				<h2>
					<xsl:apply-templates select="p:date/@ID|date/@ID"/>
					<xsl:value-of select="p:date|date"/>
				</h2>
				<h2><a name="TOC_Abstract">Abstract:</a></h2>
				<p>
					<xsl:apply-templates select="p:abstract/@ID|abstract/@ID"/>
					<xsl:value-of select="p:abstract|abstract"/>
				</p>
				<xsl:apply-templates select="p:contact|contact"/>
				<xsl:if test="p:registration_info|registration_info">
					<dl>
						<dt>
							<xsl:apply-templates
								select="p:registration_info/@ID|registration_info/@ID"/>
							<big>
								<b>Registered:</b>
							</big>
						</dt>
						<dd>
							<xsl:value-of
								select="p:registration_info/@regDate|registration_info/@regDate"/>
						</dd>
						<dd>
							<a>
								<xsl:attribute name="href">
									<xsl:value-of
										select="p:registration_info/@regURI|registration_info/@regURI"
									/>
								</xsl:attribute>
								<xsl:value-of
									select="p:registration_info/@regURI|registration_info/@regURI"/>
							</a>
						</dd>
					</dl>
				</xsl:if>
				<dl>
					<dt>
						<big>
							<b>
								<a name="TOC_Related_Profiles">Related Profiles:</a>
							</b>
						</big>
					</dt>
					<xsl:for-each select="p:related_profile|related_profile">
						<dd>
							<xsl:apply-templates select="@ID"/>
							<xsl:value-of select="@RELATIONSHIP"/>
							<xsl:if test="@RELATIONSHIP">
								<xsl:text>: </xsl:text>
							</xsl:if>
							<xsl:apply-templates/>
							<xsl:if test="@URI">
								<xsl:text> (</xsl:text>
								<a>
									<xsl:attribute name="href">
										<xsl:value-of select="@URI"/>
									</xsl:attribute>
									<xsl:value-of select="@URI"/>
								</a>
								<xsl:text>) </xsl:text>
							</xsl:if>
						</dd>
					</xsl:for-each>
				</dl>
				<dl>
					<dt>
						<big>
							<b>
								<a name="TOC_Extension_Schema">Extension Schema:</a>
							</b>
						</big>
					</dt>
					<xsl:for-each select="p:extension_schema|extension_schema">
						<dd>
							<xsl:apply-templates select="."/>
						</dd>
					</xsl:for-each>
				</dl>
				<dl>
					<dt>
						<big>
							<b>
								<a name="TOC_Description_Rules">Description Rules:</a>
							</b>
						</big>
					</dt>
					<dd>
						<xsl:apply-templates select="p:description_rules|description_rules"/>
					</dd>
				</dl>
				<dl>
					<dt>
						<xsl:apply-templates
							select="p:controlled_vocabularies/@ID|controlled_vocabularies/@ID"/>
						<big>
							<b>
								<a name="TOC_Controlled_Vocabularies">Controlled Vocabularies:</a>
							</b>
						</big>
					</dt>
					<xsl:for-each
						select="p:controlled_vocabularies/p:vocabulary|controlled_vocabularies/vocabulary">
						<dd>
							<xsl:apply-templates select="."/>
						</dd>
					</xsl:for-each>
				</dl>
				<dl>
					<dt>
						<xsl:apply-templates
							select="p:structural_requirements/@ID|structural_requirements/@ID"/>
						<big>
							<b>
								<a name="TOC_Structural_Requirements">Structural Requirements:</a>
							</b>
						</big>
					</dt>
					<dd>
						<dl>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:metsRootElement/@ID|structural_requirements/metsRootElement/@ID"/>
								<b>
									<a name="TOC_METS_Root_Element">METS Root Element</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:metsRootElement|structural_requirements/metsRootElement"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:metsHdr/@ID|structural_requirements/metsHdr/@ID"/>
								<b>
									<a name="TOC_METS_Header">METS Header</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:metsHdr|structural_requirements/metsHdr"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:dmdSec/@ID|structural_requirements/dmdSec/@ID"/>
								<b>
									<a name="TOC_Description_Metadata_Section">Description Metadata
										Section</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:dmdSec|structural_requirements/dmdSec"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:amdSec/@ID|structural_requirements/amdSec/@ID"/>
								<b>
									<a name="TOC_Administrative_Metadata_Section">Administrative
										Metadata Section</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:amdSec|structural_requirements/amdSec"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:fileSec/@ID|structural_requirements/fileSec/@ID"/>
								<b>
									<a name="TOC_Content_File_Section">Content File Section</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:fileSec|structural_requirements/fileSec"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:structMap/@ID|structural_requirements/structMap/@ID"/>
								<b>
									<a name="TOC_Structural_Map">Structural Map</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:structMap|structural_requirements/structMap"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:structLink/@ID|structural_requirements/structLink/@ID"/>
								<b>
									<a name="TOC_Structural_Map_Linking">Structural Map Linking</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:structLink|structural_requirements/structLink"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:behaviorSec/@ID|structural_requirements/behaviorSec/@ID"/>
								<b>
									<a name="TOC_Behavior_Section">Behavior Section</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:behaviorSec|structural_requirements/behaviorSec"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:structural_requirements/p:multiSection/@ID|structural_requirements/multiSection/@ID"/>
								<b>
									<a name="TOC_Multiple_Sections">Multiple Sections</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:structural_requirements/p:multiSection|structural_requirements/multiSection"
								/>
							</dd>
						</dl>
					</dd>
				</dl>
				<dl>
					<dt>
						<xsl:apply-templates
							select="p:technical_requirements/@ID|technical_requirements/@ID"/>
						<big>
							<b>
								<a name="TOC_Technical_Requirements">Technical Requirements:</a>
							</b>
						</big>
					</dt>
					<dd>
						<dl>
							<dt>
								<xsl:apply-templates
									select="p:technical_requirements/p:content_files/@ID|technical_requirements/content_files/@ID"/>
								<b>
									<a name="TOC_Content_Files">Content Files</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:technical_requirements/p:content_files|technical_requirements/content_files"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:technical_requirements/p:behavior_files/@ID|technical_requirements/behavior_files/@ID"/>
								<b>
									<a name="TOC_Behavior_Files">Behavior Files</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:technical_requirements/p:behavior_files|technical_requirements/behavior_files"
								/>
							</dd>
							<dt>
								<xsl:apply-templates
									select="p:technical_requirements/p:metadata_files/@ID|technical_requirements/metadata_files/@ID"/>
								<b>
									<a name="TOC_Metadata_Files">Metadata Files</a>
								</b>
							</dt>
							<dd>
								<xsl:apply-templates
									select="p:technical_requirements/p:metadata_files|technical_requirements/metadata_files"
								/>
							</dd>
						</dl>
					</dd>
				</dl>
				<dl>
					<dt>
						<big>
							<b>
								<a name="TOC_Tools">Tools:</a>
							</b>
						</big>
					</dt>
					<xsl:for-each select="p:tool|tool">
						<dd>
							<xsl:apply-templates select="."/>
						</dd>
					</xsl:for-each>
				</dl>
				<dl>
					<dt>
						<a name="appendices"/>
						<big>
							<b>
								<a name="TOC_Appendices">Appendices :</a>
							</b>
						</big>
					</dt>
					<dd>
						<xsl:apply-templates select="p:Appendix|Appendix"/>
					</dd>
				</dl>
			</body>
		</html>

	</xsl:template>

	<xsl:template match="p:contact|contact">
		<xsl:apply-templates select="@ID"/>
		<address>
  		<xsl:for-each select="*">
  			<xsl:apply-templates select="@ID"/><xsl:apply-templates select="."/><br/>
  		</xsl:for-each>
  	</address>
	</xsl:template>

	<xsl:template match="p:tool|tool">
		<xsl:apply-templates select="@ID"/>
		<xsl:if test="p:name|name">
			<xsl:apply-templates select="p:name/@ID|name/@ID"/>
			<b>
				<xsl:apply-templates select="p:name|name"/>
			</b>
			<br/>
		</xsl:if>
		<xsl:if test="p:agency|agency">
			<xsl:apply-templates select="p:agency/@ID|agency/@ID"/>
			<xsl:apply-templates select="p:agency|agency"/>
			<br/>
		</xsl:if>
		<xsl:if test="p:URI|URI">
			<xsl:apply-templates select="p:URI/@ID|URI/@ID"/>
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="p:URI|URI"/>
				</xsl:attribute>
				<xsl:apply-templates select="p:URI|URI"/>
			</a>
			<br/>
		</xsl:if>
		<xsl:if test="p:description|description">
			<xsl:apply-templates select="p:description/@ID|description/@ID"/>
			<xsl:apply-templates select="p:description|description"/>
		</xsl:if>
		<xsl:if test="p:note|note">
			<xsl:apply-templates select="p:note/@ID|note/@ID"/>
			<xsl:apply-templates select="p:note|note"/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="p:extension_schema|extension_schema">
		<xsl:apply-templates select="@ID"/>
		<xsl:if test="p:name|name">
			<xsl:apply-templates select="p:name/@ID|name/@ID"/>
			<b>
				<xsl:apply-templates select="p:name|name"/>
			</b>
			<br/>
		</xsl:if>
		<xsl:if test="p:URI|URI">
			<xsl:apply-templates select="p:URI/@ID|URI/@ID"/>
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="p:URI|URI"/>
				</xsl:attribute>
				<xsl:apply-templates select="p:URI|URI"/>
			</a>
			<br/>
		</xsl:if>
		<xsl:if test="p:context|context">
			<xsl:apply-templates select="p:context/@ID|context/@ID"/>
			<xsl:apply-templates select="p:context|context"/>
			<br/>
		</xsl:if>
		<xsl:if test="p:note|note">
			<xsl:apply-templates select="p:note/@ID|note/@ID"/>
			<xsl:apply-templates select="p:note|note"/>
			<br/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="p:vocabulary|vocabulary">
		<div style="margin-bottom:1em">
			<xsl:apply-templates select="@ID"/>
			<xsl:if test="p:name|name">
				<xsl:apply-templates select="p:name/@ID|name/@ID"/>
				<b>
					<xsl:apply-templates select="p:name|name"/>
				</b>
				<br/>
			</xsl:if>
			<xsl:if test="p:maintenance_agency|maintenance_agency">
				<xsl:apply-templates select="p:maintenance_agency/@ID|maintenance_agency/@ID"/>
				<xsl:apply-templates select="p:maintenance_agency|maintenance_agency"/>
				<br/>
			</xsl:if>
			<xsl:if test="p:URI|URI">
				<xsl:apply-templates select="p:URI/@ID|URI/@ID"/>
				<a>
					<xsl:attribute name="href">
						<xsl:value-of select="p:URI|URI"/>
					</xsl:attribute>
					<xsl:apply-templates select="p:URI|URI"/>
				</a>
				<br/>
			</xsl:if>
			<xsl:if test="p:values|values">
				<xsl:apply-templates select="p:values/@ID|values/@ID"/>
				<br/>
				<b>Values:</b>
				<ul>
					<xsl:for-each select="p:values/p:value|values/value">
						<li>
							<xsl:apply-templates select="@ID"/>
							<xsl:apply-templates/>
						</li>
					</xsl:for-each>
				</ul>
			</xsl:if>
			<xsl:if test="p:context|context">
				<i>Context:</i>
				<div style="margin-left:2em">
					<xsl:if test="p:context/@RELATEDMAT|context/@RELATEDMAT">
						<br/>
						<i>Related Material: </i>
						<xsl:apply-templates select="p:context/@RELATEDMAT|context/@RELATEDMAT"/>
						<br/>
					</xsl:if>
					<xsl:apply-templates select="p:context/@ID|context/@ID"/>
					<xsl:apply-templates select="p:context|context"/>
				</div>
			</xsl:if>
			<xsl:if test="p:description|description">
				<xsl:apply-templates select="p:description/@ID|description/@ID"/>
				<xsl:apply-templates select="p:description|description"/>
			</xsl:if>
		</div>
	</xsl:template>

	<xsl:template match="p:requirement|requirement"> Requirement <xsl:number
			count="p:requirement|requirement"/>: <div
			style="margin-bottom:1em;border: 1px solid black">
			<xsl:if test="@RELATEDMAT">
				<i>Related Material: </i>
				<xsl:apply-templates select="@RELATEDMAT"/>
			</xsl:if>
			<xsl:apply-templates select="@ID"/>
			<xsl:apply-templates/>
		</div>
	</xsl:template>

	<xsl:template name="RELATEDMAT" match="@RELATEDMAT">
		<xsl:param name="RELATEDMAT" select="normalize-space(.)"/>
		<xsl:choose>
			<xsl:when test="contains($RELATEDMAT,' ')">
				<xsl:call-template name="RELATEDMAT">
					<xsl:with-param name="RELATEDMAT"
						select="normalize-space(substring-before($RELATEDMAT,' '))"/>
				</xsl:call-template>
				<xsl:call-template name="RELATEDMAT">
					<xsl:with-param name="RELATEDMAT"
						select="normalize-space(substring-after($RELATEDMAT,' '))"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<a>
					<xsl:attribute name="href">#<xsl:value-of select="$RELATEDMAT"/></xsl:attribute><xsl:attribute name="onclick">ToggleUpId('<xsl:value-of select="$RELATEDMAT"/>')</xsl:attribute>
					<xsl:if
						test="//p:Appendix/descendant-or-self::*[@ID=$RELATEDMAT]|//Appendix/descendant-or-self::*[@ID=$RELATEDMAT]">
						<xsl:text>Appendix </xsl:text>
						<xsl:value-of
							select="//*[@ID=$RELATEDMAT]/ancestor-or-self::p:Appendix/@NUMBER"/>
						<xsl:text>: </xsl:text>
					</xsl:if>
					<xsl:choose>
						<xsl:when
							test="string-length(normalize-space(//*[@ID=$RELATEDMAT]/descendant::text()[string-length(normalize-space(.))>0][1]))>$MAX_LINK_LEN">
							<xsl:call-template name="TrimAtLastSpace">
								<xsl:with-param name="STR"
									select="normalize-space(substring(//*[@ID=$RELATEDMAT]/descendant::text()[string-length(normalize-space(.))>0][1],1,$MAX_LINK_LEN))"
								/>
							</xsl:call-template>
							<xsl:text>...</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of
								select="//*[@ID=$RELATEDMAT]/descendant::text()[string-length(normalize-space(.))>0][1]"
							/>
						</xsl:otherwise>
					</xsl:choose>
				</a>
				<xsl:text>&#160;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="TrimAtLastSpace">
		<xsl:param name="STR" select="''"/>
		<xsl:choose>
			<xsl:when test="contains($STR,' ')">
				<xsl:value-of select="normalize-space(substring-before($STR,' '))"/>
				<xsl:text> </xsl:text>
				<xsl:call-template name="TrimAtLastSpace">
					<xsl:with-param name="STR" select="normalize-space(substring-after($STR,' '))"/>
				</xsl:call-template>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="p:p|p">
		<p>
			<xsl:apply-templates select="@ID"/>
			<xsl:apply-templates/>
		</p>
	</xsl:template>

	<xsl:template match="p:head|head">
		<p>
			<xsl:apply-templates select="@ID"/>
			<b>
				<xsl:apply-templates/>
			</b>
		</p>
	</xsl:template>

	<xsl:template match="p:email|email">
		<a>
			<xsl:attribute name="href">mailto:<xsl:value-of select="."/></xsl:attribute>
			<xsl:apply-templates/>
		</a>
	</xsl:template>

	<xsl:template match="@ID">
		<xsl:variable name="ID" select="string(.)"/>
		<xsl:if test="//*[contains(@RELATEDMAT,$ID)]">
			<a>
				<xsl:attribute name="name">
					<xsl:value-of select="."/>
				</xsl:attribute>
			</a>
		</xsl:if>
	</xsl:template>


	<xsl:template match="p:Appendix|Appendix">
		<p>
			<a>
				<xsl:attribute name="name">APPENDIX_<xsl:value-of select="@NUMBER"/></xsl:attribute>
			</a>
			<xsl:apply-templates select="@ID"/>
			<b>Appendix <xsl:value-of select="@NUMBER"/></b>
			<xsl:if test="@LABEL">
				<b>: <xsl:value-of select="@LABEL"/></b>
			</xsl:if>
		</p>
		<xsl:apply-templates select="*" mode="xmlverb">
			<xsl:with-param name="indent-elements" select="true()"/>
			<!--system-property('xsl:vendor')='Microsoft'" />-->
		</xsl:apply-templates>
	</xsl:template>


</xsl:stylesheet>
