$.ajaxSetup ({  
	  cache: false  
	});   
var ajax_load = "<img src='../../images/load.gif' alt='loading...' />";
var servicesUrl = "/cdradmin/ir/services";
var restUrl = servicesUrl + "/rest/";
var autorefresh = null;
var activeView = "";
var selectedIDs = [];

$(function() {
	$('#servicetabs').bind('tabsselect', function(event, ui) {
		//alert("Hello tabselect "+ui.index);
		switch(ui.index) {
			case 0:
				activateIngestStatus();
				break;
			case 1:
				//loadIndexingDetails();
				break;
			case 2:
				activateEnhancementStatus();
				break;
			case 3:
				activateCatchupStatus();
				break;
		}
		return true;
	});
	$('#servicetabs').tabs({selected: null});
	$("#servicetabs").tabs("select", 0);
	
	$(".expandDetails").click(function(){
		$("#enhancementData").toggleClass("expandedDetails");
		if ($("#enhancementData").hasClass("expandedDetails")) {
			$(".expandDetails").html("(collapse)");
		} else {
			$(".expandDetails").html("(expand)");
		}
	});
	
	$(".refreshDetailsButton").click(function(){
		enhancementLoadDetails($(".refreshDetailsButton").data("messageID"), $(".refreshDetailsButton").data("type"));
	});
});

function refresh(types, seconds, viewName, refreshFunction) {
	if (activeView != viewName)
		return;
	for(type in types) {
		refreshFunction(viewName, types[type]);
	}
	autorefresh=setTimeout(function(){refresh(types, seconds, viewName, refreshFunction)},1000*seconds);
}

function activateIngestStatus(){
	view = "ingest";
	activeView = view;
	refresh(new Array("active"), 5, view, refreshJobType);
	refresh(new Array("status"), 5, view, reloadIngestStatus);
	refresh(new Array("failed","queued","finished"), 30, view, refreshJobType);
}

function activateEnhancementStatus(){
	view = "enhancement";
	activeView = view;
	refresh(new Array("active"), 5, "enhancement", refreshJobType);
	refresh(new Array("status"), 5, view, reloadEnhancementStatus);
	refresh(new Array("blocked","queued"), 10, view, refreshJobType);
	refresh(new Array("failed"), 10, view, refreshFailedJobType);
}

function activateCatchupStatus(){
	view = "catchup";
	activeView = view;
	refresh(new Array("status"), 60, view, reloadCatchupStatus);
	refresh(new Array("TechnicalMetadataEnhancementService", "ImageEnhancementService", "ThumbnailEnhancementService"), 45, view, refreshCatchup);
}

function activateIndexingStatus(){
	view = "indexing";
	activeView = view;
	//refresh(new Array("active"), 5, "enhancement", refreshJobType);
	refresh(new Array("status"), 5, view, reloadEnhancementStatus);
	//refresh(new Array("blocked","queued", "failed"), 10, view, refreshJobType);
}

function reloadIngestStatus(viewName, type) {
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

function reloadEnhancementStatus(viewName, type) {
	// load service status
	$.getJSON(
		restUrl+"enhancement",
		{},
		function(json) {
			// idle active failedJobs activeJobs queuedJobs
			$("#enhancementActive").html(""+json.active);
			$("#enhancementIdle").html(""+json.idle);
			$("#enhancementQueuedJobs").html(""+json.queuedJobs);
			$("#enhancementActiveJobs").html(""+json.activeJobs);
			$("#enhancementFailedJobs").html(""+json.failedJobs);
			$("#enhancementRefreshed").html(new Date().toTimeString());
		}
	);
}

function reloadCatchupStatus(viewName, type) {
	var loadIconRow = $("#catchupStatus .loadJobs");
	if (loadIconRow.length > 0){
		loadIconRow.show();
	} else loadIconRow = null;
	// load service status
	$.getJSON(
		restUrl+"catchup",
		{},
		function(json) {
			if (loadIconRow != null)
				loadIconRow.remove();
			// idle active failedJobs activeJobs queuedJobs
			$("#catchupActive").html(""+json.active);
			$("#catchupEnabled").html(""+json.enabled);
			$("#catchupItemsProcessed").html(""+json.itemsProcessed);
			$("#catchupItemsProcessedThisSession").html(""+json.itemsProcessedThisSession);
			var serviceOut = "<ul>";
			for (unqualifiedName in json.services) {
				var service = json.services[unqualifiedName];
				serviceOut += "<li>" + service.serviceName + " (" + service.count + ")</li>"; 
			}
			serviceOut += "</ul>";
			$("#catchupServices").html(serviceOut);
			$("#catchupRefreshed").html(new Date().toTimeString());
		}
	);
}

function refreshJobType(viewName, type, subType) {
	subType = subType || "";
	$.getJSON(
	 	restUrl+viewName+"/"+type,
		{},
		function(json) {
			$("#" + viewName + "Jobs").children("tr."+type).remove();
			for(job in json.jobs) {
				$("#" + viewName + "Jobs tr."+type+"-end").after(window[viewName+subType+"WriteJob"](json.jobs[job], type));
		 	}
			window[viewName+"InitDetails"](type);
		}
	);
}

function refreshFailedJobType(viewName, type) {
	refreshJobType(viewName, type, "Failed");
}

function ingestInitDetails(type) {
	$('tr.parent.'+type)
		.attr("title","Click to expand/collapse")
		.click(function(){
			var target = $('#child-'+this.id);
			var id = this.id.substring(1);
			target.toggle();
			// Toggle display of the details and store this state
			if (target.is(":visible")){
				selectedIDs.push(id);
			} else {
				selectedIDs.splice($.inArray(id, selectedIDs), 1);
			}
	});
}

function enhancementLoadDetails(messageID, type) {
	$.get(restUrl + "enhancement/job/" + messageID + "?type=" + type, function(data){
		if (data == null || data.type == null) {
			$(".detailsContent").removeClass("active").removeClass("finished").removeClass("failed");
			$("#enhancementDetails").html("");
			$(".refreshDetailsButton").data("messageID");
			$(".refreshDetailsButton").data("type");
		} else if (data.type == 'failed') {
			var details = "<span>Status:</span>" + data.type + " (last refreshed " + new Date().toTimeString() + ")<br/>";
			details += "<span>Message:</span>" + data.id + "<br/>";
			details += "<span>Target:</span>" + data.targetPID + "<br/>"; 
			details += "<span>Queued Timestamp:</span>" + dateFormat(data.queuedTimestamp, true) + "<br/>";
			details += "<span>Action:</span><br/>" + data.action + "<br/>";
			details += "<span>Failed service:</span><br/>" + data.serviceName + "<br/>";
			if (data.stackTrace) {
				details += "<span>Stack trace:</span><br/>";
				details += "<pre>" + data.stackTrace + "</pre>";
			}
			$(".detailsContent").removeClass("active").removeClass("finished");
			$(".detailsContent").addClass("failed");
			$("#enhancementDetails").html(details);
			$(".refreshDetailsButton").data("messageID", messageID);
			$(".refreshDetailsButton").data("type", data.type);
		} else if (data.type == 'queued'){
			var details = "<span>Status:</span>" + data.type + " (last refreshed " + new Date().toTimeString() + ")<br/>";
			details += "<span>Message:</span>" + data.id + "<br/>";
			details += "<span>Target:</span>" + data.targetPID + "<br/>";
			details += "<span>Queued Timestamp:</span>" + dateFormat(data.queuedTimestamp, true) + "<br/>";
			details += "<span>Action:</span>" + data.action + "<br/>";
			if ("xml" in data.uris) {
				$.get(servicesUrl + data.uris.xml, function(data){
					var xmlstr = data.xml ? data.xml : (new XMLSerializer()).serializeToString(data);
					var xmlElement = $("<pre class='xmlBody'></pre>").text(xmlstr);
					$("#enhancementDetails").append("<span>Message Body:</span><br/>")
						.append(xmlElement);
				});
			}
			$(".detailsContent").removeClass("active").removeClass("finished").removeClass("failed");
			$("#enhancementDetails").html(details);
			$(".refreshDetailsButton").data("messageID", messageID);
			$(".refreshDetailsButton").data("type", data.type);
		}
	});
}

function enhancementInitDetails(type) {
	$('tr.parent.' + type).attr("title","Click for message details")
		.click(function(){
			var messageID = this.id.substring(1);
			enhancementLoadDetails(messageID, type);
		});
}

function ingestWriteJob(d, type) {
	var out = "<tr class='parent "+type+" detailsLink' id='a"+d.id+"'>";
	out = out + "<td>"+type+"</td><td>"+d.submitter+"</td><td>"+dateFormat(d.submissionTime)+"</td><td>"+d.worked+"/"+d.size+"</td><td>"+d.containerPlacements[0].submittedLabel+"</td><td>"+d.message+"</td>";
	out = out + "</tr>";
	out = out + "<tr class='child "+type+"' id='child-a"+d.id+"'";
	if ($.inArray(d.id, selectedIDs) == -1){
		out += " style='display: none'";
	}
	out += "><td colspan='6'>";
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
	if(d.error != null) out += "<h3>Error Log</h3><pre class='stacktrace'>"+d.error+"</pre>";
	out = out + "</td></tr>";
	return out;
}

function refreshCatchup(viewName, type) {
	var loadIconRow = $("#catchupJobs .loadJobs");
	if (loadIconRow.length > 0){
		loadIconRow.show();
	} else loadIconRow = null;
	$.getJSON(
	 	restUrl+viewName+"/candidates/"+type,
		{},
		function(json) {
			if (loadIconRow != null)
				loadIconRow.remove();
			$("#" + viewName + "Jobs").children("tr."+type).remove();
			for(jobCount in json[type]) {
				job = json[type][jobCount];
				var out = "<tr class='parent "+type+"' id='a"+job.pid+"'>";
				out += "<td>"+type+"</td><td>"+job.pid+"</td>";
				out += "</tr>";
				$("#" + viewName + "Jobs tr."+type+"-end").after(out);
		 	}
		}
	);
}

function enhancementWriteJob(d, type) {
	var out = "<tr class='parent "+type+" detailsLink' id='a"+d.id+"'>";
	out += "<td>"+type+"</td><td>"+d.targetPID+"</td><td><ul>";
	for (filteredService in d.filteredServices){
		out += "<li>" + d.filteredServices[filteredService] + "</li>";
	}
	out += "</ul></td></tr>";
	return out;
}

function enhancementFailedWriteJob(d, type) {
	var out = "";
	for (messageID in d.messageIDs){
		out += "<tr class='parent "+type+" detailsLink' id='a"+d.messageIDs[messageID]+"'>";
		out += "<td>"+type+"</td><td>"+d.id+"</td><td>";
		for (failedService in d.failedServices){
			className = d.failedServices[failedService];
			lastIndex = className.lastIndexOf(".");
			if (lastIndex != -1)
				className = className.substring(lastIndex+1);
			out += className + "<br/>";
		}
		out += "</td></tr>";
	}
	
	return out;
}

function dateFormat(timestamp, showYear) {
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
	var formattedTime = (date.getMonth()+1)+"/"+date.getDate();
	if (showYear == true)
		formattedTime += "/"+date.getFullYear();
	formattedTime += " "+hours+':'+minutes+' '+ampm;
	//return date.toUTCString();
	return formattedTime;
}