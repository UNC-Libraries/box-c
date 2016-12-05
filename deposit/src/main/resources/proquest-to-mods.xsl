<xsl:stylesheet xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="xlink" version="2.0">
	<xsl:output encoding="UTF-8" indent="yes" method="xml" standalone="yes"/>
	<!-- top level elements ELEMENT DISS_description (DISS_title,DISS_dates,DISS_degree,(DISS_institution),(DISS_advisor)*,DISS_cmte_member*,DISS_categorization)>
    -->
	<xsl:include href="iso-639-1-to-639-2b.xsl"/>
	
	<xsl:param name="graduationSemester" />
	
	<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
	<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
	
	<xsl:template name="replace">
		<xsl:param name="text" />
		<xsl:param name="replace" />
		<xsl:param name="by" />
		<xsl:choose>
		  <xsl:when test="contains($text, $replace)">
		    <xsl:value-of select="substring-before($text,$replace)" />
		    <xsl:value-of select="$by" />
		    <xsl:call-template name="replace">
		      <xsl:with-param name="text"
		      select="substring-after($text,$replace)" />
		      <xsl:with-param name="replace" select="$replace" />
		      <xsl:with-param name="by" select="$by" />
		    </xsl:call-template>
		  </xsl:when>
		  <xsl:otherwise>
		    <xsl:value-of select="$text" />
		  </xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="//DISS_collection">
				<mods:modsCollection xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-6.xsd">
					<xsl:for-each select="//DISS_collection/DISS_submission">
						<mods:mods version="3.6">
							<xsl:call-template name="DISS_submission"/>
						</mods:mods>
					</xsl:for-each>
				</mods:modsCollection>
			</xsl:when>
			<xsl:otherwise>
				<mods:mods xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
					<xsl:for-each select="//DISS_submission">
						<xsl:call-template name="DISS_submission"/>
					</xsl:for-each>
				</mods:mods>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--AUTHOR INFORMATION: MODS:NAME-->
	<xsl:template name="DISS_submission">
		<xsl:for-each select="DISS_authorship">
			<xsl:for-each select="DISS_author">
				<xsl:for-each select="DISS_name">
					<mods:name type="personal">
						<mods:namePart type="given">
							<xsl:value-of select="DISS_fname"/>
						</mods:namePart>
						<mods:namePart type="family">
							<xsl:value-of select="DISS_surname"/>
						</mods:namePart>
						<mods:role>
							<mods:roleTerm authority="marcrelator" type="text">Author</mods:roleTerm>
						</mods:role>
						<xsl:for-each select="//DISS_description/DISS_institution">
							<!-- start cleaning up the department -->
							<xsl:variable name="deptStart" select="DISS_inst_contact" />
							<!-- remove everything after colon -->
							<xsl:variable name="deptSansColon">
								<xsl:choose>
									<xsl:when test="contains($deptStart, ':')"><xsl:value-of select="substring-before($deptStart, ':')" /></xsl:when>
									<xsl:otherwise><xsl:value-of select="$deptStart" /></xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<!-- replace ampersands with the word 'and' -->
							<xsl:variable name="deptExpandAmp">
								<xsl:call-template name="replace">
								  <xsl:with-param name="text" select="$deptSansColon" />
								  <xsl:with-param name="replace" select="'&amp;'" />
								  <xsl:with-param name="by" select="'and'" />
								</xsl:call-template>
							</xsl:variable>
							<!-- make sure the department is in the proquest vocabulary? -->
							<!-- if its not, trim off any parenthetical text -->
							<!-- if its still not, then replace it with University of North Carolina at Chapel Hill -->
							
							<mods:affiliation>
								<xsl:value-of select="normalize-space($deptExpandAmp)"/>
							</mods:affiliation>
						</xsl:for-each>
					</mods:name>
				</xsl:for-each>
			</xsl:for-each>
		</xsl:for-each>
		<!--TITLE and ABSTRACT INFORMATION: MODS:TITLEINFO and MODS:ABSTRACT-->
		<xsl:for-each select="DISS_description">
			<mods:titleInfo>
				<mods:title>
					<xsl:value-of select="DISS_title"/>
				</mods:title>
			</mods:titleInfo>
		</xsl:for-each>
		<xsl:for-each select="DISS_content">
			<xsl:for-each select="DISS_abstract">
				<mods:abstract>
					<xsl:value-of select="."/>
				</mods:abstract>
			</xsl:for-each>
		</xsl:for-each>
		<!--PUBLICATION INFORMATION: MODS:ORIGININFO -->
		<!-- Taking this part out.  <mods:dateCreate>"create" could refer to a number of dates. Bridget used the diss_accept_date as the mods:dateIssued, so I'll stick with that for now. In mods:dateIssued below, It's standard to use @encoding="iso8601" but the date given isn't formatted correctly.  Can this be normalized somehow?  Sonoe 4/08/14

		<xsl:for-each select="/DISS_submission/DISS_description/DISS_dates/DISS_comp_date">
					<mods:originInfo>
						<mods:dateCreate encoding="iso8601" keyDate="yes">
							<xsl:value-of select="/DISS_submission/DISS_description/DISS_dates/DISS_comp_date"/>
						</mods:dateCreate>
					</mods:originInfo>		
		</xsl:for-each>-->
		<mods:note type="thesis" displayLabel="Graduation Date"><xsl:value-of select="$graduationSemester"/></mods:note>
		<xsl:for-each select="/DISS_submission/DISS_description/DISS_dates/DISS_comp_date">
			<mods:originInfo>
				<mods:dateIssued encoding="iso8601">
					<xsl:value-of select="/DISS_submission/DISS_description/DISS_dates/DISS_comp_date"/>
				</mods:dateIssued>
			</mods:originInfo>
		</xsl:for-each>
		<!--SUBJECT and KEYWORDS: MODS:SUBJECT, MODS:NOTE-->
		<xsl:for-each select="DISS_description">
			<xsl:for-each select="DISS_categorization">
				<xsl:choose>
					<xsl:when test="DISS_category">
						<xsl:for-each select="//DISS_cat_desc">
							<mods:note displayLabel="Keywords">
								<xsl:value-of select="."/>
							</mods:note>
						</xsl:for-each>
					</xsl:when>
				</xsl:choose>
				<mods:note displayLabel="Keywords">
					<xsl:value-of select="DISS_keyword"/>
				</mods:note>
			</xsl:for-each>
			<!-- LANGUAGE INFORMATION: MODS:LANGUAGE-->
			<xsl:for-each select="DISS_categorization">
				<mods:language>
					<mods:languageTerm type="code" authority="iso639-2b">
						<xsl:call-template name="iso-639-1-converter"/>
					</mods:languageTerm>
				</mods:language>
			</xsl:for-each>
		</xsl:for-each>
		<!-- ACADEMIC INFORMATION (DEGREE, DISCIPLINE and ADVISOR): MODS:NOTE, MODS:NAME-->
		<xsl:variable name="normalizedDegree" select="translate(translate(/DISS_submission/DISS_description/DISS_degree, $uppercase, $smallcase), '.', '')"/>
		<mods:note displayLabel="Degree">
			<xsl:choose>
				<xsl:when test="contains($normalizedDegree, 'ma')">Master of Arts</xsl:when>
				<xsl:when test="contains($normalizedDegree, 'ms')">Master of Science</xsl:when>
				<xsl:when test="contains($normalizedDegree, 'edd')">Doctor of Education</xsl:when>
				<xsl:when test="contains($normalizedDegree, 'phd')">Doctor of Philosophy</xsl:when>
				<xsl:when test="contains($normalizedDegree, 'drph')">Doctor of Public Health</xsl:when>
			</xsl:choose>
		</mods:note>
		<mods:genre authority="local">
			<xsl:choose>
				<xsl:when test="contains($normalizedDegree, 'edd') or contains($normalizedDegree, 'phd') or contains($normalizedDegree, 'drph')">Dissertation</xsl:when>
				<xsl:otherwise>Thesis</xsl:otherwise>
			</xsl:choose>
		</mods:genre>
		<xsl:for-each select="DISS_description">
			<xsl:for-each select="DISS_institution">
				<xsl:if test="DISS_inst_name">
					<mods:name type="corporate">
						<mods:namePart>
							<xsl:value-of select="DISS_inst_name"/>
						</mods:namePart>
						<mods:role>
							<mods:roleTerm authority="marcrelator" type="text">Degree granting institution</mods:roleTerm>
						</mods:role>
					</mods:name>
				</xsl:if>
				<xsl:if test="DISS_inst_contact">
					<mods:note displayLabel="Academic concentration">
						<xsl:value-of select="DISS_inst_contact"/>
					</mods:note>
				</xsl:if>
			</xsl:for-each>
			<!--  06/16/2009  Alternate mapping for DISS_inst_contact via mods:extension. Bridget -->
			<!--  <mods:extension xmlns:marc="http://www.loc.gov/MARC21/slim"><xsl:value-of select="/DISS_submission/DISS_description/DISS_institution/DISS_inst_contact"/></mods:extension>  -->
			<!--04/10/2014 Took out mods:extension.  Don't use extension; too messy.  Sonoe-->
			<xsl:for-each select="DISS_advisor/DISS_name">
				<mods:name type="personal">
					<mods:namePart type="given">
						<xsl:value-of select="DISS_fname"/>
					</mods:namePart>
					<mods:namePart type="family">
						<xsl:value-of select="DISS_surname"/>
					</mods:namePart>
					<mods:role>
						<mods:roleTerm authority="marcrelator" type="text">Thesis advisor</mods:roleTerm>
					</mods:role>
				</mods:name>
			</xsl:for-each>
			<xsl:for-each select="DISS_cmte_member/DISS_name">
				<mods:name type="personal">
					<mods:namePart type="given">
						<xsl:value-of select="DISS_fname"/>
					</mods:namePart>
					<mods:namePart type="family">
						<xsl:value-of select="DISS_surname"/>
					</mods:namePart>
					<mods:role>
						<mods:roleTerm authority="marcrelator" type="text">Thesis advisor</mods:roleTerm>
					</mods:role>
				</mods:name>
			</xsl:for-each>
		</xsl:for-each>
		<!-- TYPE OF RESOURCE AND EXTENT: MODS:TYPE OF RESOURCE and MODS:PHYSICAL DESCRIPTION -->
		<mods:typeOfResource>text</mods:typeOfResource>
		<!--RESTRICTIONS AND ACCESS: MODS:ACCESSCONDITION-->
		<!--  06/08/2009  Added mods:accessCondition to ProQuest crosswalk (was not in specifications). Bridget.
04/10/2014 Modified mods:accessCondition.  Wasn't mapped to DISS elements, so remapped mapped to DISS_submission/@embargo_code.  Conditional statements for embargo codes 0 - 2.  Sonoe-->
		<xsl:for-each select="//@embargo_code">
			<xsl:choose>
				<xsl:when test=".=1">
					<mods:accessCondition displayLabel="Embargo" type="restrictionOnAccess">This item is restricted from public view for 6 months after publication.</mods:accessCondition>
				</xsl:when>
				<xsl:when test=".=2">
					<mods:accessCondition displayLabel="Embargo" type="restrictionOnAccess">This item is restricted from public view for 1 year after publication.</mods:accessCondition>
				</xsl:when>
				<xsl:when test=".=3">
					<mods:accessCondition displayLabel="Embargo" type="restrictionOnAccess">This item is restricted from public view for 2 years after publication.</mods:accessCondition>
				</xsl:when>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
