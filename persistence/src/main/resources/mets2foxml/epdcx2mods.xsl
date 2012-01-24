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
	version="1.0">
	
	<xsl:variable name="eprintGenre" select="document('/edu/unc/lib/dl/schematron/genres_eprints_swap.xml')/option-set"/>
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
				<role type="text">creator</role>
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
					<mods:location><mods:uri><xsl:value-of select="epdcx:valueString" /></mods:uri></mods:location>
				</xsl:when>
				<xsl:otherwise>
					<mods:identifier><xsl:value-of select="epdcx:valueString" /></mods:identifier>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>

		<!-- language element: dc.language.iso -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/language' and ./@epdcx:vesURI='http://purl.org/dc/terms/RFC3066'">
			<mods:language><xsl:value-of select="epdcx:valueString" /></mods:language>
		</xsl:if>
		
		<!-- publisher element -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/publisher'">
			<mods:originInfo>
				<mods:publisher><xsl:value-of select="epdcx:valueString" /></mods:publisher>
			</mods:originInfo>
		</xsl:if>

		<!-- item type element: dc.type -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/elements/1.1/type'">
			<xsl:variable name="genreValueURI" select="./@epdcx:valueURI"/>
			<xsl:variable name="eprintGenreName">
				<xsl:apply-templates select="$eprintGenre">
					<xsl:with-param name="valueURI" select="$genreValueURI"/>
				</xsl:apply-templates>
			</xsl:variable>
			<xsl:if test="not($eprintGenreName = '')">
				<mods:typeOfResource><xsl:value-of select="$eprintGenreName"/></mods:typeOfResource>
			</xsl:if>
			<xsl:if test="./@epdcx:vesURI = 'http://purl.org/eprint/terms/Type'">
				<mods:genre>
					<xsl:attribute name="authorityURI">http://purl.org/eprint/type/</xsl:attribute>
					<xsl:attribute name="valueURI"><xsl:value-of select="$genreValueURI"></xsl:value-of></xsl:attribute>
					<xsl:value-of select="$eprintGenreName" />
				</mods:genre>
			</xsl:if>
		</xsl:if>

		<!-- date available element: dc.date.issued -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/terms/available'">
			<mods:originInfo>
				<mods:dateIssued><xsl:value-of select="epdcx:valueString" /></mods:dateIssued>
			</mods:originInfo>
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
				<mods:note>Peer Reviewed</mods:note>
			</xsl:if>
		</xsl:if>

		<!-- copyright holder element: dc.rights.holder -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/eprint/terms/copyrightHolder'">
			<mods:accessCondition>
				<mods:rights.holder><xsl:value-of select="epdcx:valueString" /></mods:rights.holder>
			</mods:accessCondition>
		</xsl:if>
		
		<!-- license element -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/terms/license'">
			<mods:accessCondition type="use and reproduction">
				<xsl:value-of select="epdcx:valueString" />
			</mods:accessCondition>
		</xsl:if>
		
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/dc/terms/accessRights/' and ./@epdcx:vesURI='http://purl.org/eprint/terms/AccessRights'">
			<xsl:choose>
				<xsl:when test="./@epdcx:valueURI='http://purl.org/eprint/accessRights/OpenAccess'">
					<mods:accessCondition type="access">Open Access</mods:accessCondition>
				</xsl:when>
				<xsl:when test="./@epdcx:valueURI='http://purl.org/eprint/accessRights/RestrictedAccess'">
					<mods:accessCondition type="access">Restricted Access</mods:accessCondition>
				</xsl:when>
				<xsl:when test="./@epdcx:valueURI='http://purl.org/eprint/accessRights/ClosedAccess'">
					<mods:accessCondition type="access">Closed Access</mods:accessCondition>
				</xsl:when>
			</xsl:choose>
		</xsl:if>

		<!-- bibliographic citation element: dc.identifier.citation -->
		<xsl:if test="./@epdcx:propertyURI='http://purl.org/eprint/terms/bibliographicCitation'">
			<mods:relatedItem type="host">
				<mods:part>
					<mods:text><xsl:value-of select="epdcx:valueString" /></mods:text>
				</mods:part>
			</mods:relatedItem>
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
        
     
