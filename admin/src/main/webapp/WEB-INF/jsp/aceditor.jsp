<%--

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

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="header.jsp"%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon" />
<title><fmt:message key="updateobject.heading"/></title>
<LINK REL=StyleSheet HREF="<c:url value='/css/unc_styles.css'/>"
	TYPE="text/css" />
<LINK REL=StyleSheet HREF="<c:url value='/css/ir_style.css'/>"
	TYPE="text/css" />
<LINK REL=StyleSheet HREF="<c:url value='/css/xml_editor_style.css'/>"
	TYPE="text/css" />

  <script src="http://code.jquery.com/jquery-latest.js"></script>
  <script src="/cdradmin/js/xmleditor.js"></script>
  <script src="/cdradmin/js/acattributes.js"></script>
  <script src="/cdradmin/js/acelements.js"></script>

</head>
<body>

<form id="myForm">
<div id="inherit">
</div>
<br/>
    <div>
        <input type="button" id="inheritAdd" value="Add inherit" />
    </div>
<br/>
<div id="grant"/>
</div>
<br/>
    <div>
        <input type="button" id="grantAdd" value="Add grant" />
    </div>
<div id="embargo"/>
</div>
<br/>
    <div>
        <input type="button" id="embargoAdd" value="Add embargo" />
    </div>
</form>

<script>
$(document).ready(function()
{

window.MyVariables = {};
window.MyVariables.xml = {};

// set up button callbacks
$('#embargoAdd').click(function() { addEmbargoElements(); });
$('#inheritAdd').click(function() { addInheritElements(); });
$('#grantAdd').click(function() { addGrantElements(); });

$('#sendXML').click(function() { sendXML(); });

<%String p=(String)request.getSession().getAttribute("pid"); %>
  $.ajax({
    type: "GET",
    url: "/cdradmin/ir/admin/ajax/ac",
    data: ({pid : '<%= p %>'}),
    dataType: "xml",
    success: function(xml) { setupEditor(xml); },
           error: function(jqXHR, exception) {
            if (jqXHR.status === 0) {
                alert('Not connect.\n Verify Network.');
            } else if (jqXHR.status == 404) {
                alert('Requested page not found. [404]');
            } else if (jqXHR.status == 500) {
                alert('Internal Server Error [500].');
            } else if (exception === 'parsererror') {
                alert('Requested JSON parse failed.');
            } else if (exception === 'timeout') {
                alert('Time out error.');
            } else if (exception === 'abort') {
                alert('Ajax request aborted.');
            } else {
                alert('Uncaught Error.\n' + jqXHR.responseText);
            }
        }
  });

}); // document ready



function addGrantElements() {

	var num = $('#grant > .grantInstance').length; 
	
	if(num == undefined) num = 0;

	createElement(Grant, $(window.MyVariables.xml).find("accessControl"), num, '#grant', 2);
}
function addInheritElements() {

	var num = $('#inherit > .inheritInstance').length; 
	
	if(num == undefined) num = 0;

	createElement(Inherit, $(window.MyVariables.xml).find("accessControl"), num, '#inherit', 2);
}
function addEmbargoElements() {

	var num = $('#embargo > .embargoInstance').length; 
	
	if(num == undefined) num = 0;

	createElement(Embargo, $(window.MyVariables.xml).find("accessControl"), num, '#embargo', 2);
}


function setupEditor(xml)
{
  // make XML accessible to rest of code
  window.MyVariables.xml = xml;

  // preload the title
  $(window.MyVariables.xml).find('accessControl').children("grant").each(function() { addGrantElements(); });
  $(window.MyVariables.xml).find('accessControl').children("inherit").each(function() { addInheritElements(); });
  $(window.MyVariables.xml).find('accessControl').children("embargo").each(function() { addEmbargoElements(); });

}

// Send XML back to be stored
function sendXML() {
	if( !window.XMLSerializer ){
	   window.XMLSerializer = function(){};

	   window.XMLSerializer.prototype.serializeToString = function( XMLObject ){
	      return XMLObject.xml || '';
	   };
	}

	// convert XML DOM to string
	var xmlString = xml2Str(window.MyVariables.xml);

        // console.log($.isXMLDoc(xmlString)); 
        $.ajax({
           url: '/cdradmin/ir/admin/updateac',
           contentType: "application/xml",
           type: "POST",  
           data: xmlString, 
           success: function(response){
             alert(response);
           },
           error: function(jqXHR, exception) {
            if (jqXHR.status === 0) {
                alert('Not connect.\n Verify Network.');
            } else if (jqXHR.status == 404) {
                alert('Requested page not found. [404]');
            } else if (jqXHR.status == 500) {
                alert('Internal Server Error [500].');
            } else if (exception === 'parsererror') {
                alert('Requested JSON parse failed.');
            } else if (exception === 'timeout') {
                alert('Time out error.');
            } else if (exception === 'abort') {
                alert('Ajax request aborted.');
            } else {
                alert('Uncaught Error.\n' + jqXHR.responseText);
            }
        }
    });
}


function xml2Str(xmlNode)
{
  try {
    // Gecko-based browsers, Safari, Opera.
    return (new XMLSerializer()).serializeToString(xmlNode);
  }
  catch (e) {
    try {
      // Internet Explorer.
      return xmlNode.xml;
    }
    catch (e)
    {//Strange Browser ??
     alert('Xmlserializer not supported');
    }
  }
  return false;
}


</script>
</body>