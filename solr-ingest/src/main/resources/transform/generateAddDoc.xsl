<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
		xmlns:cdr-fn="http://cdr.lib.unc.edu/"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:foxml="info:fedora/fedora-system:def/foxml#"
		xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		xmlns:dcterms="http://purl.org/dc/terms/"
		xmlns:owl="http://www.w3.org/2002/07/owl#"
		xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:ns5="http://cdr.unc.edu/definitions/1.0/base-model.xml#"
		xmlns:ns6="info:fedora/fedora-system:def/model#"
		xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
		xmlns:mods="http://www.loc.gov/mods/v3"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
		xmlns:fn="http://www.w3.org/2005/xpath-functions">
		
	<xsl:import href="/xsl/languageMappings.xsl"/>
	<xsl:import href="cdr-functions.xsl" />
	<xsl:output  method="xml" omit-xml-declaration="yes" indent="no"/>
	
	<xsl:variable name="collectionType">Collection</xsl:variable>
	<xsl:variable name="folderType">Folder</xsl:variable>
	<xsl:variable name="fileType">File</xsl:variable>
	<xsl:variable name="aggregateType">Aggregate</xsl:variable>
	
	
	<xsl:template match="mods:subject" mode="modsSubject">
		<xsl:if test="boolean(self::node()/*)">
			<xsl:for-each select="self::node()/*">
				<field name="subject">
					<xsl:value-of select="."/>
				</field>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="mods:language/mods:languageTerm">
		<field name="language">
			<xsl:choose>
				<xsl:when test="@authority = 'iso639-2b'">
					<xsl:call-template name="ISO639-2btoName">
						<xsl:with-param name="langCode" select="." />
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="."/>
				</xsl:otherwise>
			</xsl:choose>
		</field>
	</xsl:template>
	
	<xsl:template name="formatDate">
		<xsl:param name="dateNode" />
		<xsl:variable name="dateTrim" select="normalize-space($dateNode)"/>
		<xsl:variable name="formattedDate">
			<xsl:choose>
				<xsl:when test="$dateNode/@encoding = 'iso8601' or not(boolean($dateNode/@encoding))">
					<xsl:choose>
						<xsl:when test="string-length($dateTrim) = 4"><xsl:value-of select="$dateTrim"/>-01-01T00:00:00Z</xsl:when>
						<xsl:when test="contains($dateTrim, '-')">
							<xsl:choose>
								<xsl:when test="string-length($dateTrim) = 7"><xsl:value-of select="substring($dateTrim, 1, 4)"/>-<xsl:value-of select="substring($dateTrim, 6, 2)"/>-01T00:00:00Z</xsl:when>
								<xsl:when test="string-length($dateTrim) = 10"><xsl:value-of select="substring($dateTrim, 1, 4)"/>-<xsl:value-of select="substring($dateTrim, 6, 2)"/>-<xsl:value-of select="substring($dateTrim, 9, 2)"/>T00:00:00Z</xsl:when>
								<xsl:when test="string-length($dateTrim) > 10"><xsl:value-of select="$dateTrim"/></xsl:when>
							</xsl:choose>
						</xsl:when>
						<xsl:otherwise>
							<xsl:choose>
								<xsl:when test="string-length($dateTrim) = 6"><xsl:value-of select="substring($dateTrim, 1, 4)"/>-<xsl:value-of select="substring($dateTrim, 5, 2)"/>-01T00:00:00Z</xsl:when>
								<xsl:when test="string-length($dateTrim) >= 8"><xsl:value-of select="substring($dateTrim, 1, 4)"/>-<xsl:value-of select="substring($dateTrim, 5, 2)"/>-<xsl:value-of select="substring($dateTrim, 7, 2)"/>T00:00:00Z</xsl:when>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
			</xsl:choose>
		</xsl:variable>
		<xsl:if test="matches($formattedDate, '\d{2}\-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z')"><xsl:value-of select="$formattedDate"/></xsl:if>
	</xsl:template>
	
	<xsl:template match="mods:name/mods:affiliation" mode="exact">
		<field name="department"><xsl:value-of select="."/></field>
	</xsl:template>
	
	<xsl:template match="mods:name/mods:affiliation" mode="split">
		<xsl:variable name="trimmedDept" select="fn:normalize-space()"/>
		<xsl:if test="fn:contains($trimmedDept, 'University of North Carolina') or fn:contains($trimmedDept, 'Chapel Hill, North Carolina')">
			<xsl:variable name="deptFirstSeg" select="fn:replace(fn:substring-before($trimmedDept, ','), 'University of North Carolina', '')"/>
			<xsl:if test="fn:starts-with($trimmedDept, 'Department of ') or fn:starts-with($trimmedDept, 'School of ') or fn:contains($trimmedDept, 'Center') or fn:ends-with($trimmedDept, ' Department') ">
				<xsl:variable name="deptRemoveDeptPrefix" select="fn:replace(fn:replace(fn:replace($deptFirstSeg, 'Department of ', ''), 'School of ', ''), ' Department', '')"/>
				<field name="department"><xsl:value-of select="$deptRemoveDeptPrefix"/></field>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="mods:genre">
		<field name="keyword"><xsl:value-of select="."/></field>
	</xsl:template>
	
	<xsl:template match="mods:typeOfResource">
		<field name="keyword"><xsl:value-of select="."/></field>
	</xsl:template>
	
	<xsl:template match="mods:physicalDescription/mods:note">
		<field name="keyword"><xsl:value-of select="."/></field>
	</xsl:template>
	
	<xsl:template match="mods:note">
		<field name="keyword"><xsl:value-of select="."/></field>
	</xsl:template>
	
	<xsl:template match="mods:relatedItem/*/*">
		<field name="keyword"><xsl:value-of select="."/></field>
	</xsl:template>
	
	<xsl:template match="/view-inputs/foxml:digitalObject/foxml:datastream/@ID" mode="datastreamField">
		<field name="datastream"><xsl:value-of select="."/></field>
	</xsl:template>

	
	<xsl:template match="/view-inputs/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#createdDate']/@VALUE">
		<field name="dateAdded"><xsl:value-of select="normalize-space()"/></field>
	</xsl:template>
	
	<xsl:template match="/view-inputs/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/view#lastModifiedDate']/@VALUE">
		<field name="dateUpdated"><xsl:value-of select="normalize-space()"/></field>
	</xsl:template>
	
	<xsl:template name="identifierKeyword">
		<xsl:param name="uuid" /> 
		<xsl:param name="node" />
		<xsl:for-each select="$node">
			<xsl:if test="text() != $uuid">
				<field name="keyword"><xsl:value-of select="text()"/></field>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template match="mods:mods" mode="modsDocument">
		<xsl:param name="uuid" />
		<xsl:param name="departmentSplitting" />
		<!-- Get titles -->
		<xsl:variable name="plainTitle" select="mods:titleInfo[not(@type) or @type != 'alternative'][1]/mods:title"></xsl:variable>
		<xsl:choose>
			<xsl:when test="boolean(plainTitle)">
				<field name="title"><xsl:value-of select="plainTitle"/></field>
			</xsl:when>
			<xsl:otherwise>
				<field name="title"><xsl:value-of select="mods:titleInfo[1]/*"/></field>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:for-each select="mods:titleInfo/*">
			<field name="otherTitle"><xsl:value-of select="text()"/></field>
		</xsl:for-each>
		
		<!-- Get names --> 
		<xsl:variable name="creatorRole" select="mods:name/mods:role/mods:roleTerm[text() = 'creator']"/>
		<xsl:choose>
			<xsl:when test="boolean($creatorRole)">
				<xsl:for-each select="$creatorRole/ancestor-or-self::node()/ancestor-or-self::node()/mods:namePart">
					<field name="creator"><xsl:value-of select="text()"/></field>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="noRole" select="mods:name[not(mods:role)]"/>
				<xsl:if test="boolean($noRole)">
					<xsl:for-each select="$noRole/mods:namePart">
						<field name="creator"><xsl:value-of select="text()"/></field>
					</xsl:for-each>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:for-each select="mods:name/mods:namePart">
			<field name="name"><xsl:value-of select="text()"/></field>
		</xsl:for-each>
		
		<field name="abstract"><xsl:value-of select="mods:abstract[1]"/></field>
		
		<xsl:apply-templates select="mods:subject" mode="modsSubject"/>
		<xsl:choose>
			<xsl:when test="$departmentSplitting = true()">
				<xsl:apply-templates select="mods:name/mods:affiliation" mode="split"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="mods:name/mods:affiliation" mode="exact"/>
			</xsl:otherwise>
		</xsl:choose>
		
		
		<xsl:apply-templates select="mods:language/mods:languageTerm"/>

		<!-- Get date created -->
		<xsl:variable name="formattedDate">
			<xsl:choose>
				<xsl:when test="boolean(mods:originInfo/mods:dateCreated)">
					<xsl:call-template name="formatDate">
						<xsl:with-param name="dateNode" select="mods:originInfo/mods:dateCreated" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="boolean(mods:originInfo/mods:dateIssued)">
					<xsl:call-template name="formatDate">
						<xsl:with-param name="dateNode" select="mods:originInfo/mods:dateIssued" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="boolean(mods:originInfo/mods:dateCaptured)">
					<xsl:call-template name="formatDate">
						<xsl:with-param name="dateNode" select="mods:originInfo/mods:dateCaptured" />
					</xsl:call-template>
				</xsl:when>
			</xsl:choose>
		</xsl:variable>
		<xsl:if test="$formattedDate != ''">
			<field name="dateCreated"><xsl:value-of select="$formattedDate"/></field>
		</xsl:if>
		
		<!-- Get keywords -->
		<xsl:apply-templates select="mods:typeOfResource"/>
		<xsl:apply-templates select="mods:genre"/>
		<xsl:apply-templates select="mods:relatedItem/*/*"/>
		<xsl:apply-templates select="mods:physicalDescription/mods:note"/>
		<xsl:apply-templates select="mods:note"/>
		<xsl:apply-templates select="mods:classification"/>
		<xsl:call-template name="identifierKeyword">
			<xsl:with-param name="uuid" select="$uuid"/>
			<xsl:with-param name="node" select="mods:identifier"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="oai_dc:dc">
		<xsl:param name="uuid" />
		<!-- Get titles -->
		<xsl:for-each select="dc:title">
			<xsl:choose>
				<xsl:when test="position() = 1">
					<field name="title"><xsl:value-of select="."/></field>
				</xsl:when>
				<xsl:otherwise>
					<field name="otherTitle"><xsl:value-of select="."/></field>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
		
		<!-- Get names -->
		<xsl:for-each select="dc:creator">
			<field name="creator"><xsl:value-of select="text()"/></field>
		</xsl:for-each>
		<xsl:for-each select="dc:contributor|dc:creator">
			<field name="name"><xsl:value-of select="."/></field>
		</xsl:for-each>
		
		<xsl:if test="boolean(dc:language)">
			<field name="language">
				<xsl:call-template name="ISO639-2btoName">
					<xsl:with-param name="langCode" select="dc:language[1]" />
				</xsl:call-template>
			</field>
		</xsl:if>
		
		<field name="abstract"><xsl:value-of select="dc:description[1]"/></field>
		
		<xsl:for-each select="dc:subject">
			<xsl:if test="boolean(text())">
				<field name="subject"><xsl:value-of select="."/></field>
			</xsl:if>
		</xsl:for-each>
		
		<!-- get keywords -->
		<xsl:for-each select="dc:type">
			<field name="keyword"><xsl:value-of select="."/></field>
		</xsl:for-each>
		<xsl:call-template name="identifierKeyword">
			<xsl:with-param name="uuid" select="$uuid"/>
			<xsl:with-param name="node" select="dc:identifier"/>
		</xsl:call-template>
		
	</xsl:template>
	
	<xsl:function name="cdr-fn:getMostRecentDate">
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
				<xsl:value-of select="cdr-fn:getMostRecentDate($newHighDate, $nextSibling)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$newHighDate"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	<xsl:template match="/view-inputs/permissions/rights/permitMetadataRead">
		<field name="recordAccess"><xsl:value-of select="text()"/></field>
	</xsl:template>
	<xsl:template match="/view-inputs/permissions/rights/permitDerivativesRead">
		<field name="surrogateAccess"><xsl:value-of select="text()"/></field>
	</xsl:template>
	<xsl:template match="/view-inputs/permissions/rights/permitOriginalsRead">
		<field name="fileAccess"><xsl:value-of select="text()"/></field>
	</xsl:template>
	
	<xsl:template name="fileMetadata">
		<xsl:param name="digitalObject"/>
	
		<xsl:variable name="mimeType">
			<xsl:variable name="fitsMimeType" select="lower-case($digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/ns5:hasSourceMimeType)"/>
			<xsl:choose>
				<xsl:when test="boolean($fitsMimeType)"><xsl:value-of select="$fitsMimeType"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="lower-case($digitalObject/foxml:datastream[@ID='DATA_FILE']/foxml:datastreamVersion[last()]/@MIMETYPE)"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="altIds" select="$digitalObject/foxml:datastream[@ID='DATA_FILE']/foxml:datastreamVersion[last()]/@ALT_IDS"/>
		<xsl:variable name="fileName">
			<xsl:if test="boolean($altIds)">
				<xsl:value-of select="cdr-fn:substringAfterLast($altIds, '/')"/>
			</xsl:if>
		</xsl:variable>
		<xsl:if test="boolean($fileName) and $fileName != ''">
			<field name="keyword"><xsl:value-of select="$fileName"/></field>
		</xsl:if>
		
		<!-- Get the best guess at the file extension -->
		<xsl:variable name="fileExtension" select="cdr-fn:getFileExtension($mimeType, $fileName)"/>
		
		<!-- Generate the content type facet using file extension and mimetype -->
		<xsl:variable name="contentGeneralType" select="cdr-fn:getContentType($mimeType, $fileExtension)" />
		<field name="contentType">1,<xsl:value-of select="$contentGeneralType"/></field>
		<field name="contentType">2,<xsl:value-of select="$fileExtension"/>,<xsl:value-of select="$fileExtension"/></field>
		
		<!-- Get the file size, first from the FITS generated relation, and then from the file itself if fits not present -->
		<xsl:variable name="fitsFileSize" select="$digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/ns5:hasSourceFileSize"/>
		<xsl:choose>
			<xsl:when test="boolean($fitsFileSize)">
				<field name="filesize"><xsl:value-of select="$fitsFileSize"/></field>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="filesize" select="$digitalObject/foxml:datastream[@ID='DATA_FILE']/foxml:datastreamVersion[last()]/@SIZE"/>
				<xsl:if test="$filesize != '-1'">
					<field name="filesize"><xsl:value-of select="$digitalObject/foxml:datastream[@ID='DATA_FILE']/foxml:datastreamVersion[last()]/@SIZE"/></field>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="/">
		<add>
			<doc>
				<xsl:variable name="uuid" select="/view-inputs/foxml:digitalObject/@PID"></xsl:variable>
				<field name="id"><xsl:value-of select="$uuid"/></field>
				<xsl:variable name="resourceType">
					<xsl:choose>
						<xsl:when test="boolean(/view-inputs/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/ns6:hasModel[@rdf:resource = 'info:fedora/cdr-model:Collection']/@rdf:resource)"><xsl:value-of select="$collectionType"/></xsl:when>
						<xsl:when test="boolean(/view-inputs/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/ns6:hasModel[@rdf:resource = 'info:fedora/cdr-model:AggregateWork']/@rdf:resource)"><xsl:value-of select="$aggregateType"/></xsl:when>
						<xsl:when test="boolean(/view-inputs/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/ns6:hasModel[@rdf:resource = 'info:fedora/cdr-model:Container']/@rdf:resource)"><xsl:value-of select="$folderType"/></xsl:when>
						<xsl:when test="boolean(/view-inputs/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/ns6:hasModel[@rdf:resource = 'info:fedora/cdr-model:Simple']/@rdf:resource)"><xsl:value-of select="$fileType"/></xsl:when>
					</xsl:choose>
				</xsl:variable>
				<field name="resourceType"><xsl:value-of select="$resourceType"/></field>
				<field name="resourceTypeSort">
					<xsl:choose>
						<xsl:when test="$resourceType = $collectionType">01</xsl:when>
						<xsl:when test="$resourceType = $folderType">02</xsl:when>
						<xsl:when test="$resourceType = $fileType or $resourceType = $aggregateType">03</xsl:when>
					</xsl:choose>
				</field>
				
				<field name="parentCollection">
						<xsl:if test="boolean(/view-inputs/parentCollection)">
							<xsl:value-of select="/view-inputs/parentCollection"/>
						</xsl:if>
				</field>
				
				<xsl:variable name="pathObjects" select="/view-inputs/path/object"/>
				<xsl:choose>
					<xsl:when test="count($pathObjects) > 2">
						<xsl:choose>
							<xsl:when test="count($pathObjects) > 3">
								<xsl:for-each select="$pathObjects">
									<xsl:if test="position() > 2 and position() != last()">
										<field name="ancestorPath"><xsl:value-of select="position() - 2" />,<xsl:value-of select="@pid" />,<xsl:value-of select="normalize-space(@label)" /></field>
									</xsl:if>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<field name="ancestorPath"></field>
							</xsl:otherwise>
						</xsl:choose>
						<field name="ancestorNames">
							<xsl:for-each select="/view-inputs/path/object/@label">
								<xsl:if test="position() > 2 and (position() != last() or $resourceType != $fileType)">/<xsl:value-of select="."/></xsl:if>
							</xsl:for-each>
						</field>	
					</xsl:when>
					<xsl:otherwise>
						<field name="ancestorPath"></field>
						<field name="ancestorNames">/</field>
					</xsl:otherwise>
				</xsl:choose>
				
				<xsl:variable name="departmentSplitting">
					<xsl:choose>
						<xsl:when test="count($pathObjects) > 3 and fn:contains($pathObjects[3]/@slug, 'BioMed_Central')"><xsl:value-of select="true()"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="false()"/></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				
				
				<!-- Access control -->
				<xsl:apply-templates select="/view-inputs/permissions/rights/permitMetadataRead"/>
				<xsl:apply-templates select="/view-inputs/permissions/rights/permitDerivativesRead"/>
				<xsl:apply-templates select="/view-inputs/permissions/rights/permitOriginalsRead"/>
				
				<xsl:variable name="displayOrder" select="/view-inputs/order"/>
				<xsl:choose>
					<xsl:when test="not(boolean($displayOrder)) or matches($displayOrder, '[^\d]+')">
						 <field name="displayOrder">0</field>
					</xsl:when>
					<xsl:otherwise>
						<field name="displayOrder"><xsl:value-of select="$displayOrder"/></field>
					</xsl:otherwise>
				</xsl:choose>
				
				<!-- File specific metadata, including file name, size, content type -->
				<xsl:variable name="defaultWebObject" select="/view-inputs/defaultWebObject/foxml:digitalObject"/>
				<xsl:choose>
					<xsl:when test="boolean($defaultWebObject)">
						<xsl:call-template name="fileMetadata">
							<xsl:with-param name="digitalObject" select="$defaultWebObject"/>
						</xsl:call-template>
						<xsl:variable name="defaultWebObjectPID" select="$defaultWebObject/@PID"/>
						<xsl:for-each select="$defaultWebObject/foxml:datastream">
							<field name="datastream"><xsl:value-of select="$defaultWebObjectPID"/>/<xsl:value-of select="./@ID"/></field>
						</xsl:for-each>
					</xsl:when>
					<xsl:when test="$resourceType = $fileType">
						<xsl:call-template name="fileMetadata">
							<xsl:with-param name="digitalObject" select="/view-inputs/foxml:digitalObject"/>
						</xsl:call-template>
					</xsl:when>
				</xsl:choose>
				
				<xsl:apply-templates select="/view-inputs/foxml:digitalObject/foxml:datastream/@ID" mode="datastreamField"/>
				
				<xsl:apply-templates select="/view-inputs/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/view#lastModifiedDate']/@VALUE"/>
				<xsl:apply-templates select="/view-inputs/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#createdDate']/@VALUE"/>
				
				<xsl:variable name="hasMods" select="boolean(/view-inputs/foxml:digitalObject/foxml:datastream[@ID='MD_DESCRIPTIVE']/foxml:datastreamVersion/foxml:xmlContent/mods:mods)"/>
				<xsl:variable name="hasDC" select="boolean(/view-inputs/foxml:digitalObject/foxml:datastream[@ID='DC']/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc)"/>
				
				<xsl:choose>
					<xsl:when test="$hasMods">
						<xsl:variable name="mostRecentDate" select="cdr-fn:getMostRecentDate('', /view-inputs/foxml:digitalObject/foxml:datastream[@ID='MD_DESCRIPTIVE']/foxml:datastreamVersion[1])"/>
						<xsl:apply-templates select="/view-inputs/foxml:digitalObject/foxml:datastream[@ID='MD_DESCRIPTIVE']/foxml:datastreamVersion[@CREATED = $mostRecentDate]/foxml:xmlContent/mods:mods[1]" mode="modsDocument">
							<xsl:with-param name="departmentSplitting" select="$departmentSplitting"></xsl:with-param>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$hasDC">
						<xsl:variable name="mostRecentDate" select="cdr-fn:getMostRecentDate('', /view-inputs/foxml:digitalObject/foxml:datastream[@ID='DC']/foxml:datastreamVersion)"/>
						<xsl:apply-templates select="/view-inputs/foxml:digitalObject/foxml:datastream[@ID='DC']/foxml:datastreamVersion[@CREATED = $mostRecentDate]/foxml:xmlContent/oai_dc:dc[1]">
							<xsl:with-param name="uuid" select="$uuid"></xsl:with-param>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<field name="title"><xsl:value-of select="/view-inputs/path/object[last()]/@label"/></field>
					</xsl:otherwise>
				</xsl:choose>
			</doc>
		</add>
	</xsl:template>
</xsl:stylesheet>