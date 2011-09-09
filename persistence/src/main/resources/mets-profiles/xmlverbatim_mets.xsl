<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
   XML to HTML Verbatim Formatter with Syntax Highlighting
   Version 1.1
   LGPL (c) Oliver Becker, 2002-08-22
   obecker@informatik.hu-berlin.de
   Contributors: Doug Dicks, added auto-indent (parameter indent-elements)
                 for pretty-print
                 
   Modified by Tom Habing specifically for use in formatting sample METS files in the 
   aapendices of METS profiles.
-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:verb="http://informatik.hu-berlin.de/xmlverbatim"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                exclude-result-prefixes="verb">

   <xsl:output method="html" omit-xml-declaration="yes" indent="no"/>

   <xsl:param name="indent-elements" select="false()" />

   <xsl:template match="/">
      <xsl:apply-templates select="." mode="xmlverb" />
   </xsl:template>

   <!-- root -->
   <xsl:template match="/" mode="xmlverb">
      <xsl:text>&#xA;</xsl:text>
      <xsl:comment>
         <xsl:text> converted by xmlverbatim.xsl 1.1, (c) O. Becker </xsl:text>
      </xsl:comment>
      <xsl:text>&#xA;</xsl:text>
      <div class="xmlverb-default">
         <xsl:apply-templates mode="xmlverb">
            <xsl:with-param name="indent-elements" select="$indent-elements" />
         </xsl:apply-templates>
      </div>
      <xsl:text>&#xA;</xsl:text>
   </xsl:template>

   <!-- wrapper -->
   <xsl:template match="verb:wrapper">
      <xsl:apply-templates mode="xmlverb">
         <xsl:with-param name="indent-elements" select="$indent-elements" />
      </xsl:apply-templates>
   </xsl:template>

   <xsl:template match="verb:wrapper" mode="xmlverb">
      <xsl:apply-templates mode="xmlverb">
         <xsl:with-param name="indent-elements" select="$indent-elements" />
      </xsl:apply-templates>
   </xsl:template>

   <!-- element nodes -->
   <xsl:template match="*" mode="xmlverb">
      <xsl:param name="indent-elements" select="false()" />
      <xsl:param name="indent" select="''" />
      <xsl:param name="indent-increment" select="'&#xA0;&#xA0;&#xA0;'" />
      <xsl:if test="$indent-elements">
         <xsl:element name="br"/>
         <xsl:value-of select="$indent" />
      </xsl:if>
      <xsl:element name="span"><xsl:attribute name="class">xmlverb-element-container</xsl:attribute>
      <xsl:choose>
        <xsl:when test="@ID">
  				<xsl:attribute name="ID"><xsl:value-of select="@ID"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="@xlink:label">
  				<xsl:attribute name="ID"><xsl:value-of select="@xlink:label"/></xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="*"><a class="xmlverb-element-expander" href="#" onclick="ToggleChildren(this);return false;">&#x2013;</a><xsl:text>&#xA0;</xsl:text></xsl:when>
        <xsl:otherwise>
        	<xsl:text>&#xA0;&#xA0;</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>&lt;</xsl:text>
      <xsl:variable name="ns-prefix" select="substring-before(name(),':')" />
      <xsl:if test="$ns-prefix != ''">
         <xsl:element name="span"><xsl:attribute name="class">xmlverb-element-nsprefix</xsl:attribute>
            <xsl:value-of select="$ns-prefix"/>
         </xsl:element>
         <xsl:text>:</xsl:text>
      </xsl:if>
      <xsl:element name="span"><xsl:attribute name="class">xmlverb-element-name</xsl:attribute><xsl:value-of select="local-name()"/></xsl:element>
      <xsl:variable name="pns" select="../namespace::*"/>
      <xsl:if test="$pns[name()=''] and not(namespace::*[name()=''])">
         <xsl:element name="span"><xsl:attribute name="class">xmlverb-ns-name</xsl:attribute>
            <xsl:text> xmlns</xsl:text>
         </xsl:element>
         <xsl:text>=&quot;&quot;</xsl:text>
      </xsl:if>
      <xsl:for-each select="namespace::*">
         <xsl:if test="not($pns[name()=name(current()) and .=current()])">
            <xsl:call-template name="xmlverb-ns">
              <xsl:with-param name="indent-elements" select="$indent-elements"/>
              <xsl:with-param name="indent" select="concat($indent, $indent-increment)"/>
              <xsl:with-param name="indent-increment" select="$indent-increment"/>            
            </xsl:call-template>
         </xsl:if>
      </xsl:for-each>
      <xsl:for-each select="@*">
         <xsl:call-template name="xmlverb-attrs" >
           <xsl:with-param name="indent-elements" select="$indent-elements"/>
           <xsl:with-param name="indent" select="concat($indent, $indent-increment)"/>
           <xsl:with-param name="indent-increment" select="$indent-increment"/>            
           <xsl:with-param name="attr_count" select="count(.)"/>            
         </xsl:call-template>
      </xsl:for-each>
      <xsl:choose>
         <xsl:when test="node()">
            <xsl:text>&gt;</xsl:text>
            <xsl:apply-templates mode="xmlverb">
              <xsl:with-param name="indent-elements" select="$indent-elements"/>
              <xsl:with-param name="indent" select="concat($indent, $indent-increment)"/>
              <xsl:with-param name="indent-increment" select="$indent-increment"/>
            </xsl:apply-templates>
            <xsl:if test="* and $indent-elements">
               <xsl:element name="br"/>
               <xsl:value-of select="$indent" />
            </xsl:if>
            <xsl:choose>
              <xsl:when test="*">
              	<xsl:text>&#xA0;&#xA0;</xsl:text>
              </xsl:when>
            </xsl:choose>
            <xsl:text>&lt;/</xsl:text>
            <xsl:if test="$ns-prefix != ''">
               <xsl:element name="span"><xsl:attribute name="class">xmlverb-element-nsprefix</xsl:attribute>
                  <xsl:value-of select="$ns-prefix"/>
               </xsl:element>
               <xsl:text>:</xsl:text>
            </xsl:if>
            <xsl:element name="span"><xsl:attribute name="class">xmlverb-element-name</xsl:attribute>
               <xsl:value-of select="local-name()"/>
            </xsl:element>
            <xsl:text>&gt;</xsl:text>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text> /&gt;</xsl:text>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="not(parent::*)"><xsl:element name="br"/><xsl:text>&#xA;</xsl:text></xsl:if>
      </xsl:element>
   </xsl:template>

   <!-- attribute nodes -->
   <xsl:template name="xmlverb-attrs">
      <xsl:param name="indent-elements" select="false()" />
      <xsl:param name="indent" select="''" />
      <xsl:param name="indent-increment" select="'&#xA0;&#xA0;&#xA0;'" />
      <xsl:param name="attr_count" select="'1'" />
      <xsl:text> </xsl:text>
      <xsl:if test="$indent-elements">
         <xsl:element name="br"/>
         <xsl:value-of select="concat($indent,$indent-increment)" />
      </xsl:if>
      <xsl:element name="span"><xsl:attribute name="class">xmlverb-attr-name</xsl:attribute>
         <xsl:value-of select="name()"/>
      </xsl:element>
      <xsl:text>=&quot;</xsl:text>
      <xsl:element name="span"><xsl:attribute name="class">xmlverb-attr-content</xsl:attribute>
      	<xsl:choose>
      		<xsl:when test="name()='ADMID' or name()='DMDID' or name()='FILEID' or name()='xlink:from' or name()='xlink:to'">
      			<xsl:call-template name="IDREFS"/>
      		</xsl:when>
      		<xsl:when test="string-length(normalize-space(.))>80">
            <xsl:call-template name="html-replace-entities">
               <xsl:with-param name="text" select="concat(substring(normalize-space(.),1,80),'...')" />
               <xsl:with-param name="attrs" select="true()" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="html-replace-entities">
               <xsl:with-param name="text" select="normalize-space(.)" />
               <xsl:with-param name="attrs" select="true()" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:element>
      <xsl:text>&quot;</xsl:text>
   </xsl:template>

	<xsl:template name="IDREFS" >
		<xsl:param name="IDREFS" select="normalize-space(.)"/>
		<xsl:choose>
			<xsl:when test="contains($IDREFS,' ')">
				<xsl:call-template name="IDREFS">
					<xsl:with-param name="IDREFS"
						select="normalize-space(substring-before($IDREFS,' '))"/>
				</xsl:call-template>
				<xsl:call-template name="IDREFS">
					<xsl:with-param name="IDREFS"
						select="normalize-space(substring-after($IDREFS,' '))"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<a>
					<xsl:attribute name="href">#<xsl:value-of select="$IDREFS"/></xsl:attribute><xsl:attribute name="onclick">ToggleUpId('<xsl:value-of select="$IDREFS"/>')</xsl:attribute>
					<xsl:value-of select="$IDREFS"/>
				</a>
				<xsl:text>&#160;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

   <!-- namespace nodes -->
   <xsl:template name="xmlverb-ns">
      <xsl:param name="indent-elements" select="false()" />
      <xsl:param name="indent" select="''" />
      <xsl:param name="indent-increment" select="'&#xA0;&#xA0;&#xA0;'" />
      <xsl:if test="name()!='xml'">
         <xsl:element name="span"><xsl:attribute name="class">xmlverb-ns-name</xsl:attribute>
            <xsl:text> xmlns</xsl:text>
            <xsl:if test="name()!=''">
               <xsl:text>:</xsl:text>
            </xsl:if>
            <xsl:value-of select="name()"/>
         </xsl:element>
         <xsl:text>=&quot;</xsl:text>
         <xsl:element name="span"><xsl:attribute name="class">xmlverb-ns-uri</xsl:attribute>
            <xsl:value-of select="."/>
         </xsl:element>
         <xsl:text>&quot;</xsl:text>
         <xsl:if test="$indent-elements">
            <xsl:element name="br"/>
            <xsl:value-of select="$indent" />
         </xsl:if>
      </xsl:if>
   </xsl:template>

   <!-- text nodes -->
   <xsl:template match="text()" mode="xmlverb">
      <xsl:element name="span"><xsl:attribute name="class">xmlverb-text</xsl:attribute>
      	<xsl:choose>
      		<xsl:when test="string-length(normalize-space(.))>80">
            <xsl:call-template name="preformatted-output">
               <xsl:with-param name="text">
                  <xsl:call-template name="html-replace-entities">
                     <xsl:with-param name="text" select="concat(substring(normalize-space(.),1,80),'...')" />
                  </xsl:call-template>
               </xsl:with-param>
            </xsl:call-template>
      		</xsl:when>
      		<xsl:otherwise>
            <xsl:call-template name="preformatted-output">
               <xsl:with-param name="text">
                  <xsl:call-template name="html-replace-entities">
                     <xsl:with-param name="text" select="normalize-space(.)" />
                  </xsl:call-template>
               </xsl:with-param>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:element>
   </xsl:template>

   <!-- comments -->
   <xsl:template match="comment()" mode="xmlverb">
      <xsl:param name="indent-elements" select="false()" />
      <xsl:param name="indent" select="''" />
      <xsl:param name="indent-increment" select="'&#xA0;&#xA0;&#xA0;'" />
      <xsl:if test="$indent-elements">
         <xsl:element name="br"/>
         <xsl:value-of select="$indent" />
      </xsl:if>
      <xsl:text>&lt;!--</xsl:text>
      <xsl:element name="span"><xsl:attribute name="class">xmlverb-comment</xsl:attribute>
         <xsl:call-template name="preformatted-output">
            <xsl:with-param name="text" select="." />
         </xsl:call-template>
      </xsl:element>
      <xsl:text>--&gt;</xsl:text>
      <xsl:if test="not(parent::*)"><xsl:element name="br"/><xsl:text>&#xA;</xsl:text></xsl:if>
   </xsl:template>

   <!-- processing instructions -->
   <xsl:template match="processing-instruction()" mode="xmlverb">
      <xsl:text>&lt;?</xsl:text>
      <xsl:element name="span"><xsl:attribute name="class">xmlverb-pi-name</xsl:attribute>
         <xsl:value-of select="name()"/>
      </xsl:element>
      <xsl:if test=".!=''">
         <xsl:text> </xsl:text>
         <xsl:element name="span"><xsl:attribute name="class">xmlverb-pi-content</xsl:attribute>
            <xsl:value-of select="."/>
         </xsl:element>
      </xsl:if>
      <xsl:text>?&gt;</xsl:text>
      <xsl:if test="not(parent::*)"><xsl:element name="br"/><xsl:text>&#xA;</xsl:text></xsl:if>
   </xsl:template>


   <!-- =========================================================== -->
   <!--                    Procedures / Functions                   -->
   <!-- =========================================================== -->

   <!-- generate entities by replacing &, ", < and > in $text -->
   <xsl:template name="html-replace-entities">
      <xsl:param name="text" />
      <xsl:param name="attrs" />
      <xsl:variable name="tmp">
         <xsl:call-template name="replace-substring">
            <xsl:with-param name="from" select="'&gt;'" />
            <xsl:with-param name="to" select="'&amp;gt;'" />
            <xsl:with-param name="value">
               <xsl:call-template name="replace-substring">
                  <xsl:with-param name="from" select="'&lt;'" />
                  <xsl:with-param name="to" select="'&amp;lt;'" />
                  <xsl:with-param name="value">
                     <xsl:call-template name="replace-substring">
                        <xsl:with-param name="from" 
                                        select="'&amp;'" />
                        <xsl:with-param name="to" 
                                        select="'&amp;amp;'" />
                        <xsl:with-param name="value" 
                                        select="$text" />
                     </xsl:call-template>
                  </xsl:with-param>
               </xsl:call-template>
            </xsl:with-param>
         </xsl:call-template>
      </xsl:variable>
      <xsl:choose>
         <!-- $text is an attribute value -->
         <xsl:when test="$attrs">
            <xsl:call-template name="replace-substring">
               <xsl:with-param name="from" select="'&#xA;'" />
               <xsl:with-param name="to" select="'&amp;#xA;'" />
               <xsl:with-param name="value">
                  <xsl:call-template name="replace-substring">
                     <xsl:with-param name="from" 
                                     select="'&quot;'" />
                     <xsl:with-param name="to" 
                                     select="'&amp;quot;'" />
                     <xsl:with-param name="value" select="$tmp" />
                  </xsl:call-template>
               </xsl:with-param>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$tmp" />
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- replace in $value substring $from with $to -->
   <xsl:template name="replace-substring">
      <xsl:param name="value" />
      <xsl:param name="from" />
      <xsl:param name="to" />
      <xsl:choose>
         <xsl:when test="contains($value,$from)">
            <xsl:value-of select="substring-before($value,$from)" />
            <xsl:value-of select="$to" />
            <xsl:call-template name="replace-substring">
               <xsl:with-param name="value" 
                               select="substring-after($value,$from)" />
               <xsl:with-param name="from" select="$from" />
               <xsl:with-param name="to" select="$to" />
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$value" />
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- preformatted output: space as &nbsp;, tab as 8 &nbsp; nl as <br> -->
   <xsl:template name="preformatted-output">
      <xsl:param name="text" />
      <xsl:call-template name="output-nl">
         <xsl:with-param name="text">
            <xsl:call-template name="replace-substring">
               <xsl:with-param name="value" select="translate($text,' ','&#xA0;')" />
               <xsl:with-param name="from" select="'&#9;'" />
               <xsl:with-param name="to" select="'&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;'" />
            </xsl:call-template>
         </xsl:with-param>
      </xsl:call-template>
   </xsl:template>

   <!-- output nl as <br> -->
   <xsl:template name="output-nl">
      <xsl:param name="text" />
      <xsl:choose>
         <xsl:when test="contains($text,'&#xA;')">
            <xsl:value-of select="substring-before($text,'&#xA;')" />
            <xsl:element name="br"/>
            <xsl:text>&#xA;</xsl:text>
            <xsl:call-template name="output-nl">
               <xsl:with-param name="text" 
                               select="substring-after($text,'&#xA;')" />
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$text" />
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

</xsl:stylesheet>
