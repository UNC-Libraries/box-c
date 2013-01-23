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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<div class="review_page contentarea">
	<div class="contentarea">
		<c:choose>
			<c:when test="${sessionScope.resultOperation == 'review'}">
				<h2>Reviewing items</h2>
			</c:when>
			<c:otherwise>
				<h2>Listing contents</h2>
			</c:otherwise>
		</c:choose>
		<c:set var="facetNodes" scope="request" value="${containerBean.path.facetNodes}"/>
		<div class="results_header_hierarchy_path">
			<c:import url="/jsp/util/pathTrail.jsp">
				<c:param name="displayHome">false</c:param>
				<c:param name="resultOperation">${sessionScope.resultOperation}</c:param>
			</c:import>
		</div>
	</div>
	
	<div id="results_list_actions">
		<div class="left"><p><a id="select_all">Select All</a></p> <p><a id="deselect_all">Deselect All</a></p></div>
		<div class="right"><input type="Button" value="Delete"/>&nbsp;&nbsp;<input id="publish_selected" type="Button" value="Publish Selected"/><input id="unpublish_selected" type="Button" value="Unpublish Selected"/></div>
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
									<c:set var="childCount" value="${metadata.countMap.child}"/>
									<span class="searchitem_container_count">
										<c:choose>
											<c:when test="${not empty childCount}">
												(${childCount} item<c:if test="${childCount != 1}">s</c:if>)
											</c:when>
											<c:otherwise>(0 items)</c:otherwise>
										</c:choose>
									</span>
								</c:otherwise>
							</c:choose>
							<c:if test="${metadata.datastreamObjects.contains('DATA_FILE')}">
								&nbsp;<a target="_preview" href="${cdr:getDatastreamUrl(metadata, 'DATA_FILE', fedoraUtil)}" class="preview">(preview ${metadata.getDatastream("DATA_FILE").extension})</a>
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
<script>
	var require = {
		config: {
			'reviewList' : {
				'test' : 'awesome',
				'metadataObjects': ${cdr:objectToJSON(resultResponse.resultList)}
			}
		}
	};
</script>
<script type="text/javascript" src="/static/js/require.js" data-main="/static/js/admin/reviewList"></script>