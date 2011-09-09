<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:import href="languageNames.xsl" />
	<xsl:output  method="xml" omit-xml-declaration="yes" indent="no"/>
	<!--
	Transforms a dublin core record into a table formatted according to the needs of the 
	full record page in the CDR public UI.  
	Author: Ben Pennell
	$Id
	$URL: https://vcs.lib.unc.edu/cdr/cdr-access/external/recordTransformations/modsToFullRecord.xsl $
	 -->
	<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
	
	<xsl:template name="dcLanguages">
		<xsl:variable name="languages" select="*[local-name() = 'language']"/>
		<xsl:if test="boolean($languages)">
			<tr>
				<th>
					<xsl:text>Language</xsl:text>		
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="$languages">
						<xsl:call-template name="getISO639-2Name">
							<xsl:with-param name="langCode" select="text()"/>
						</xsl:call-template>
						<br/><xsl:value-of select="$newline"/>
					</xsl:for-each>
				</td>
			</tr><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="dcField">
		<xsl:param name="label"/>
		<xsl:param name="field"/>
		<xsl:if test="boolean($field)">
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="boolean($label)"><xsl:value-of select="$label"/></xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring(local-name(),1,1)), substring(local-name(),2))"></xsl:value-of>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="$field">
						<xsl:value-of select="text()"/><br/><xsl:value-of select="$newline"/>
					</xsl:for-each>
				</td>
			</tr><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'dc']">
		<div>
			<xsl:variable name="creator" select="*[local-name() = 'creator']"/>
			<xsl:variable name="contributor" select="*[local-name() = 'contributor']"/>
			<xsl:variable name="title" select="*[local-name() = 'title']"/>
			<xsl:if test="boolean($creator) or boolean($contributor) or boolean($title)">
				<table><xsl:value-of select="$newline"/>
					<xsl:call-template name="dcField">
						<xsl:with-param name="label">Creator</xsl:with-param>
						<xsl:with-param name="field" select="$creator"/>
					</xsl:call-template>
					<xsl:call-template name="dcField">
						<xsl:with-param name="label">Contributor</xsl:with-param>
						<xsl:with-param name="field" select="$contributor"/>
					</xsl:call-template>
					<xsl:call-template name="dcField">
						<xsl:with-param name="label">Title</xsl:with-param>
						<xsl:with-param name="field" select="$title"/>
					</xsl:call-template>
				</table><xsl:value-of select="$newline"/>
			</xsl:if>
			<xsl:variable name="subject" select="*[local-name() = 'subject']"/>
			<xsl:if test="boolean($subject)">
				<table><xsl:value-of select="$newline"/>
					<xsl:call-template name="dcField">
					<xsl:with-param name="label">Subject</xsl:with-param>
					<xsl:with-param name="field" select="$subject"/>
				</xsl:call-template>
				</table><xsl:value-of select="$newline"/>
			</xsl:if>
			<xsl:variable name="language" select="*[local-name() = 'language']"/>
			<xsl:variable name="type" select="*[local-name() = 'type']"/>
			<xsl:variable name="coverage" select="*[local-name() = 'coverage']"/>
			<xsl:variable name="identifier" select="*[local-name() = 'identifier']"/>
			<xsl:variable name="rights" select="*[local-name() = 'rights']"/>
			<xsl:if test="boolean($language) or boolean($type) or boolean($coverage) or boolean($identifier) or boolean($rights)">
				<table><xsl:value-of select="$newline"/>
					<xsl:call-template name="dcLanguages"/>
					<xsl:call-template name="dcField">
						<xsl:with-param name="label">Type</xsl:with-param>
						<xsl:with-param name="field" select="$type"/>
					</xsl:call-template>
					<xsl:call-template name="dcField">
						<xsl:with-param name="label">Coverage</xsl:with-param>
						<xsl:with-param name="field" select="$coverage"/>
					</xsl:call-template>
						<xsl:call-template name="dcField">
						<xsl:with-param name="label">Identifier</xsl:with-param>
						<xsl:with-param name="field" select="$identifier"/>
					</xsl:call-template>
					<xsl:call-template name="dcField">
						<xsl:with-param name="label">Access Rights</xsl:with-param>
						<xsl:with-param name="field" select="$rights"/>
					</xsl:call-template>
				</table><xsl:value-of select="$newline"/>
			</xsl:if>
		</div>
		
	</xsl:template>
</xsl:stylesheet>