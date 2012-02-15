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
tr.finished > td {
  background-color: Beige;
  background-image: none;
}
tr.active > td {
  background-color: Lavender;
  background-image: none;
}
tr.failed > td {
  background-color: LavenderBlush;
  background-image: none;
}
tr.narrow > th {
  min-width: 0px;
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
var autorefresh = null;
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
    refresh(new Array("status","active"), 5);
    refresh(new Array("failed","queued","finished"), 30);
    return false;
});
function reloadIngestStatus() {
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
  		$("#ingestRefreshed").html(new Date().toTimeString());
    }
  );
}

function refresh(types, seconds) {
	var ts = " new Array(";
	var first = true;
	for(type in types) {
		if("status"==types[type]) {
			reloadIngestStatus();
		} else {
			refreshJobType(types[type]);
		}
		if(first) {
			first = false;
		} else {
			ts += ",";
		}
		ts += " '"+types[type]+"'";
	}
	ts += ") ";
	autorefresh=setTimeout("refresh("+ts+", "+seconds+");",1000*seconds);
}

function refreshJobType(type) {
	$.getJSON(
	 	restUrl+"ingest/"+type,
	  {},
	  function(json) {
		  $("#jobs").children("tr."+type).remove();
	  	for(job in json.jobs) {
				$("#jobs").children("tr#"+type+"-end").after(writeJob(json.jobs[job], type));
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
	out = out + "<td>"+type+"</td><td>"+d.submitter+"</td><td>"+dateFormat(d.submissionTime)+"</td><td>"+d.worked+"/"+d.size+"</td><td>"+d.containerPlacements[0].submittedLabel+"</td><td>"+d.message+"</td>";
	out = out + "</tr>" 
	out = out + "<tr class='child "+type+"' id='child-a"+d.id+"' style='display: none'><td colspan='6'>";
	if(d.startTime != null) out += "<p>Started: "+dateFormat(new Date(d.startTime))+"</p>";
	if(d.failedTime != null) out += "<p>Failed: "+dateFormat(new Date(d.failedTime))+"</p>";
	if(d.finishedTime != null) out += "<p>Finished: "+dateFormat(new Date(d.finishedTime))+"</p>";
	if(d.startTime != null) {
		if(d.finishedTime != null) {
			out += "<p>Elapsed: "+(d.finishedTime-d.startTime)/1000+" seconds</p>";	
		} else if(d.failedTime != null) {
			out += "<p>Elapsed: "+(d.failedTime-d.startTime)/1000+" seconds</p>";
		} else {
			out += "<p>Elapsed: "+(Date.now()-d.startTime)/1000+" seconds</p>";
		}
	}	
	if(d.depositId != null) out += "<p>Deposit ID: "+d.depositId+"</p>";
	if(d.error != null) out += "<h3>Error Log</h3><p>"+d.error+"</p>";
	out = out + "</td></tr>";
	return out;
}

function dateFormat(timestamp) {
	var date = new Date(timestamp);
	// hours part from the timestamp
	var hours = date.getHours();
	var ampm = "AM";
	if(hours >= 12) {
		ampm = "PM";
	}
	if(hours > 12) {
		hours = hours -12;
	}
	if(hours == 0) {
		hours = 12;
	}
	// minutes part from the timestamp
	var minutes = date.getMinutes();
	if(minutes < 10) {
		minutes = "0"+minutes;
	}
	// seconds part from the timestamp
	//var seconds = date.getSeconds();
	// will display time in 10:30:23 format
	var formattedTime = (date.getMonth()+1)+"/"+date.getDate()+" "+hours+':'+minutes+' '+ampm;
	//return date.toUTCString();
	return formattedTime;
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
						<table>
						  <tr class="narrow"><th>Active</th><th>Idle</th><th>Queued</th><th>Active</th><th>Failed</th><th>Finished<sup>*</sup></th><th>Refreshed</th></tr>
						  <tr>
						    <td><span id="ingestActive"></span></td>
						    <td><span id="ingestIdle"></span></td>
						    <td><span id="ingestQueuedJobs"></span></td>
						    <td><span id="ingestActiveJobs"></span></td>
						    <td><span id="ingestFailedJobs"></span></td>
						    <td><span id="ingestFinishedJobs"></span></td>
						    <td><span id="ingestRefreshed"></span></td>
						  </tr>
						</table>
					  <p>Batch Ingest Jobs by Status</p>
					  <table>
					    <thead><tr class="narrow"><th>status</th><th>submitter</th><th>submit time &uarr;</th><th>ingested</th><th>first object</th><th>message</th></tr></thead>
					    <tbody id="jobs">
					      <tr id="queued-end" style="display:none"><td></td></tr>
					      <tr id="active-end" style="display:none"><td></td></tr>
					      <tr id="finished-end" style="display:none"><td></td></tr>
					      <tr id="failed-end" style="display:none"><td></td></tr>
					    </tbody>
					  </table>
					  <p>* Finished ingest jobs are removed after two days.</p>
					</div>
					<div id="servicetabs-2">
						<p>Indexing</p>
					</div>
					<div id="servicetabs-3">
					  <p>Enhancement</p>
					</div>
        </div>
<%@ include file="../../html/footer.html"%>
