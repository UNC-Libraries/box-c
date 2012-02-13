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
<link rel="stylesheet" href="../../css/jquery/ui/jquery-ui.css" type="text/css" media="all" />
<style>
<!--
h3.queued { 
}
h3.active {
}
h3.finished {
}
h3.failed {
}
-->
</style>
<script type="text/javascript" src="../../js/jquery/jquery.min.js"></script>
<script type="text/javascript" src="../../js/jquery/ui/jquery-ui.min.js"></script>
<script type="text/javascript">
<!--
$.ajaxSetup ({  
	  cache: false  
	});   
var ajax_load = "<img src='../../images/load.gif' alt='loading...' />";
var restUrl = "/services/rest/";
$(function() {
	$( "#servicetabs" ).tabs();
  $('#servicetabs').bind('tabsselect', function(event, ui) {
   	//alert("Hello tabselect "+ui.index);
   	switch(ui.index) {
   		case 0:
   			reloadIngestDetails();
   			break;
   		case 1:
   			//loadIndexingDetails();
   			break;
  		case 2:
   			//loadEnhancementDetails();
  			break;
  	}
  	return true;
  });
});
$(document).ready(function(){
    $.getJSON(
    		restUrl+"info",
        {},
        function(json) {
        	var serviceInfo = "<p>CDR Services Build: "+json.serviceInfo.groupId+":"+json.serviceInfo.artifactId+":"+json.serviceInfo.version+"</p>";
        	$("#serviceInfo").html(serviceInfo)
        	
        }
    );
    reloadIngestDetails();
    return false;
});
function reloadIngestDetails() {
  // load service status
  $.getJSON(
  	restUrl+"ingest",
    {},
    function(json) {
    	// idle active failedJobs activeJobs queuedJobs
  		$("#ingestActive").html(""+json.active);
  		$("#ingestIdle").html(""+json.idle);
  		$("#ingestQueuedJobs").html(""+json.queuedJobs);
  		$("#ingestActiveJobs").html(""+json.activeJobs);
  		$("#ingestFinishedJobs").html(""+json.finishedJobs);
  		$("#ingestFailedJobs").html(""+json.failedJobs);
    }
  );
  refreshJobType("finished");
  refreshJobType("failed");
  refreshJobType("active");
  refreshJobType("queued");
}

function refreshJobType(type) {
	$.getJSON(
	 	restUrl+"ingest/"+type,
	  {},
	  function(json) {
		  $("#jobs").children("tr."+type).remove();
	  	for(job in json.jobs) {
				$("#jobs").prepend(writeJob(json.jobs[job], type));
		 	}
		 	initChildRows(type);
		}
	);
}

function initChildRows(type) {
	$('tr.parent.'+type)
	  .css("cursor","pointer")
	  .attr("title","Click to expand/collapse")
	  .click(function(){
		  $('#child-'+this.id).toggle();
	});
}

function writeJob(d, type) {
	var out = "<tr class='parent "+type+"' id='a"+d.id+"'>";
	out = out + "<td>"+type+"</td><td>"+d.submitter+"</td><td>"+dateFormat(d.submissionTime)+"</td><td>"+"10/30"+"</td><td>"+d.containerPlacements[0].submittedLabel+"</td><td>"+d.message+"</td>";
	out = out + "</tr>" 
	out = out + "<tr class='child "+type+"' id='child-a"+d.id+"' style='display: none'><td colspan='6'>";
	out = out + "Job details";
	out = out + "</td></tr>";
	return out;
}

function dateFormat(timestamp) {
	var date = new Date(timestamp);
	// hours part from the timestamp
	//var hours = date.getHours();
	// minutes part from the timestamp
	//var minutes = date.getMinutes();
	// seconds part from the timestamp
	//var seconds = date.getSeconds();
	// will display time in 10:30:23 format
	//var formattedTime = hours + ':' + minutes + ':' + seconds;
	return date.toUTCString();
}

//-->
</script>
<title><fmt:message key="admin.status.heading" /></title>
<%@ include file="../../html/admincontents.html"%>
      <div id="content">
        <p class="breadcrumbs"><a href="<c:url value='/index.jsp'/>">Home</a> > Status Monitors    
        </p>
        <h2 class="fontface">Status Monitors</h2>
        
        <div id="serviceInfo"></div>
        <div id="servicetabs">
					<ul style="height: 37px">
						<li><a href="#servicetabs-1">Ingest</a></li>
						<li><a href="#servicetabs-2">Indexing</a></li>
						<li><a href="#servicetabs-3">Enhancement</a></li>
					</ul>
					<div id="servicetabs-1">
						<p>
						Active: <span id="ingestActive"></span> -	Idle: <span id="ingestIdle"></span><br />
						Queued: <span id="ingestQueuedJobs"></span> - Active: <span id="ingestActiveJobs"></span> - Finished: <span id="ingestFinishedJobs"></span>
						- Failed: <span id="ingestFailedJobs"></span>
						</p>
					<table>
					  <thead><tr><th>status</th><th>submitter</th><th>submit time</th><th>ingested</th><th>name</th><th>message</th></tr></thead>
					  <tbody id="jobs">
					  </tbody>
					</table>
					</div>
					<div id="servicetabs-2">
						<p>Indexing</p>
					</div>
					<div id="servicetabs-3">
					  <p>Enhancement</p>
					</div>
        </div>
<%@ include file="../../html/footer.html"%>
