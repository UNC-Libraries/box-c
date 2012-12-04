<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<script type="text/javascript" src="/static/js/admin/AjaxCallbackButton.js"></script>
<script type="text/javascript" src="/static/js/admin/PID.js"></script>
<script>
function publishFollowup(data) {
	if (data && data > this.completeTimestamp) {
		return true;
	}
	return false;
}

function publishComplete() {
	this.element.text("Unpublish");
	this.setWorkURL("services/rest/edit/unpublish/{idPath}");
	this.options.complete = unpublishComplete;
	this.options.workLabel = "Unpublishing...";
	this.options.followupLabel = "Unpublishing....";
	this.options.parentElement.switchClass("unpublished", "published", "fast");
}

function unpublishComplete() {
	this.element.text("Publish");
	this.setWorkURL("services/rest/edit/publish/{idPath}");
	this.options.complete = publishComplete;
	this.options.workLabel = "Publishing...";
	this.options.followupLabel = "Publishing....";
	this.options.parentElement.switchClass("published", "unpublished", "fast");
}

function publishWorkDone(data) {
	if (data == null) {
		alert("Failed to change publication status for " + this.pid.pid);
		return false;
	}
	this.completeTimestamp = data.timestamp;
	return true;
}

function deleteFollowup(data) {
	if (data && data > this.completeTimestamp) {
		return true;
	}
	return false;
}

function deleteComplete() {
	this.element.remove();
	this.destroy();
}

function deleteWorkDone(data) {
	if (data == null) {
		alert("Unable to delete object " + this.pid.pid);
		return false;
	}
	this.completeTimestamp = data.timestamp;
	return true;
}

$(function() {
	var resultObjects = ${cdr:objectToJSON(resultResponse.resultList)};
	
	$(".browseitem input[type='checkbox']").prop("checked", false).click(function(event){
		$(this).parents(".browseitem").toggleClass("selected");
		event.stopPropagation();
	});
	
	$("#select_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", true);
		$(".browseitem").addClass("selected");
	});
	
	$("#deselect_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", false);
		$(".browseitem").removeClass("selected");
	});
	
	$(".browseitem").click(function(){
		var checkbox = $(this).find("input");
		checkbox.prop("checked", !checkbox.prop("checked"));
		$(this).toggleClass("selected");
	}).find('a').click(function(event){ event.stopPropagation(); });
	
	$.each(resultObjects, function(){
		var parentEl = $("#entry_" + this.id.replace(":", "\\:"));
		if ($.inArray("Unpublished", this.status) != -1) {
			parentEl.find(".publish_link").ajaxCallbackButton({
				pid: this.id,
				workLabel: "Publishing...",
				workPath: "services/rest/edit/publish/{idPath}",
				workDone: publishWorkDone,
				followupLabel: "Publishing....",
				followupPath: "services/rest/item/{idPath}/solrRecord/lastIndexed",
				followup: publishFollowup,
				complete: publishComplete,
				parentElement: parentEl
			});
		} else {
			parentEl.find(".publish_link").ajaxCallbackButton({
				pid: this.id,
				workLabel: "Unpublishing...",
				workPath: "services/rest/edit/unpublish/{idPath}",
				workDone: publishWorkDone,
				followupLabel: "Unpublishing....",
				followupPath: "services/rest/item/{idPath}/solrRecord/lastIndexed",
				followup: publishFollowup,
				complete: unpublishComplete,
				parentElement: parentEl
			});
		}
		
		parentEl.find(".delete_link").ajaxCallbackButton({
			pid: this.id,
			workLabel: "Deleting...",
			workPath: "delete/{idPath}",
			workDone: deleteWorkDone,
			followupLabel: "Cleaning up...",
			followupPath: "services/rest/item/{idPath}/solrRecord/lastIndexed",
			followup: deleteFollowup,
			complete: deleteComplete,
			parentElement: parentEl,
			confirm: true
		});
	});
});
</script>

<div class="review_page contentarea">
	<div class="contentarea">
		<h2>Reviewing items</h2>
		<c:set var="facetNodes" scope="request" value="${containerBean.path.facetNodes}"/>
		<div class="results_header_hierarchy_path">
			<c:import url="/jsp/util/pathTrail.jsp">
				<c:param name="displayHome">true</c:param>
				<c:param name="resultOperation">review</c:param>
			</c:import>
		</div>
	</div>
	
	<div id="results_list_actions">
		<div class="left"><p><a id="select_all">Select All</a></p> <p><a id="deselect_all">Deselect All</a></p></div>
		<div class="right"><input type="Button" value="Delete"/>&nbsp;&nbsp;<input type="Button" value="Publish Selected"/></div>
	</div>
	
	<div>
		<c:forEach items="${resultResponse.resultList}" var="metadata" varStatus="status">
			<c:set var="publicationStatus"> <c:if test="${!metadata.status.contains('Published')}">un</c:if>published</c:set>
			<div id="entry_${metadata.id}" class="browseitem ${publicationStatus}">
				<div class="contentarea">
					<div class="left">
						<input type="checkbox"/>
					</div>
					<ul class="itemnavigation">
						<li><a href="#" class="publish_link">
							<c:choose>
								<c:when test="${metadata.status.contains('Unpublished')}">Publish</c:when>
								<c:otherwise>Unpublish</c:otherwise>
							</c:choose>
						</a></li>
						<li><a href="describe/${metadata.pid.path}">
							<c:choose>
								<c:when test="${metadata.datastreamObjects.contains('MD_DESCRIPTIVE')}">
									Edit Description
								</c:when>
								<c:otherwise>
									Add Description
								</c:otherwise>
							</c:choose>
						</a></li>
						<li><a href="#" class="delete_link">Delete</a></li>
					</ul>
	
					<div class="itemdetails">
						<h2>
							<c:choose>
								<c:when test="${metadata.resourceType == searchSettings.resourceTypeFile}">
									<a href="/record?id=${metadata.id}" target="_new" class="has_tooltip"
										title="View details for <c:out value='${metadata.title}'/>."><c:out value='${metadata.title}'/></a>
								</c:when>
								<c:otherwise>
									<a href="list/${metadata.pid.path}" class="has_tooltip"
										title="View contents of <c:out value='${metadata.title}'/>."><c:out value='${metadata.title}'/></a>
								</c:otherwise>
							</c:choose>
							<c:if test="${metadata.datastreamObjects.contains('DATA_FILE')}">
								&nbsp;<a target="_preview" href="/indexablecontent?id=${metadata.id}&ds=DATA_FILE" class="preview">(preview ${metadata.getDatastream("DATA_FILE").extension})</a>
							</c:if>						
						</h2>
						<p>Added: <c:out value='${metadata.dateAdded}'/></p>
						<c:if test="${not empty metadata.creator}">
							<p>Creator: 
								<c:forEach var="creatorObject" items="${metadata.creator}" varStatus="creatorStatus">
									<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
								</c:forEach>
							</p>
						</c:if>
					</div>
					<div class="clear"></div>
				</div>
			</div>
		</c:forEach>
		<div class="clear"></div>
	</div>
</div>