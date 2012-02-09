<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:p="http://www.loc.gov/METS_Profile/" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="p xs" version="2.0">

	<xs:annotation>
		<xs:documentation> Creator: Thomas Habing, Grainger Engineering Library Information Center,
			University of Illinois at Urbana-Champaign. thabing@uiuc.edu Date: 2006-04-28
			Description: Transform XML METS Profiles into HTML for viewing in a web browser. This
			style sheet has been tested with both METS_Profile documents for version 1.1 and 1.2 of
			the schema. The Appendix is not rendered by this stylesheet. History: None so far.
		</xs:documentation>
	</xs:annotation>

	<xsl:output method="text" indent="no" />

	<xsl:param name="MAX_LINK_LEN" select="50"/>

	<xsl:template match="/">
		<xsl:apply-templates/>
	</xsl:template>

<xsl:template match="p:METS_Profile|METS_Profile">
[[PageOutline]]
= <xsl:value-of select="p:title|title"/> = #<xsl:value-of select="p:title/@ID|title/@ID"/>

== <xsl:value-of select="p:URI/@LOCTYPE|URI/@LOCTYPE"/>: [<xsl:if
							test="p:URI/@LOCTYPE='URL' or p:URI/@LOCTYPE='PURL' or URI/@LOCTYPE='URL' or URI/@LOCTYPE='PURL'">
							<xsl:value-of select="p:URI|URI"/> | </xsl:if><xsl:value-of select="p:URI|URI"/>] == #<xsl:value-of select="p:URI/@ID|URI/@ID"/>

== <xsl:value-of select="p:date|date"/> == #<xsl:value-of select="p:date/@ID|date/@ID"/>

== Abstract ==	
<xsl:value-of select="p:abstract|abstract"/>

<xsl:apply-templates select="p:contact|contact"/>
		
<xsl:if test="p:registration_info|registration_info">
=== Registered === #<xsl:value-of select="p:registration_info/@ID|registration_info/@ID"/>
 [<xsl:value-of select="p:registration_info/@regURI|registration_info/@regURI"/>] <xsl:value-of select="p:registration_info/@regDate|registration_info/@regDate"/>
</xsl:if>

=== Related Profiles ===
<xsl:for-each select="p:related_profile|related_profile">
		<xsl:value-of select="@RELATIONSHIP"/>
		<xsl:if test="@RELATIONSHIP">
			<xsl:text>: </xsl:text>
		</xsl:if>
		<xsl:apply-templates/>
		<xsl:if test="@URI">
			<xsl:text> (</xsl:text> [<xsl:value-of select="@URI"/>] <xsl:text>) </xsl:text>
		</xsl:if>
	</xsl:for-each>		
		
== Extension Schema ==
<xsl:for-each select="p:extension_schema|extension_schema">
 * <xsl:apply-templates select="."/>
</xsl:for-each>
		
== Description Rules ==
<xsl:apply-templates select="p:description_rules|description_rules"/>
		
== Controlled Vocabularies ==	
<xsl:for-each select="p:controlled_vocabularies/p:vocabulary|controlled_vocabularies/vocabulary">
 <xsl:apply-templates select="."/>
</xsl:for-each>
		
== Structural Requirements ==

=== METS Root Element ===
<xsl:apply-templates
		select="p:structural_requirements/p:metsRootElement|structural_requirements/metsRootElement"/>

===	METS Header ===
<xsl:apply-templates
		select="p:structural_requirements/p:metsHdr|structural_requirements/metsHdr"/>
	
=== Description Metadata Section ===
<xsl:apply-templates
		select="p:structural_requirements/p:dmdSec|structural_requirements/dmdSec"/>
	
=== Administrative Metadata Section ===
<xsl:apply-templates
		select="p:structural_requirements/p:amdSec|structural_requirements/amdSec"/>
	
=== Content File Section ===
<xsl:apply-templates
		select="p:structural_requirements/p:fileSec|structural_requirements/fileSec"/>
	
=== Structural Map ===
<xsl:apply-templates
		select="p:structural_requirements/p:structMap|structural_requirements/structMap"/>
	
=== Structural Map Linking ===
<xsl:apply-templates
		select="p:structural_requirements/p:structLink|structural_requirements/structLink"/>
	
=== Behavior Section ===
<xsl:apply-templates
		select="p:structural_requirements/p:behaviorSec|structural_requirements/behaviorSec"/>
	
=== Multiple Sections ===
<xsl:apply-templates
		select="p:structural_requirements/p:multiSection|structural_requirements/multiSection"/>
	
== Technical Requirements ==
	
=== Content Files ===
<xsl:apply-templates
		select="p:technical_requirements/p:content_files|technical_requirements/content_files"/>
	
=== Behavior Files ===
<xsl:apply-templates
		select="p:technical_requirements/p:behavior_files|technical_requirements/behavior_files"/>
	
=== Metadata Files ===
<xsl:apply-templates select="p:technical_requirements/p:metadata_files|technical_requirements/metadata_files"/>
	
== Tools == #TOC_Tools
<xsl:for-each select="p:tool|tool">
		<xsl:apply-templates select="."/>
	</xsl:for-each>

== Appendices == #appendices
<xsl:apply-templates select="p:Appendix|Appendix"/>
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
			<xsl:if test="p:name|name">
=== <xsl:apply-templates select="p:name|name"/> ===
			</xsl:if>
			<xsl:if test="p:maintenance_agency|maintenance_agency">
				<xsl:apply-templates select="p:maintenance_agency/@ID|maintenance_agency/@ID"/>
				<xsl:apply-templates select="p:maintenance_agency|maintenance_agency"/>
			</xsl:if>
			<xsl:if test="p:URI|URI">
				[<xsl:apply-templates select="p:URI|URI"/>]
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
