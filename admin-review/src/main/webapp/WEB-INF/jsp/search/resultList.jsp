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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page trimDirectiveWhitespaces="true" %>
<div class="result_page contentarea">
	<c:import url="search/searchMenu.jsp"/>
	<c:if test="${not empty resultResponse.selectedContainer}">
		<c:set var="containerBean" value="${resultResponse.selectedContainer}"/>
	</c:if>

	<div class="result_area">
		<div>
			<div id="result_view">
				<div class="result_header">
					<div class="container_header">
						<c:set var="facetNodes" scope="request" value="${containerBean.path.facetNodes}"/>
						<c:set var="withPath" value="${fn:length(facetNodes) > 1}" />
						<c:if test="${withPath}">
							<div class="results_header_hierarchy_path">
								<c:import url="/jsp/util/pathTrail.jsp">
									<c:param name="displayHome">false</c:param>
									<c:param name="skipLast">true</c:param>
									<c:param name="trailingSeparator">true</c:param>
									<c:param name="resultOperation">${sessionScope.resultOperation}</c:param>
								</c:import>
							</div>
						</c:if>
						<c:choose>
							<c:when test="${not empty containerBean}">
								<span class="container_entry">
									<h2>
										<c:choose>
											<c:when test="${containerBean.resourceType == searchSettings.resourceTypeFolder}">
												<img src="/static/images/admin/type_folder.png" />
											</c:when>
											<c:when test="${containerBean.resourceType == searchSettings.resourceTypeCollection}">
												<img src="/static/images/admin/type_coll.png" />
											</c:when>
											<c:when test="${containerBean.resourceType == searchSettings.resourceTypeAggregate}">
												<img src="/static/images/admin/type_aggr.png" />
											</c:when>
										</c:choose>
										&nbsp;<c:out value="${containerBean.title}" />
									</h2>
									<span><img class="action_gear container_menu" src="/static/images/admin/gear.png"/></span>
								</span>
							</c:when>
							<c:otherwise>
								<h2>Searching</h2>
							</c:otherwise>
						</c:choose>
						<span id="add_menu" class="container_action">+ Add</span>
						<c:if test="${not empty containerBean}">
							<span id="arrange_button" class="container_action">Arrange</span>
						</c:if>
						<div class="right">
							<c:if test="${containerBean == null || cdr:hasAccess(accessGroupSet, containerBean, 'purgeForever')}">
								<span class="delete_selected ajaxCallbackButton container_action">Delete</span>&nbsp;&nbsp;
							</c:if>
							<c:if test="${containerBean == null || cdr:hasAccess(accessGroupSet, containerBean, 'publish')}">
								<span class="publish_selected ajaxCallbackButton container_action">Publish</span><span class="unpublish_selected ajaxCallbackButton container_action">Unpublish</span>
							</c:if>
						</div>
						<ul id="add_menu_content" class="action_menu">
							<li><a>New Folder</a></li>
							<li><a>Ingest Package</a></li>
						</ul>
					</div>
					<div class="left batch_actions"><p class="select_all"><input type="checkbox"/></p></div>
					<div class="right">
						<c:import url="search/navigationBar.jsp" >
							<c:param name="queryMethod" value="${queryMethod}"/>
						</c:import>
					</div>
				</div>
				<div class="result_table_scroll">
					<div class="result_table_wrap${(withPath) ? ' with_path' : ''}">
						<table class="result_table">
							<colgroup>
								<col class="narrow">
								<col class="narrow">
								<col class="itemdetails">
								<col class="creator">
								<col class="date_added">
								<col class="date_added">
								<col class="narrow">
							</colgroup>
							<thead>
								<tr class="column_headers">
									<th class="sort_col narrow" data-type="index" data-field="collection"><a></a></th>
									<th class="sort_col narrow" data-field="resourceType"><a></a></th>
									<th class="sort_col itemdetails" data-type="title" data-field="title"><a>Title</a></th>
									<th class="sort_col creator" data-field="creator"><a>Creator</a></th>
									<th class="sort_col date_added" data-field="dateAdded"><a>Added</a></th>
									<th class="sort_col date_added" data-field="dateUpdated"><a>Modified</a></th>
									<th class="narrow"><a></a></th>
								</tr>
							</thead>
							<tbody>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<link rel="stylesheet" type="text/css" href="/static/css/admin/search_results.css" />
<link rel="stylesheet" type="text/css" href="/static/css/structure_browse.css" />
<script>
	console.log("Starting " + (new Date()).getTime());
	var startTimer = (new Date()).getTime();
	var require = {
		config: {
			'resultList' : {
				'metadataObjects': ${cdr:resultsToJSON(resultResponse, accessGroupSet)},
				'pagingActive' : ${resultResponse.resultCount > fn:length(resultResponse.resultList)},
				'resultUrl' : '${currentRelativeUrl}',
				'filterParams' : '${searchStateUrl}'
				<c:if test="${not empty containerBean}">
					, 'container' : ${cdr:metadataToJSON(containerBean, accessGroupSet)}
				</c:if>
			},
		}
	};
	console.log("Loaded in " + ((new Date()).getTime() - startTimer));
</script>
<script type="text/javascript" src="/static/js/require.js" data-main="/static/js/admin/resultList"></script>