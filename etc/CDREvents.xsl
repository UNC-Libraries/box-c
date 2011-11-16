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
<!-- license goes here -->
<!--  -->
<xsl:stylesheet xmlns:p="info:lc/xmlns/premis-v2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml" version="2.0">

    <xsl:output method="xhtml" indent="yes"/>

    <xsl:param name="generate-html" required="no" select="no"/>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$generate-html eq 'yes'">
                <html>
                    <body>
                        <xsl:apply-templates select="p:events"/>
                    </body>
                </html>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="p:events"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="p:events">
        <table>
            <tr>
                <th>Event Type</th>
                <th>Roles of Agents</th>
                <th>Date/Time</th>
            </tr>
            <xsl:apply-templates select="p:event"/>
        </table>
    </xsl:template>

    <xsl:template match="p:event">
        <tr>
            <td>
                <xsl:value-of select="p:eventType"/>
            </td>
            <td>
                <xsl:for-each select="p:linkingAgentIdentifier">
                    <xsl:value-of select="p:linkingAgentRole"/> - <xsl:value-of
                        select="p:linkingAgentIdentifierValue"/><br/>
                </xsl:for-each>
            </td>
            <td>
                <xsl:value-of select="p:eventDateTime"/>
            </td>
        </tr>
        <xsl:if test="exists(p:eventDetail)">
            <tr>
                <td colspan="3">
                    <p>
                        <xsl:value-of select="p:eventDetail"/>
                    </p>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
