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

	<div class="result_area">
		<div>
			<div>
				<div class="container_header">
					<h2>
						<c:choose>
							<c:when test="${not empty containerBean}">
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
							</c:when>
							<c:otherwise>
								Searching
							</c:otherwise>
						</c:choose>
					</h2>
					<span id="add_menu">+ Add</span>
					<c:if test="${not empty containerBean}">
						<span id="arrange_button">Arrange</span>
						<img class="container_menu" src="/static/images/admin/gear.png"/>
					</c:if>
					<ul id="add_menu_content" class="action_menu">
						<li><a>New Folder</a></li>
						<li><a>Ingest Package</a></li>
					</ul>
					
					<c:set var="facetNodes" scope="request" value="${containerBean.path.facetNodes}"/>
					<div class="results_header_hierarchy_path">
						<c:import url="/jsp/util/pathTrail.jsp">
							<c:param name="displayHome">false</c:param>
							<c:param name="resultOperation">${sessionScope.resultOperation}</c:param>
						</c:import>
					</div>
				</div>
				
				<table class="result_table">
					<tr class="batch_actions">
						<td colspan="7">
							<div class="left"><p><a id="select_all">Select All</a></p> <p><a id="deselect_all">Deselect All</a></p></div>
							<div class="right">
								<c:if test="${containerBean == null || cdr:hasAccess(accessGroupSet, containerBean, 'purgeForever')}"><input type="Button" value="Delete" id="delete_selected" class="ajaxCallbackButton"></input>&nbsp;&nbsp;</c:if>
								<c:if test="${containerBean == null || cdr:hasAccess(accessGroupSet, containerBean, 'publish')}"><input type="Button" value="Publish Selected" id="publish_selected" class="ajaxCallbackButton"></input><input type="Button" value="Unpublish Selected" id="unpublish_selected" class="ajaxCallbackButton"></input></c:if>
							</div>
						</td>
					</tr>
					<tr>
						<th class="sort_col" data-type="index"></th>
						<th class="sort_col"></th>
						<th class="sort_col">Title</th>
						<th class="sort_col">Creator</th>
						<th class="sort_col">Added</th>
						<th class="sort_col">Modified</th>
						<th></th>
					</tr>
					<c:forEach items="${resultResponse.resultList}" var="metadata" varStatus="status">
						<c:set var="metadata" scope="request" value="${metadata}"/>
						<c:import url="search/resultEntry.jsp"/>
					</c:forEach>
				</table>
			</div>
		</div>
	</div>
</div>

<link rel="stylesheet" type="text/css" href="/static/css/admin/search_results.css" />
<script>
	var require = {
		config: {
			'resultList' : {
				'metadataObjects': ${cdr:objectToJSON(resultResponse.resultList)}
			}
		}
	};
</script>
<script type="text/javascript" src="/static/js/require.js" data-main="/static/js/admin/resultList"></script>