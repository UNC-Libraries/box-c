<?xml version="1.0" encoding="utf-8"?>
<!--
  Fedora EZDep XSLT1.1 Stylesheet, version 1.0
  
  Input:  EZDep XML document
  Param:  sdef - Filename of the corresponding EZDef document
  Output: FOXML Service Deployment for use with Fedora 3.2+
-->

<xsl:stylesheet version="1.1"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:foxml="info:fedora/fedora-system:def/foxml#">
  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="ezdef"/>

  <xsl:template match="/sdep">

  <!-- Get the corresponding EZDef document -->
  <xsl:variable name="sdef" select="document($ezdef)/sdef"/>

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
                  rdf:resource="info:fedora/fedora-system:ServiceDeployment-3.0"/>
              <fedora-model:isDeploymentOf>
                <xsl:attribute name="rdf:resource">
                  <xsl:text>info:fedora/</xsl:text>
                  <xsl:value-of select="$sdef/@pid"/>
                </xsl:attribute>
              </fedora-model:isDeploymentOf>
              <fedora-model:isContractorOf>
                <xsl:attribute name="rdf:resource">
                  <xsl:text>info:fedora/</xsl:text>
                  <xsl:value-of select="@cmodel"/>
                </xsl:attribute>
              </fedora-model:isContractorOf>
            </rdf:Description>
          </rdf:RDF>
        </foxml:xmlContent>
      </foxml:datastreamVersion>
    </foxml:datastream>

    <!-- METHODMAP Datastream -->

    <foxml:datastream ID="METHODMAP"
          CONTROL_GROUP="X" STATE="A" VERSIONABLE="true">
      <foxml:datastreamVersion ID="METHODMAP1.0"
          FORMAT_URI="info:fedora/fedora-system:FedoraSDepMethodMap-1.1"
          LABEL="Deployment Method Map" MIMETYPE="text/xml">
        <foxml:xmlContent>
          <fmm:MethodMap
                xmlns:fmm="http://fedora.comm.nsdlib.org/service/methodmap"
                name="N/A">
            <xsl:for-each select="impl">
              <fmm:method>
                <xsl:variable name="method" select="@method"/>
                <xsl:attribute name="operationName">
                  <xsl:value-of select="$method"/>
                </xsl:attribute>
                <xsl:attribute name="wsdlMsgName">
                  <xsl:value-of select="$method"/>
                  <xsl:text>Request</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="wsdlMsgOutput">response</xsl:attribute>
                <xsl:for-each select="$sdef/method[@name=$method]/user-input">

                  <!-- User Input Section - Copied from EZDef Stylesheet -->
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

                <xsl:for-each select="/sdep/impl[@method=$method]/default-input">
                  <fmm:DefaultInputParm>
                    <xsl:attribute name="parmName">
                      <xsl:value-of select="@name"/>
                    </xsl:attribute>
                    <xsl:attribute name="defaultValue">
                      <xsl:value-of select="@value"/>
                    </xsl:attribute>
                    <xsl:attribute name="passBy">VALUE</xsl:attribute>
                    <xsl:attribute name="required">TRUE</xsl:attribute>
                  </fmm:DefaultInputParm>
                </xsl:for-each>

                <xsl:for-each select="/sdep/impl[@method=$method]/datastream-input">
                  <fmm:DatastreamInputParm>
                    <xsl:attribute name="parmName">
                      <xsl:value-of select="@datastream"/>
                    </xsl:attribute>
                    <xsl:attribute name="passBy">URL_REF</xsl:attribute>
                    <xsl:attribute name="required">TRUE</xsl:attribute>
                  </fmm:DatastreamInputParm>
                </xsl:for-each>

                <fmm:MethodReturnType wsdlMsgName="response" wsdlMsgTOMIME="N/A"/>

              </fmm:method>
            </xsl:for-each>
          </fmm:MethodMap>
        </foxml:xmlContent>
      </foxml:datastreamVersion>
    </foxml:datastream>

    <!-- DSINPUTSPEC Datastream -->

    <foxml:datastream ID="DSINPUTSPEC"
          CONTROL_GROUP="X" STATE="A" VERSIONABLE="true">
      <foxml:datastreamVersion ID="DSINPUTSPEC1.0"
          MIMETYPE="text/xml"
          FORMAT_URI="info:fedora/fedora-system:FedoraDSInputSpec-1.1"
          LABEL="Datastream Input Specification">
        <foxml:xmlContent>
          <fbs:DSInputSpec
              xmlns:fbs="http://fedora.comm.nsdlib.org/service/bindspec"
              label="N/A">
            <!-- For each unique datastream id... -->
            <xsl:for-each select="//datastream-input[not(@datastream=preceding::datastream-input/@datastream)]">
              <fbs:DSInput>
                <xsl:attribute name="wsdlMsgPartName">
                  <xsl:value-of select="@datastream"/>
                </xsl:attribute>
                <xsl:if test="@object">
                  <xsl:attribute name="pid">
                    <xsl:value-of select="@object"/>
                  </xsl:attribute>
                </xsl:if>
                <xsl:attribute name="DSMin">1</xsl:attribute>
                <xsl:attribute name="DSMax">1</xsl:attribute>
                <xsl:attribute name="DSOrdinality">false</xsl:attribute>
                <fbs:DSInputLabel>N/A</fbs:DSInputLabel>
                <fbs:DSMIME>N/A</fbs:DSMIME>
                <fbs:DSInputInstruction>N/A</fbs:DSInputInstruction>
              </fbs:DSInput>
            </xsl:for-each>
          </fbs:DSInputSpec>
        </foxml:xmlContent>
      </foxml:datastreamVersion>
    </foxml:datastream>

    <!-- WSDL Datastream -->

    <foxml:datastream ID="WSDL"
        CONTROL_GROUP="X" STATE="A" VERSIONABLE="true">
      <foxml:datastreamVersion ID="WSDL1.0"
          MIMETYPE="text/xml"
          FORMAT_URI="http://schemas.xmlsoap.org/wsdl/"
          LABEL="WSDL Bindings">
        <foxml:xmlContent>
          <wsdl:definitions name="N/A"
              targetNamespace="urn:thisNamespace"
              xmlns:http="http://schemas.xmlsoap.org/wsdl/http/"
              xmlns:mime="http://schemas.xmlsoap.org/wsdl/mime/"
              xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap"
              xmlns:soapenc="http://schemas.xmlsoap.org/wsdl/soap/encoding"
              xmlns:this="urn:thisNamespace"
              xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
              xmlns:xsd="http://www.w3.org/2001/XMLSchema">
            <wsdl:types>
              <xsd:schema targetNamespace="urn:thisNamespace">
                <xsd:simpleType name="inputType">
                  <xsd:restriction base="xsd:string"/>
                </xsd:simpleType>
              </xsd:schema>
            </wsdl:types>

            <xsl:for-each select="impl">
              <xsl:variable name="method" select="@method"/>
              <wsdl:message>
                <xsl:attribute name="name">
                  <xsl:value-of select="$method"/>
                  <xsl:text>Request</xsl:text>
                </xsl:attribute>
                <xsl:for-each select="$sdef/method[@name=$method]/user-input">
                  <wsdl:part>
                    <xsl:attribute name="name">
                      <xsl:value-of select="@name"/>
                    </xsl:attribute>
                    <xsl:attribute name="type">this:inputType</xsl:attribute>
                  </wsdl:part>
                </xsl:for-each>
                <xsl:for-each select="default-input">
                  <wsdl:part>
                    <xsl:attribute name="name">
                      <xsl:value-of select="@name"/>
                    </xsl:attribute>
                    <xsl:attribute name="type">this:inputType</xsl:attribute>
                  </wsdl:part>
                </xsl:for-each>
                <xsl:for-each select="datastream-input">
                  <wsdl:part>
                    <xsl:attribute name="name">
                      <xsl:value-of select="@datastream"/>
                    </xsl:attribute>
                    <xsl:attribute name="type">this:inputType</xsl:attribute>
                  </wsdl:part>
                </xsl:for-each>
              </wsdl:message>
              <wsdl:message name="response">
                <wsdl:part name="response" type="xsd:base64Binary"/>
              </wsdl:message>
            </xsl:for-each>

            <wsdl:portType name="portType">
              <xsl:for-each select="impl">
                <wsdl:operation>
                  <xsl:attribute name="name">
                    <xsl:value-of select="@method"/>
                  </xsl:attribute>
                  <wsdl:input>
                    <xsl:attribute name="message">
                      <xsl:text>this:</xsl:text>
                      <xsl:value-of select="@method"/>
                      <xsl:text>Request</xsl:text>
                    </xsl:attribute>
                  </wsdl:input>
                  <wsdl:output message="this:response"/>
                </wsdl:operation>
              </xsl:for-each>
            </wsdl:portType>

            <wsdl:service name="N/A">
              <wsdl:port binding="this:binding" name="port">
                <http:address location="LOCAL"/>
              </wsdl:port>
            </wsdl:service>

            <wsdl:binding name="binding" type="this:portType">
              <http:binding verb="GET"/>
              <xsl:for-each select="impl">
                <wsdl:operation>
                  <xsl:attribute name="name">
                    <xsl:value-of select="@method"/>
                  </xsl:attribute>
                  <http:operation>
                    <xsl:attribute name="location">
                      <xsl:value-of select="translate(normalize-space(url-pattern), ' ', '')"/>
                    </xsl:attribute>
                  </http:operation>
                  <wsdl:input>
                    <http:urlReplacement/>
                  </wsdl:input>
                  <wsdl:output>
                    <mime:content type="N/A"/>
                  </wsdl:output>
                </wsdl:operation>
              </xsl:for-each>
            </wsdl:binding>

          </wsdl:definitions>
        </foxml:xmlContent>
      </foxml:datastreamVersion>
    </foxml:datastream>
  </foxml:digitalObject>
  
  </xsl:template>

</xsl:stylesheet>
