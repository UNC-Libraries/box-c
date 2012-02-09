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
<%@ include file="include.jsp"%>
<%@ include file="../../html/head.html"%>
<title><fmt:message key="admin.status.heading" /></title>
<%@ include file="../../html/admincontents.html"%>
<script type="text/javascript" src="../../js/jquery/jquery.min.js"></script>
<script type="text/javascript">
<!--
$.ajaxSetup ({  
	  cache: false  
	});   
var ajax_load = "<img src='../../images/load.gif' alt='loading...' />";
var jsonUrl = "/services/rest/info";
$(document).ready(function(){  
    // var q = $("#q").val();
    $("#result").html(ajax_load);
    $.getJSON(
        jsonUrl,
        {},
        function(json) {
        	var serviceInfo = "<p>CDR Services Build: "+json.serviceInfo.groupId+":"+json.serviceInfo.artifactId+":"+json.serviceInfo.version+"</p>";
        	$("#serviceInfo").html(serviceInfo)
          var serviceTabs = "<p><ul><li><a href=\"" + json.serviceInfo.uris.ingestServiceUri + "\">Ingest</a></li></ul></p>";
          $("#tabs").html(serviceTabs);
        }
    );
    return false;
}); 
//-->
</script>
      <div id="content">            
        <p class="breadcrumbs"><a href="<c:url value='/index.jsp'/>">Home</a> > Status Monitors    
        </p>
        <h2 class="fontface">Status Monitors</h2>
        
        <div id="serviceInfo"></div>
        <ul id="tabs">
          <li id="ingestService">Ingest</li>
          <li id="indexingService">Indexing</li>
          <li id="enhancementService">Enhancement</li>
        </ul>
<%@ include file="../../html/footer.html"%>
