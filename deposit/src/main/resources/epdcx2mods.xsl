<?xml version="1.0" encoding="utf-8"?>
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:mods="http://www.loc.gov/mods/v3" xmlns:epdcx="http://purl.org/eprint/epdcx/2006-11-16/" 
	xmlns:rights="http://www.cdlib.org/inside/diglib/copyrightMD" xmlns:xlink2="http://www.w3.org/1999/xlink"
	version="2.0">
	<xsl:import href="languageMappings.xsl"/>
	
	<xsl:variable name="eprintGenre" select="document('genres_eprints_swap.xml')/option-set"/>
	<xsl:key name="eprintGenreLookup" match="option" use="valueURI"/>

	<xsl:template name="epdcx2mods">
		<xsl:param name="xmlData"/>
		<xsl:for-each select="$xmlData/epdcx:descriptionSet">
			<xsl:apply-templates select="."/>
		</xsl:for-each>
	</xsl:template>

	<!-- match the top level descriptionSet element and kick off the template 
		matching process -->
	<xsl:template match="epdcx:descriptionSet">
		<mods:mods>
			<xsl:apply-templates select="epdcx:description/epdcx:statement"/>
		</mods:mods>
	</xsl:template>

	<!-- general matcher for all "statement" elements -->
	<xsl:template match="epdcx:descriptionSet/epdcx:description/epdcx:statement">
		<!-- title element: dc.title -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/title'">
			<mods:titleInfo>
				<mods:title><xsl:value-of select="epdcx:valueString" /></mods:title>
			</mods:titleInfo>
		</xsl:if>

		<!-- abstract element: dc.description.abstract -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/terms/abstract'">
			<mods:abstract><xsl:value-of select="epdcx:valueString" /></mods:abstract>
		</xsl:if>

		<!-- creator element: dc.contributor.author -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/creator'">
			<mods:name>
				<mods:namePart><xsl:value-of select="epdcx:valueString" /></mods:namePart>
			</mods:name>
		</xsl:if>
		
		<!-- contributor element dc.contributor -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/contributor'">
			<mods:name>
				<mods:namePart><xsl:value-of select="epdcx:valueString" /></mods:namePart>
			</mods:name>
		</xsl:if>

		<!-- identifier element: dc.identifier.* -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/identifier'">
			<xsl:choose>
				<xsl:when test="epdcx:valueString[@epdcx:sesURI='http://purl.org/dc/terms/URI']">
					<mods:location><mods:url><xsl:value-of select="epdcx:valueString" /></mods:url></mods:location>
				</xsl:when>
				<xsl:otherwise>
					<mods:identifier><xsl:value-of select="epdcx:valueString" /></mods:identifier>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>

		<!-- language element: dc.language.iso -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/language' and ./@epdcx:vesURI='http://purl.org/dc/terms/RFC3066'">
			<xsl:variable name="rfcCode" select="./epdcx:valueString/text()"/>
			<xsl:variable name="languageCode">
				<xsl:call-template name="RFC3066toISO639-2b">
					<xsl:with-param name="langCode" select="$rfcCode"/>
				</xsl:call-template>
			</xsl:variable>
			
			<mods:language><mods:languageTerm type="code" authority="iso639-2b"><xsl:value-of select="$languageCode"/></mods:languageTerm></mods:language>
		</xsl:if>
		
		<!-- origin info element -->
		<xsl:variable name="isPublisher" select="boolean(./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/publisher')" />
		<xsl:variable name="isDateIssued" select="boolean(./@epdcx:propertyURI='http://purl.org/dc/terms/available')" />
		<xsl:if test="$isPublisher or $isDateIssued">
			<xsl:choose>
				<xsl:when test="$isPublisher">
					<mods:originInfo>
						<mods:publisher><xsl:value-of select="epdcx:valueString" /></mods:publisher>
						<xsl:variable name="dateIssued" select="parent::*/epdcx:statement[@epdcx:propertyURI='http://purl.org/dc/terms/available']"/>
						<xsl:if test="boolean($dateIssued)">
							<mods:dateIssued><xsl:value-of select="$dateIssued/epdcx:valueString" /></mods:dateIssued>
						</xsl:if>
					</mods:originInfo>
				</xsl:when>
				<!-- Only show date issued this way if there is no publisher, so that the values are rolled up -->
				<xsl:when test="$isDateIssued and not(boolean(parent::*/epdcx:statement[@epdcx:propertyURI='http://purl.org/dc/elements/1.1/publisher']))">
					<mods:originInfo>
						<mods:dateIssued><xsl:value-of select="epdcx:valueString" /></mods:dateIssued>
					</mods:originInfo>
				</xsl:when>
			</xsl:choose>
		</xsl:if>

		<!-- item type element: dc.type -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/type'">
			<xsl:variable name="genreValueURI" select="./@epdcx:valueURI"/>
			<xsl:variable name="eprintGenreName">
				<xsl:apply-templates select="$eprintGenre">
					<xsl:with-param name="valueURI" select="$genreValueURI"/>
				</xsl:apply-templates>
			</xsl:variable>
			<xsl:if test="./@epdcx:vesURI = 'http://purl.org/eprint/terms/Type'">
				<mods:typeOfResource>text</mods:typeOfResource>
				<mods:genre>
					<xsl:attribute name="authorityURI">http://purl.org/eprint/type/</xsl:attribute>
					<xsl:attribute name="valueURI"><xsl:value-of select="$genreValueURI"></xsl:value-of></xsl:attribute>
					<xsl:attribute name="type">work type</xsl:attribute>
					<xsl:value-of select="$eprintGenreName" />
				</mods:genre>
			</xsl:if>
		</xsl:if>
		
		<!-- date element: dc.date -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/date'">
			<mods:originInfo>
				<mods:dateOther><xsl:value-of select="epdcx:valueString" /></mods:dateOther>
			</mods:originInfo>
		</xsl:if>

		<!-- publication status element: dc.description.version -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/eprint/terms/status' and ./@epdcx:vesURI='http://purl.org/eprint/terms/Status'">
			<xsl:if test="./@epdcx:valueURI='http://purl.org/eprint/status/PeerReviewed'">
				<mods:genre authority="local">Peer Reviewed</mods:genre>
			</xsl:if>
		</xsl:if>

		<!-- copyright holder element: dc.rights.holder -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/eprint/terms/copyrightHolder'">
			<mods:accessCondition>
				<rights:copyright copyright.status="copyrighted" publication.status="published">
					<rights:rights.holder><rights:name><xsl:value-of select="epdcx:valueString" /></rights:name></rights:rights.holder>
				</rights:copyright>
			</mods:accessCondition>
		</xsl:if>
		
		<!-- license element -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/terms/license'">
			<mods:accessCondition type="use and reproduction">
				<xsl:attribute name="xlink2:href">
					<xsl:value-of select="epdcx:valueString" />
				</xsl:attribute>
			</mods:accessCondition>
		</xsl:if>
		
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/terms/accessRights/' and ./@epdcx:vesURI='http://purl.org/eprint/terms/AccessRights'">
			<xsl:choose>
				<xsl:when test="./@epdcx:valueURI='http://purl.org/eprint/accessRights/OpenAccess'">
					<mods:accessCondition type="restriction on access">Open Access</mods:accessCondition>
				</xsl:when>
				<xsl:when test="./@epdcx:valueURI='http://purl.org/eprint/accessRights/RestrictedAccess'">
					<mods:accessCondition type="restriction on access">Restricted Access</mods:accessCondition>
				</xsl:when>
				<xsl:when test="./@epdcx:valueURI='http://purl.org/eprint/accessRights/ClosedAccess'">
					<mods:accessCondition type="restriction on access">Closed Access</mods:accessCondition>
				</xsl:when>
			</xsl:choose>
		</xsl:if>

		<!-- bibliographic citation element: dc.identifier.citation -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/eprint/terms/bibliographicCitation'">
			<mods:note type="citation/reference"><xsl:value-of select="epdcx:valueString" /></mods:note>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="option-set">
		<xsl:param name="valueURI" />
		<xsl:value-of select="key('eprintGenreLookup', $valueURI)/value" />
	</xsl:template>
	
	<xsl:template name="getEPDCXTitle">
		<xsl:param name="xmlData"/>
		<xsl:value-of select="$xmlData/epdcx:descriptionSet/epdcx:description/epdcx:statement[@epdcx:propertyURI='http://purl.org/dc/elements/1.1/title']/epdcx:valueString"/>
	</xsl:template>
</xsl:stylesheet>
        
     
