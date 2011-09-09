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
<xsl:stylesheet version="2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:p="info:lc/xmlns/premis-v2">

<xsl:output encoding="UTF-8" method="xhtml" indent="yes"/>

<xsl:template match="/">
	<xsl:apply-templates select="transaction">
		<xsl:sort select="@start" data-type="text"/>
	</xsl:apply-templates>
</xsl:template>

<xsl:template match="transaction">
	<div>
		<div class="summary">
			<span class="start"><xsl:value-of select="@start"/></span>
			- <span class="user"><xsl:value-of select="@user"/></span>
			- <span class="method"><xsl:value-of select="@method"/></span>
			- in progress
			- <xsl:value-of select="count(p:events/p:event)"/> events
		</div>
		<div><h3>Message</h3>
			<p><xsl:value-of select="message"/></p>
		</div>		
		<div class="objects"><h3>Linked Agents</h3>
			<xsl:for-each-group select="p:events/p:event" group-by="p:linkingAgentIdentifier/p:linkingAgentIdentifierValue">
				<xsl:sort select="current-grouping-key()"/>
				<xsl:value-of select="current-grouping-key()"/><br/>
			</xsl:for-each-group>
		</div>
		<div class="objects"><h3>Linked Objects</h3>
			<xsl:for-each-group select="p:events/p:event" group-by="p:linkingObjectIdentifier/p:linkingObjectIdentifierValue">
				<xsl:sort select="current-grouping-key()"/>
				<xsl:value-of select="current-grouping-key()"/> Events: <xsl:value-of select="count(current-group())"/><br />
			</xsl:for-each-group>
		</div>
	</div>
</xsl:template>

</xsl:stylesheet>