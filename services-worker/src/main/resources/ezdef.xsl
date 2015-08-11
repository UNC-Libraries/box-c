<?xml version="1.0" encoding="utf-8"?>
<!--
  Fedora EZDef XSLT1.1 Stylesheet, version 1.0
  
  Input:  EZDef XML document

  Output: FOXML Service Definition for use with Fedora 3.2+
-->

<xsl:stylesheet version="1.1"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:foxml="info:fedora/fedora-system:def/foxml#">
  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="/sdef">

  <foxml:digitalObject>

    <!-- Persistent Identifier -->

    <xsl:attribute name="VERSION">1.1</xsl:attribute>
    <xsl:attribute name="PID">
      <xsl:value-of select="@pid"/>
    </xsl:attribute>

    <!-- Object Properties -->

    <foxml:objectProperties>
      <foxml:property
          NAME="info:fedora/fedora-system:def/model#state"
          VALUE="Active"/>
      <foxml:property>
        <xsl:attribute name="NAME">info:fedora/fedora-system:def/model#label</xsl:attribute>
        <xsl:attribute name="VALUE">
          <xsl:value-of select="@label"/>
        </xsl:attribute>
      </foxml:property>
    </foxml:objectProperties>

    <!-- RELS-EXT Datastream -->

    <foxml:datastream ID="RELS-EXT"
        CONTROL_GROUP="X" STATE="A" VERSIONABLE="true">
      <foxml:datastreamVersion ID="RELS-EXT1.0"
          MIMETYPE="application/rdf+xml"
          FORMAT_URI="info:fedora/fedora-system:FedoraRELSExt-1.0"
          LABEL="RDF Statements about this object">
        <foxml:xmlContent>
          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
              xmlns:fedora-model="info:fedora/fedora-system:def/model#">
            <rdf:Description>
              <xsl:attribute name="rdf:about">
                <xsl:text>info:fedora/</xsl:text>
                <xsl:value-of select="@pid"/>
              </xsl:attribute>
              <fedora-model:hasModel
                  rdf:resource="info:fedora/fedora-system:ServiceDefinition-3.0"/>
            </rdf:Description>
          </rdf:RDF>
        </foxml:xmlContent>
      </foxml:datastreamVersion>
    </foxml:datastream>

    <!-- METHODMAP Datastream -->

    <foxml:datastream ID="METHODMAP"
          CONTROL_GROUP="X" STATE="A" VERSIONABLE="true">
      <foxml:datastreamVersion ID="METHODMAP1.0"
          FORMAT_URI="info:fedora/fedora-system:FedoraSDefMethodMap-1.0"
          LABEL="Abstract Method Map" MIMETYPE="text/xml">
        <foxml:xmlContent>
          <fmm:MethodMap
                xmlns:fmm="http://fedora.comm.nsdlib.org/service/methodmap"
                name="N/A">
            <xsl:for-each select="method">
              <fmm:method>
                <xsl:attribute name="operationName">
                  <xsl:value-of select="@name"/>
                </xsl:attribute>
                <xsl:for-each select="user-input">
                  <fmm:UserInputParm>
                    <xsl:attribute name="parmName">
                      <xsl:value-of select="@name"/>
                    </xsl:attribute>
                    <xsl:attribute name="defaultValue">
                      <xsl:value-of select="@default"/>
                    </xsl:attribute>
                    <xsl:attribute name="required">
                      <xsl:choose>
                        <xsl:when test="@optional='true'">
                          <xsl:text>false</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                          <xsl:text>true</xsl:text>
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:attribute>
                    <xsl:attribute name="passBy">VALUE</xsl:attribute>
                    <xsl:if test="valid">
                      <fmm:ValidParmValues>
                        <xsl:for-each select="valid">
                          <fmm:ValidParm>
                            <xsl:attribute name="value">
                              <xsl:value-of select="@value"/>
                            </xsl:attribute>
                          </fmm:ValidParm>
                        </xsl:for-each>
                      </fmm:ValidParmValues>
                    </xsl:if>
                  </fmm:UserInputParm>
                </xsl:for-each>
              </fmm:method>
            </xsl:for-each>
          </fmm:MethodMap>
        </foxml:xmlContent>
      </foxml:datastreamVersion>
    </foxml:datastream>
  </foxml:digitalObject>

  </xsl:template>

</xsl:stylesheet>
