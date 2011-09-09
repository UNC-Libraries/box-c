<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:import href="dcToFullRecord.xsl" />
	<xsl:import href="modsToFullRecord.xsl" />
	<xsl:output  method="xml" omit-xml-declaration="yes" indent="no"/>
	
	<xsl:template name="getMostRecentDate">
		<xsl:param name="highDate"/>
		<xsl:param name="currentNode"/>
		<xsl:variable name="newHighDate">
			<xsl:choose>
				<xsl:when test="$currentNode/@CREATED > $highDate">
					<xsl:value-of select="$currentNode/@CREATED"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$highDate"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="nextSibling" select="$currentNode/following-sibling::node()[local-name() = 'datastreamVersion'][1]"/>
		<xsl:choose>
			<xsl:when test="boolean($nextSibling)">
				<xsl:call-template name="getMostRecentDate">
					<xsl:with-param name="highDate" select="$newHighDate"/>
					<xsl:with-param name="currentNode" select="$nextSibling"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$newHighDate"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="/">
		<div class="metadata">
			<xsl:choose>
				<xsl:when test="boolean(/view-inputs/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='MD_DESCRIPTIVE']/*[local-name() = 'datastreamVersion'][last()]/*[local-name() = 'xmlContent']/*[local-name() = 'mods'])">
					<xsl:variable name="mostRecentDate">
						<xsl:call-template name="getMostRecentDate">
							<xsl:with-param name="highDate"> </xsl:with-param>
							<xsl:with-param name="currentNode" select="/view-inputs/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='MD_DESCRIPTIVE']/*[local-name() = 'datastreamVersion'][1]"/>
						</xsl:call-template>
					</xsl:variable>
					<xsl:apply-templates select="/view-inputs/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='MD_DESCRIPTIVE']/*[local-name() = 'datastreamVersion' and @CREATED = $mostRecentDate]/*[local-name() = 'xmlContent']/*[local-name() = 'mods']"/>
				</xsl:when>
				<xsl:when test="boolean(/view-inputs/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='DC']/*[local-name() = 'datastreamVersion'][last()]/*[local-name() = 'xmlContent']/*[local-name() = 'dc'])">
					<xsl:variable name="mostRecentDate">
						<xsl:call-template name="getMostRecentDate">
							<xsl:with-param name="highDate"> </xsl:with-param>
							<xsl:with-param name="currentNode" select="/view-inputs/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='DC']/*[local-name() = 'datastreamVersion'][1]"/>
						</xsl:call-template>
					</xsl:variable>
					<xsl:apply-templates select="/view-inputs/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='DC']/*[local-name() = 'datastreamVersion' and @CREATED = $mostRecentDate]/*[local-name() = 'xmlContent']/*[local-name() = 'dc']"/>
				</xsl:when>
			</xsl:choose>
		</div>
	</xsl:template>
</xsl:stylesheet>