
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<% response.setContentType("application/xml"); %>

<xhtml:html xmlns:xforms="http://www.w3.org/2002/xforms"
xmlns:xxforms="http://orbeon.org/oxf/xml/xforms"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ev="http://www.w3.org/2001/xml-events"
xmlns:mods="http://www.loc.gov/mods/v3">

    <xhtml:head>
        <xhtml:title>MODS Editor</xhtml:title>
        <xforms:model>
 
            <xforms:instance id="modsdata">
${modsFormDAO.mods}
            </xforms:instance>            
           
     <xforms:submission id="submitmods" ref="instance('modsdata')" action="https://nagina/cdradmin/ir/admin/modsform" method="post" replace="instance" instance="modsdata">
        <xforms:action ev:event="xforms-submit">
          <xforms:send submission="send-submission" />
        </xforms:action>
      </xforms:submission>
     <xforms:submission id="send-submission" ref="instance('modsdata')" action="https://nagina/cdradmin/ir/admin/modsform" method="post" />
         </xforms:model>
        <xhtml:style type="text/css">
            h1 { display: inline; padding-right: 10px; }
            .paragraph { margin-bottom: 1em; }
            .back { display: block; margin-top: .5em }
        </xhtml:style>
    </xhtml:head>
    <xhtml:body>
        <xhtml:h1>MODS Editor</xhtml:h1>
        <xhtml:div class="paragraph">
            <xforms:group>
                <xforms:input ref="instance('modsdata')/mods:titleInfo/mods:title" xxforms:size="50">
                    <xforms:label>Title:</xforms:label><br/>
                    <xforms:help>Enter a title for the item</xforms:help>
                    <xforms:hint>Enter a title for the item</xforms:hint>
                </xforms:input>
		<br/><br/>
                <xforms:textarea ref="instance('modsdata')/mods:abstract" xxforms:cols="50">
                    <xforms:label>Abstract:</xforms:label><br/>
                    <xforms:help>Enter a description for the item</xforms:help>
                    <xforms:hint>Enter a description for the item</xforms:hint>
                </xforms:textarea>
		<br/><br/>
		<xforms:repeat nodeset="instance('modsdata')/mods:name">
			<xhtml:tr>
		             	<xforms:input ref="mods:namePart" xxforms:size="50">
  	                        <xforms:label>Name:</xforms:label>
		                <xforms:help>Enter a name for the creator</xforms:help>
		                <xforms:hint>Enter a name for the creator</xforms:hint>
                		</xforms:input>
			</xhtml:tr>
			<br/>
		</xforms:repeat>
		<br/><br/>
                <xforms:submit submission="submitmods">
                    <xforms:label>Submit Changes</xforms:label>
		</xforms:submit>
            </xforms:group>
        </xhtml:div>
        <xhtml:a class="back" href="/">Back to Administration</xhtml:a>
    </xhtml:body>
</xhtml:html>