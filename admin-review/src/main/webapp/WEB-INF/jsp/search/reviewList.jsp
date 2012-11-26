<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<script>
$(function() {
	$(".browseitem input[type='checkbox']").prop("checked", false).click(function(){
		$(this).parent().parent().parent().toggleClass("selected");
	});
	
	$("#select_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", true);
	});
	
	$("#deselect_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", false);
	});
	
	$(".itemdetails").click(function(){
		var checkbox = $(this).prev().prev().children("input");
		checkbox.prop("checked", !checkbox.prop("checked"));
		$(this).parent().parent().toggleClass("selected");
	});
	
	$(".publish_link").click(function(){
		if ($(this).html() == "Publish")
			$(this).html("Unpublish");
		else $(this).html("Publish");
		return false;
	});
});
</script>

<div class="review_page contentarea">
	<div class="contentarea">
		<h2>Reviewing items</h2>
		<c:set var="facetNodes" scope="request" value="${containerBean.path.facetNodes}"/>
		<div class="results_header_hierarchy_path">
			<c:import url="/jsp/util/hierarchyTrail.jsp" />
		</div>
	</div>
	
	<div id="results_list_actions">
		<div class="left"><p><a id="select_all">Select All</a></p> <p><a id="deselect_all">Deselect All</a></p></div>
		<div class="right"><input type="Button" value="Delete"/>&nbsp;&nbsp;<input type="Button" value="Publish Selected"/></div>
	</div>
	
	<div>
		<c:forEach items="${resultResponse.resultList}" var="metadata" varStatus="status">
			<div id="entry${metadata.id}" class="browseitem">
				<div class="contentarea">
					<div class="left">
						<input type="checkbox"/>
					</div>
					<ul class="itemnavigation">
						<li><a href="#" class="publish_link">Publish</a></li>
						<li><a href="edit_desc.html">
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
							<a href="/record?id=${metadata.id}" target="_new" class="has_tooltip"
								title="View details for <c:out value='${metadata.title}'/>."><c:out value='${metadata.title}'/></a>
							<c:if test="${metadata.datastreamObjects.contains('DATA_FILE')}">
								&nbsp;<a target="_preview" href="/indexablecontent?id=${metadata.id}&ds=DATA_FILE" class="preview">(preview pdf)</a>
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
				</div>
			</div>
		</c:forEach>
	</div>
</div>