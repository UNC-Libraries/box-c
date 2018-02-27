<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:xlink="http://www.w3.org/1999/xlink"
		xmlns:cdrfn="http://cdr.lib.unc.edu/fn/"
		xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xsl:import href="/recordTransformations/languageNames.xsl"/>
	<xsl:import href="/recordTransformations/scriptNames.xsl"/>
	<xsl:output method="xml" omit-xml-declaration="yes" indent="no"/>
	<!--
	Transforms a mods record into a table formatted according to the needs of the
	full record page in the CDR public UI.
	Author: Ben Pennell
	 -->
	<xsl:variable name="newline"><xsl:text>
	</xsl:text></xsl:variable>

	<!-- mods:name -->
	<xsl:template match="*[local-name() = 'name']">
		<xsl:call-template name="formatName">
			<xsl:with-param name="nameField" select="." />
		</xsl:call-template>

		<br/><xsl:value-of select="$newline"/>
		
		<xsl:variable name="altName" select="*[local-name() = 'alternativeName']"/>
		<xsl:if test="boolean($altName)">
			<xsl:for-each select="*[local-name() = 'alternativeName']">
				<span>
					<xsl:text>Alternative Name:  </xsl:text>
					<xsl:call-template name="formatName">
						<xsl:with-param name="nameField" select="." />
					</xsl:call-template>
				</span>
			</xsl:for-each>
		</xsl:if>

		<xsl:variable name="orcid" select="*[local-name() = 'nameIdentifier' and @type = 'orcid']"/>
		<xsl:if test="boolean($orcid)">
			<xsl:variable name="orcid_id">
				<xsl:choose>
					<xsl:when test="contains($orcid, 'orcid.org/')">
						<xsl:value-of select="cdrfn:substring-after-last($orcid, '/')" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$orcid" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

			<span>
				<a>
					<xsl:attribute name="href">
						<xsl:text>http://orcid.org/</xsl:text><xsl:value-of select="$orcid_id"/>
					</xsl:attribute>
					<xsl:text>orcid.org/</xsl:text><xsl:value-of select="$orcid_id"/>
				</a><br/><xsl:value-of select="$newline"/>
			</span>
		</xsl:if>

		<xsl:variable name="affiliation" select="*[local-name() = 'affiliation']"/>
		<xsl:if test="boolean($affiliation)">
			<span>
				<xsl:text>Affiliation:  </xsl:text>
				<xsl:for-each select="*[local-name() = 'affiliation']">
					<xsl:value-of select="text()"/><br/><xsl:value-of select="$newline"/>
				</xsl:for-each>
			</span>
		</xsl:if>

		<xsl:variable name="description" select="*[local-name() = 'description']"/>
		<xsl:if test="boolean($description)">
			<xsl:text>Description:  </xsl:text><xsl:value-of select="$description"/><br/><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="formatName">
		<xsl:param name="nameField"/>
		
		<xsl:variable name="displayForm" select="$nameField/*[local-name() = 'displayForm']"/>
		<xsl:variable name="givenName" select="$nameField/*[local-name() = 'namePart' and @type='given']"/>
		<xsl:variable name="familyName" select="$nameField/*[local-name() = 'namePart' and @type='family']"/>
		<xsl:variable name="dateName" select="$nameField/*[local-name() = 'namePart' and @type='date']"/>
		<xsl:variable name="termsOfAddress" select="$nameField/*[local-name() = 'namePart' and @type='termsOfAddress']"/>
		
		<xsl:choose>
			<xsl:when test="boolean($displayForm)">
				<xsl:for-each select="$displayForm">
					<xsl:if test="position() != 1">
						<xsl:text>; </xsl:text>
					</xsl:if>
					<xsl:value-of select="text()"/>
				</xsl:for-each>
			</xsl:when>
			<xsl:when test="boolean($givenName) and boolean($familyName)">
				<xsl:value-of select="$familyName"/><xsl:text>, </xsl:text><xsl:value-of select="$givenName"/>
				<xsl:if test="boolean($termsOfAddress)">
					<xsl:text>, </xsl:text><xsl:value-of select="$termsOfAddress"/>
				</xsl:if>

				<xsl:if test="boolean($dateName)">
					<xsl:text>, </xsl:text><xsl:value-of select="$dateName"/>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="*[local-name() = 'namePart']">
					<xsl:if test="position() != 1">
						<xsl:text>; </xsl:text>
					</xsl:if>
					<xsl:value-of select="text()"/>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="modsNames">
		<xsl:for-each-group select="*[local-name() = 'name']" group-by="@displayLabel, .[not(@displayLabel)]/*[local-name() = 'role']/*[local-name() = 'roleTerm']/text(), local-name(.[not(@displayLabel)][./not(*[local-name() = 'role']/*[local-name() = 'roleTerm'])])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>

				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = local-name()">
							<xsl:value-of>Creator</xsl:value-of>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="current-group()">
						<p>
							<xsl:apply-templates select="."/>
						</p>
					</xsl:for-each>
				</td>
			</tr><xsl:value-of select="$newline"/>
		</xsl:for-each-group>
	</xsl:template>
	<!-- mods:titleInfo -->
	<xsl:template match="*[local-name() = 'titleInfo']" mode="brief">
		<xsl:variable name="nonSort" select="*[local-name() = 'nonSort']"/>
		<xsl:if test="boolean($nonSort)">
			<xsl:value-of select="$nonSort"/><xsl:text> </xsl:text>
		</xsl:if>

		<xsl:variable name="title" select="*[local-name() = 'title']"/>
		<xsl:if test="boolean($title)">
			<xsl:value-of select="$title"/>
		</xsl:if>

		<xsl:variable name="subTitle" select="*[local-name() = 'subTitle']"/>
		<xsl:if test="boolean($subTitle)">
			<xsl:text>: </xsl:text><xsl:value-of select="$subTitle"/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="*[local-name() = 'titleInfo']">
		<xsl:variable name="nonSort" select="*[local-name() = 'nonSort']"/>
		<xsl:if test="boolean($nonSort)">
			<xsl:value-of select="$nonSort"/><xsl:text> </xsl:text>
		</xsl:if>

		<xsl:variable name="title" select="*[local-name() = 'title']"/>
		<xsl:if test="boolean($title)">
			<xsl:value-of select="$title"/>
		</xsl:if>

		<xsl:variable name="subTitle" select="*[local-name() = 'subTitle']"/>
		<xsl:if test="boolean($subTitle)">
			<xsl:text>: </xsl:text><xsl:value-of select="$subTitle"/>
		</xsl:if>
		<xsl:value-of select="$newline"/>

		<xsl:variable name="partNumber" select="*[local-name() = 'partNumber']"/>
		<xsl:if test="boolean($partNumber)">
			<xsl:for-each select="$partNumber">
				<xsl:text>Part Number: </xsl:text><xsl:value-of select="."/><br/>
			</xsl:for-each>
		</xsl:if>
		<xsl:variable name="partName" select="*[local-name() = 'partName']"/>
		<xsl:if test="boolean($partName)">
			<xsl:for-each select="$partName">
				<xsl:text>Part Name: </xsl:text><xsl:value-of select="."/><br/>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<xsl:template name="modsTitles">
		<xsl:for-each-group select="*[local-name() = 'titleInfo']" group-by="@displayLabel, .[not(@displayLabel)]/@type, local-name(.[not(@displayLabel) and not(@type)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = local-name()">
							<xsl:value-of>Title</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'alternative'">
							<xsl:value-of>Alternative Title</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'translated'">
							<xsl:value-of>Translated Title</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'abbreviated'">
							<xsl:value-of>Abbreviated Title</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'uniform'">
							<xsl:value-of>Uniform Title</xsl:value-of>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="current-group()">
						<p>
							<xsl:apply-templates select="."/>
						</p>
					</xsl:for-each>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>
	<!-- mods:originInfo mods:originInfo/place-->
	<xsl:template name="modsOriginPlaces">


		<xsl:variable name="place" select="*[local-name() = 'originInfo']/*[local-name() = 'place']"/>
		<xsl:if test="boolean($place)">
			<tr>
				<th>Place of Publication</th>
				<td>
					<xsl:for-each select="$place">
						<xsl:for-each select="*[local-name() = 'placeTerm']">
							<xsl:value-of select="."/><br/><xsl:value-of select="$newline"/>
						</xsl:for-each>

					</xsl:for-each>
				</td>
			</tr>
		</xsl:if>


	</xsl:template>
	<!-- Template for a simple name / value pair display -->
	<xsl:template name="modsField">
		<xsl:param name="label"/>
		<xsl:param name="field"/>

		<xsl:if test="boolean($field)">
			<tr>
				<th><xsl:value-of select="$label"/></th>
				<td>
					<xsl:for-each select="$field">
						<xsl:value-of select="text()"/><br/><xsl:value-of select="$newline"/>
					</xsl:for-each>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>

	<xsl:template name="modsGroupedField">
		<xsl:param name="defaultLabel"/>
		<xsl:param name="field"/>
		<xsl:if test="boolean($field)">
			<xsl:for-each-group select="$field" group-by="@displayLabel, local-name(.[not(@displayLabel)])[. != '']">
				<xsl:variable name="groupKey" select="current-grouping-key()"/>
				<tr>
					<th>
						<xsl:choose>
							<xsl:when test="$groupKey = local-name()">
								<xsl:value-of select="$defaultLabel"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
							</xsl:otherwise>
						</xsl:choose>
					</th><xsl:value-of select="$newline"/>
					<td>
						<xsl:for-each select="current-group()">
							<xsl:value-of select="text()"/><br/><xsl:value-of select="$newline"/>
						</xsl:for-each>
					</td>
				</tr>
			</xsl:for-each-group>
		</xsl:if>
	</xsl:template>

	<xsl:template name="modsGroupedFieldWithType">
		<xsl:param name="defaultLabel"/>
		<xsl:param name="field"/>
		<xsl:if test="boolean($field)">
			<xsl:for-each-group select="$field" group-by="@displayLabel, .[not(@displayLabel)]/@type, local-name(.[not(@displayLabel) and not(@type)])[. != '']">
				<xsl:variable name="groupKey" select="current-grouping-key()"/>
				<tr>
					<th>
						<xsl:choose>
							<xsl:when test="$groupKey = local-name()">
								<xsl:value-of select="$defaultLabel"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:choose>
									<xsl:when test="$groupKey = 'doi' or $groupKey = 'issn'">
										<xsl:value-of select="upper-case($groupKey)"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:otherwise>
						</xsl:choose>
					</th><xsl:value-of select="$newline"/>
					<td>
						<xsl:for-each select="current-group()">
							<xsl:choose>
								<xsl:when test="boolean(text())">
									<xsl:choose>
										<xsl:when test="boolean(@xlink:href)">
											<a href="{@xlink:href}" target="_blank">
												<xsl:analyze-string select="text()" regex="\n">
													<xsl:matching-substring>
														<br />
													</xsl:matching-substring>
													<xsl:non-matching-substring>
														<xsl:value-of select="normalize-space(.)" />
													</xsl:non-matching-substring>
												</xsl:analyze-string>
											</a>
										</xsl:when>
										<xsl:otherwise>
											<xsl:analyze-string select="text()" regex="\n">
												<xsl:matching-substring>
													<br />
												</xsl:matching-substring>
												<xsl:non-matching-substring>
													<xsl:value-of select="normalize-space(.)" />
												</xsl:non-matching-substring>
											</xsl:analyze-string>
										</xsl:otherwise>
									</xsl:choose>
									<br />
									<xsl:value-of select="$newline" />
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="./@*[local-name() = 'href']"/><br/><xsl:value-of select="$newline"/>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:for-each>
					</td>
				</tr>
			</xsl:for-each-group>
		</xsl:if>
	</xsl:template>
<!-- mods:originInfo dates -->
	<xsl:template name="modsOriginDates">
		<xsl:for-each-group select="*[local-name() = 'originInfo']/*[contains(local-name(), 'date') or local-name() = 'copyrightDate']" group-by="../@displayLabel, local-name(.[not(../@displayLabel)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = 'dateIssued'">
							<xsl:value-of>Publication Date</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'dateCreated'">
							<xsl:value-of>Date Created</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'dateCaptured'">
							<xsl:value-of>Date Captured</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'dateValid'">
							<xsl:value-of>Date Valid</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'dateModified'">
							<xsl:value-of>Date Modified</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'copyrightDate'">
							<xsl:value-of>Copyright Date</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'dateOther'">
							<xsl:value-of>Other Date</xsl:value-of>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="current-group()">
						<xsl:if test="boolean(@point='start')">
							<xsl:text>Start date: </xsl:text>
						</xsl:if>
						<xsl:if test="boolean(@point='end')">
							<xsl:text>End date: </xsl:text>
						</xsl:if>
						<xsl:value-of select="text()"/>
						<xsl:if test="boolean(@qualifier)">
							<xsl:text> (</xsl:text><xsl:value-of select="@qualifier"/><xsl:text>)</xsl:text>
						</xsl:if>
						<br/><xsl:value-of select="$newline"/>
					</xsl:for-each>

					<br/><xsl:value-of select="$newline"/>
				</td>
			</tr>

		</xsl:for-each-group>
	</xsl:template>

	<!-- mods:language -->
	<xsl:template name="modsLanguages">
		<xsl:for-each-group select="*[local-name() = 'language']" group-by="@displayLabel, .[not(@displayLabel)]/@usage, local-name(.[not(@displayLabel) and not(@usage)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = local-name()">
							<xsl:text>Language</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
							<xsl:if test="@usage">
								<xsl:text> language</xsl:text>
							</xsl:if>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<!-- Output languages, grouped by display label or not having a display label.  Languages are prioritized
					so that text language entries will take precedence, followed by iso639-2b codes, and then everything else. -->
					<xsl:for-each select="current-group()">
						<xsl:variable name="languages" select="*[local-name() = 'languageTerm']"/>
						<xsl:if test="boolean($languages)">
							<xsl:call-template name="getLanguageName">
								<xsl:with-param name="languageNodes" select="$languages"/>
							</xsl:call-template>
								<xsl:if test="@objectPart">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="@objectPart"/>
									<xsl:text>)</xsl:text>
								</xsl:if>
							<br/><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:for-each>
						<!-- Display script if available -->
					<xsl:for-each select="current-group()">
						<xsl:variable name="languages" select="*[local-name() = 'scriptTerm']"/>
						<xsl:if test="boolean($languages)">
							<xsl:text>Script:  </xsl:text>
							<xsl:call-template name="getScriptName">
								<xsl:with-param name="scriptNodes" select="$languages"/>
							</xsl:call-template>
							<xsl:if test="@objectPart">
								<xsl:text> (</xsl:text>
								<xsl:value-of select="@objectPart"/>
								<xsl:text>)</xsl:text>
							</xsl:if>
							<br/><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:for-each>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>
	<!-- mods:subject -->
	<xsl:template name="cartographics">
		<xsl:value-of select="*[local-name() = 'scale']"/>
		<xsl:if test="*[local-name() = 'projection']">
			<xsl:if test="*[local-name() = 'scale']">
				<xsl:text> ; </xsl:text>
			</xsl:if>
			<xsl:value-of select="*[local-name() = 'projection']"/>
		</xsl:if>
		<xsl:if test="*[local-name() = 'coordinates']">
			<xsl:text> </xsl:text>
			(<xsl:value-of select="*[local-name() = 'coordinates']"/>).
		</xsl:if>
	</xsl:template>
	<xsl:template name="modsSubjects">
		<xsl:for-each-group select="*[local-name() = 'subject']" group-by="@displayLabel, local-name(.[not(@displayLabel)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = local-name()">
							<xsl:text>Subject</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="current-group()">
						<xsl:for-each select="./*">
							<br/><xsl:value-of select="$newline"/>
							<!-- Render the second tier of children based on the element name of the first tier -->
							<xsl:choose>
								<xsl:when test="local-name() = 'name' or local-name() = 'titleInfo'">
									<xsl:apply-templates select="." mode="brief"/>
								</xsl:when>
								<xsl:when test="local-name() = 'hierarchicalGeographic'">
									<xsl:for-each select="./*">
										<xsl:if test="position() != 1">
											<xsl:text>, </xsl:text>
										</xsl:if>
										<xsl:value-of select="text()"/>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="local-name() = 'cartographics'">
									<xsl:call-template name="cartographics"/>
								</xsl:when>
								<xsl:when test="../@xlink:href">
									<a href="{../@xlink:href}" target="_blank">
									<xsl:value-of select="text()"/>
									</a>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="text()"/>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:for-each>
						<br/><xsl:value-of select="$newline"/>
					</xsl:for-each>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>

	<!-- mods:abstract -->
	<xsl:template name="modsAbstract">
		<xsl:for-each select="*[local-name() = 'abstract']">
			<xsl:if test="@type='Content advice'">
			<tr>
				<th>
						<xsl:value-of select="@type"/>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:if test="@type='Content advice'">
						<xsl:value-of select="text()"/>
					</xsl:if>
				</td><xsl:value-of select="$newline"/>
			</tr>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	<!-- mods:classification -->
	<xsl:template name="modsClassifications">
		<xsl:for-each-group select="*[local-name() = 'classification']" group-by="@displayLabel, local-name(.[not(@displayLabel)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = local-name()">
							<xsl:text>Classification</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="current-group()">
						<xsl:value-of select="text()"/>
						<xsl:if test="boolean(@authority)">
							<xsl:text> (</xsl:text><xsl:value-of select="@authority"/><xsl:text>)</xsl:text>
						</xsl:if>
						<br/><xsl:value-of select="$newline"/>
					</xsl:for-each>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>
	<xsl:template name="modsLocations">
		<xsl:for-each-group select="*[local-name() = 'location']" group-by="@displayLabel, local-name(.[not(@displayLabel)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = local-name()">
							<xsl:text>File location</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="current-group()">
						<xsl:for-each select="./*[local-name() = 'url']|./*[local-name() = 'physicalLocation']">
							<xsl:if test="@displayLabel">
								<xsl:value-of select="concat(upper-case(substring(@displayLabel,1,1)), substring(@displayLabel,2))"/><xsl:text>: </xsl:text>
							</xsl:if>
							<a href="{text()}" target="_blank">
								 <xsl:value-of select="text()"/>
							</a>
							<xsl:if test="local-name() = 'url'">
								<xsl:for-each select="@access|@note">
									<xsl:if test="position() = 1">
										<xsl:text> (</xsl:text>
									</xsl:if>
									<xsl:if test="position() != 1">
										<xsl:text>, </xsl:text>
									</xsl:if>

									<xsl:value-of select="."/>

									<xsl:if test="position() = last()">
										<xsl:text>)</xsl:text>
									</xsl:if>
								</xsl:for-each>
							</xsl:if>
							<br/><xsl:value-of select="$newline"/>
						</xsl:for-each>
					</xsl:for-each>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>

	<!-- mods:physicalDescription -->
	<xsl:template name="modsPhysicalDescription">
		<xsl:for-each-group select="*[local-name() = 'physicalDescription']/*" group-by="@displayLabel, .[not(@displayLabel)]/@type, @unit, local-name(.[not(@displayLabel) and not(@type) and not (@unit)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = 'form'">
							<xsl:value-of>Form</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'reformattingQuality'">
							<xsl:value-of>Reformatting Quality</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'internetMediaType'">
							<xsl:value-of>Internet Media Type</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'extent'">
							<xsl:choose>
								<xsl:when test="@unit">
									<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of>Extent</xsl:value-of>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:when>
						<xsl:when test="$groupKey = 'digitalOrigin'">
							<xsl:value-of>Digital Origin</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'note'">
							<xsl:value-of>Physical Description Note</xsl:value-of>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<td>
					<xsl:for-each select="current-group()">
						<xsl:value-of select="text()"/><br/><xsl:value-of select="$newline"/>
					</xsl:for-each>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>

	<!-- mods:part -->
	<xsl:template name="modsParts">
		<xsl:for-each-group select="*[local-name() = 'part']" group-by="@displayLabel, local-name(.[not(@displayLabel)])[. != '']">
			<xsl:variable name="groupKey" select="current-grouping-key()"/>
			<tr>
				<!-- if part/text is available, show only this subelement.  When @displayLabel is present it overrides "Part" label. -->
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = local-name()">
							<xsl:text>Part</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th><xsl:value-of select="$newline"/>
				<!-- If part/text is not available, show the rest of the subelements  -->
				<td>
					<xsl:for-each select="current-group()">
						<xsl:sort select="@order"/>
						<xsl:for-each select="./*">
								<xsl:choose>
									<xsl:when test="local-name() = 'detail'">
										<xsl:for-each select="./*">
											<xsl:choose>
												<xsl:when test="local-name() = 'number' and boolean(../@type)">
													<xsl:value-of select="concat(upper-case(substring(../@type,1,1)), substring(../@type,2))"/><xsl:text>: </xsl:text><xsl:value-of select="text()"/>
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="concat(upper-case(substring(local-name(),1,1)), substring(local-name(),2))"/>
													<xsl:text>: </xsl:text><xsl:value-of select="text()"/>
												</xsl:otherwise>
											</xsl:choose>
											<br/><xsl:value-of select="$newline"/>
										</xsl:for-each>
									</xsl:when>

									<xsl:when test="local-name() = 'extent'">
										<xsl:for-each select="./*">
											<xsl:choose>
												<xsl:when test="local-name() = 'start' or local-name() = 'end'">
													<xsl:value-of select="concat(upper-case(substring(local-name(),1,1)), substring(local-name(),2))"/>
													<xsl:text>: </xsl:text><xsl:value-of select="text()"/>
												</xsl:when>

												<xsl:when test="local-name() = 'total'">
													<xsl:choose>
														<xsl:when test="../@unit">
															<xsl:value-of select="concat(upper-case(substring(local-name(),1,1)), substring(local-name(),2)), ../@unit"/>
															<xsl:text>: </xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:value-of select="concat(upper-case(substring(local-name(),1,1)), substring(local-name(),2))"/>
															<xsl:text>: </xsl:text>
														</xsl:otherwise>
													</xsl:choose>
													<xsl:value-of select="text()"/>
												</xsl:when>
												<xsl:when test="local-name() = 'list'">
													<xsl:text>List: </xsl:text>
													<xsl:value-of select="text()"/>
												</xsl:when>
											</xsl:choose>
											<br/><xsl:value-of select="$newline"/>
										</xsl:for-each>
									</xsl:when>

									<xsl:when test="local-name()='date'">
										<xsl:choose>
											<xsl:when test="@point='start'">
												<xsl:text>Start date: </xsl:text>
												<xsl:value-of select="text()"/>
												<xsl:if test="boolean(@qualifier)">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="@qualifier"/>
													<xsl:text>)</xsl:text>
												</xsl:if>
												<br/><xsl:value-of select="$newline"/>
											</xsl:when>
											<xsl:when test="@point='end'">
												<xsl:text>End date: </xsl:text>
												<xsl:value-of select="text()"/>
												<xsl:if test="boolean(@qualifier)">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="@qualifier"/>
													<xsl:text>)</xsl:text>
												</xsl:if>
												<br/><xsl:value-of select="$newline"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>Date: </xsl:text>
												<xsl:value-of select="text()"/>
												<xsl:if test="boolean(@qualifier)">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="@qualifier"/>
													<xsl:text>)</xsl:text>
												</xsl:if>
												<br/><xsl:value-of select="$newline"/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>

									<xsl:when test="local-name()= 'text'">

										<xsl:choose>
											<xsl:when test="boolean(@type)">
												<xsl:choose>
													<xsl:when test="boolean(@displayLabel)">
														<xsl:value-of select="concat(upper-case(substring(@displayLabel,1,1)), substring(@displayLabel,2))"/>
													<xsl:text>: </xsl:text><xsl:value-of select="text()"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="concat(upper-case(substring(@type,1,1)), substring(@type,2))"/>
														<xsl:text>: </xsl:text><xsl:value-of select="text()"/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select="text()"/>
											</xsl:otherwise>
										</xsl:choose>
										<br/><xsl:value-of select="$newline"/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</xsl:for-each>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>

	<!-- mods:relatedItem -->
	<!-- Related items can contain any kind of item, so just reusing the other templates and nesting in an extra table-->
	<xsl:template name="modsRelatedItems">
		<xsl:for-each-group select="*[local-name() = 'relatedItem']" group-by="@displayLabel, .[not(@displayLabel)]/@type, local-name(.[not(@displayLabel) and not(@type)])[. != '']">
			<tr>
				<xsl:variable name="groupKey" select="current-grouping-key()"/>
				<th>
					<xsl:choose>
						<xsl:when test="$groupKey = 'isReferencedBy'">
							<xsl:attribute name="title">Finding aid, catalog record, or similar description referencing this resource</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Description</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'preceding'">
							<xsl:attribute name="title">Information about a predecessor to this resource</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Preceeding resource</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'succeeding'">
							<xsl:attribute name="title">Information about a successor to this resource</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Succeeding resource</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'original'">
							<xsl:attribute name="title">Information about the original version of this resource</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Original version</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'constituent'">
							<xsl:attribute name="title">Description of a part, subset, or supplement of this resource</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Part or supplement</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'series'">
							<xsl:attribute name="title">Series in which the resource was issued</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Series</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'otherVersion'">
							<xsl:attribute name="title">Another version of the resource; a change in intellectual content</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Version or edition</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'otherFormat'">
							<xsl:attribute name="title">The resource in another physical format, but same content</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Format</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'references'">
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>References</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'reviewOf'">
							<xsl:attribute name="title">Information about a review of this resource</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Review</xsl:value-of>
						</xsl:when>
						<xsl:when test="$groupKey = 'host'">
							<xsl:attribute name="title">Information about the item or collection this resource is part of</xsl:attribute>
							<xsl:text>Related:</xsl:text> <br/>
							<xsl:value-of>Host item or collection</xsl:value-of>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(upper-case(substring($groupKey,1,1)), substring($groupKey,2))"/>
						</xsl:otherwise>
					</xsl:choose>
				</th>
				<td>
					<xsl:if test="*[not(local-name() = 'relatedItem')]">
						<table>
							<xsl:call-template name="modsNames"/>
							<xsl:call-template name="modsTitles"/>
							<xsl:call-template name="modsField">
								<xsl:with-param name="label">Publisher</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'originInfo']/*[local-name() = 'publisher']"/>
							</xsl:call-template>
							<xsl:call-template name="modsField">
								<xsl:with-param name="label">Issuance</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'originInfo']/*[local-name() = 'issuance']"/>
							</xsl:call-template>
							<xsl:call-template name="modsField">
								<xsl:with-param name="label">Frequency</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'originInfo']/*[local-name() = 'frequency']"/>
							</xsl:call-template>
							<xsl:call-template name="modsField">
								<xsl:with-param name="label">Edition</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'originInfo']/*[local-name() = 'edition']"/>
							</xsl:call-template>
							<xsl:call-template name="modsOriginPlaces"/>
							<xsl:call-template name="modsOriginDates"/>

							<xsl:call-template name="modsGroupedField">
								<xsl:with-param name="defaultLabel">Type of Resource</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'typeOfResource']"/>
							</xsl:call-template>

							<xsl:call-template name="modsGroupedFieldWithType">
								<xsl:with-param name="defaultLabel">Genre</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'genre']"/>
							</xsl:call-template>

							<xsl:call-template name="modsLanguages"/>

							<xsl:call-template name="modsGroupedFieldWithType">
								<xsl:with-param name="defaultLabel">Table of Contents</xsl:with-param>
							</xsl:call-template>

							<xsl:call-template name="modsGroupedFieldWithType">
								<xsl:with-param name="defaultLabel">Target Audience</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'targetAudience']"/>
							</xsl:call-template>

							<xsl:call-template name="modsGroupedFieldWithType">
								<xsl:with-param name="defaultLabel">Note</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'note' and not(@type = 'admin')]"/>
							</xsl:call-template>

							<xsl:call-template name="modsGroupedFieldWithType">
								<xsl:with-param name="defaultLabel">Identifier</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'identifier']"/>
							</xsl:call-template>

							<xsl:call-template name="modsSubjects"/>
							<xsl:call-template name="modsClassifications"/>
							<xsl:call-template name="modsAbstract"/>
							<xsl:call-template name="modsLocations"/>

							<xsl:call-template name="modsGroupedFieldWithType">
								<xsl:with-param name="defaultLabel">Access Conditions</xsl:with-param>
								<xsl:with-param name="field" select="*[local-name() = 'accessCondition']"/>
							</xsl:call-template>
							<xsl:call-template name="modsParts"/>
						</table>
					</xsl:if>
				</td>
			</tr>
		</xsl:for-each-group>
	</xsl:template>

	<xsl:template match="*[local-name() = 'mods']">
		<xsl:variable name="name" select="*[local-name() = 'name']"/>
		<xsl:variable name="titleInfo" select="*[local-name() = 'titleInfo']"/>
		<xsl:if test="boolean($name) or boolean($titleInfo)">
			<table>
				<xsl:call-template name="modsNames"/>
				<xsl:call-template name="modsTitles"/>
			</table>
		</xsl:if>
		<xsl:variable name="subject" select="*[local-name() = 'subject']"/>
		<xsl:if test="boolean($subject)">
			<table>
				<xsl:call-template name="modsSubjects"/>
			</table>
		</xsl:if>
		<xsl:variable name="language" select="*[local-name() = 'language']"/>
		<xsl:variable name="typeOfResource" select="*[local-name() = 'typeOfResource']"/>
		<xsl:variable name="genre" select="*[local-name() = 'genre']"/>
		<xsl:variable name="identifier" select="*[local-name() = 'identifier']"/>
		<xsl:variable name="classification" select="*[local-name() = 'classification']"/>
		<xsl:variable name="abstract" select="*[local-name() = 'abstract' and @type='Content advice']"/>
		<xsl:variable name="targetAudience" select="*[local-name() = 'targetAudience']"/>

		<xsl:if test="boolean($language) or boolean($typeOfResource) or boolean($genre) or boolean($identifier) or boolean($classification) or boolean($targetAudience) or boolean ($abstract)">
			<table>
				<xsl:call-template name="modsLanguages"/>
				<xsl:call-template name="modsGroupedField">
					<xsl:with-param name="defaultLabel">Type of Resource</xsl:with-param>
					<xsl:with-param name="field" select="$typeOfResource"/>
				</xsl:call-template>
				<xsl:call-template name="modsGroupedFieldWithType">
					<xsl:with-param name="defaultLabel">Genre</xsl:with-param>
					<xsl:with-param name="field" select="$genre"/>
				</xsl:call-template>
				<xsl:call-template name="modsGroupedFieldWithType">
					<xsl:with-param name="defaultLabel">Identifier</xsl:with-param>
					<xsl:with-param name="field" select="$identifier"/>
				</xsl:call-template>
				<xsl:call-template name="modsClassifications"/>
				<xsl:call-template name="modsAbstract"/>
				<xsl:call-template name="modsGroupedFieldWithType">
					<xsl:with-param name="defaultLabel">Target Audience</xsl:with-param>
					<xsl:with-param name="field" select="$targetAudience"/>
				</xsl:call-template>
			</table>
		</xsl:if>


		<xsl:variable name="publisher" select="*[local-name() = 'originInfo']/*[local-name() = 'publisher']"/>
		<xsl:variable name="issuance" select="*[local-name() = 'originInfo']/*[local-name() = 'issuance']"/>
		<xsl:variable name="frequency" select="*[local-name() = 'originInfo']/*[local-name() = 'frequency']"/>
		<xsl:variable name="edition" select="*[local-name() = 'originInfo']/*[local-name() = 'edition']"/>
		<xsl:variable name="place" select="*[local-name() = 'originInfo']/*[local-name() = 'place']"/>
		<xsl:variable name="originDate" select="*[local-name() = 'originInfo']/*[contains(local-name(), 'date') or local-name() = 'copyrightDate']"/>

		<xsl:if test="boolean($publisher) or boolean($issuance) or boolean($frequency) or boolean($edition) or boolean($place) or boolean($originDate)">
			<table>
				<xsl:call-template name="modsField">
					<xsl:with-param name="label">Publisher</xsl:with-param>
					<xsl:with-param name="field" select="$publisher"/>
				</xsl:call-template>
				<xsl:call-template name="modsField">
					<xsl:with-param name="label">Issuance</xsl:with-param>
					<xsl:with-param name="field" select="$issuance"/>
				</xsl:call-template>
				<xsl:call-template name="modsField">
					<xsl:with-param name="label">Frequency</xsl:with-param>
					<xsl:with-param name="field" select="$frequency"/>
				</xsl:call-template>
				<xsl:call-template name="modsField">
					<xsl:with-param name="label">Edition</xsl:with-param>
					<xsl:with-param name="field" select="$edition"/>
				</xsl:call-template>
				<xsl:call-template name="modsOriginPlaces"/>
				<xsl:call-template name="modsOriginDates"/>
			</table>
		</xsl:if>

		<xsl:variable name="location" select="*[local-name() = 'location']"/>
		<xsl:variable name="physicalDescription" select="*[local-name() = 'physicalDescription']"/>
		<xsl:variable name="part" select="*[local-name() = 'part']"/>
		<xsl:if test="boolean($location) or boolean($physicalDescription) or boolean($part)">
			<table>
				<xsl:call-template name="modsLocations"/>
				<xsl:call-template name="modsPhysicalDescription"/>
				<xsl:call-template name="modsParts"/>
			</table>
		</xsl:if>

		<xsl:variable name="note" select="*[local-name() = 'note' and not(@type = 'admin')]"/>
		<xsl:variable name="accessCondition" select="*[local-name() = 'accessCondition' and not(child::*)]"/>
		<xsl:variable name="rightsHolder" select="*[local-name() = 'accessCondition']/*[local-name() = 'rights.holder']/*[local-name() = 'name']"/>
		<xsl:if test="boolean($note) or boolean($accessCondition) or boolean($rightsHolder)">
			<table>
				<xsl:call-template name="modsGroupedFieldWithType">
					<xsl:with-param name="defaultLabel">Note</xsl:with-param>
					<xsl:with-param name="field" select="$note"/>
				</xsl:call-template>
				<xsl:call-template name="modsGroupedFieldWithType">
					<xsl:with-param name="defaultLabel">Access Conditions</xsl:with-param>
					<xsl:with-param name="field" select="$accessCondition"/>
				</xsl:call-template>
				<xsl:call-template name="modsGroupedFieldWithType">
					<xsl:with-param name="defaultLabel">Rights Holder</xsl:with-param>
					<xsl:with-param name="field" select="$rightsHolder"/>
				</xsl:call-template>
			</table>
		</xsl:if>


		<xsl:variable name="tableOfContents" select="*[local-name() = 'tableOfContents']" />

		<xsl:if test="boolean($tableOfContents) and not ($tableOfContents[@shareable])">

			<table>
				<xsl:call-template name="modsGroupedFieldWithType">
					<xsl:with-param name="defaultLabel">Table of Contents</xsl:with-param>
					<xsl:with-param name="field" select="$tableOfContents"/>
				</xsl:call-template>

			</table>
		</xsl:if>

		<xsl:variable name="relatedItem" select="*[local-name() = 'relatedItem']"/>
		<xsl:if test="boolean($relatedItem)">
			<table>
				<xsl:call-template name="modsRelatedItems"/>
			</table>
		</xsl:if>
	</xsl:template>

	<xsl:template match="/">
		<view>
			<xsl:apply-templates select="/*[local-name() = 'mods']"/>
		</view>
	</xsl:template>

	<xsl:function name="cdrfn:substring-after-last" as="xs:string">
		<xsl:param name="value" as="xs:string?"/>
		<xsl:param name="separator" as="xs:string"/>
		<xsl:choose>
			<xsl:when test="contains($value, $separator)">
				<xsl:value-of select="cdrfn:substring-after-last(substring-after($value, $separator), $separator)" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$value" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
</xsl:stylesheet>
