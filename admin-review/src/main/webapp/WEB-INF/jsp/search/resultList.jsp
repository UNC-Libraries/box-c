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
<div class="result_page contentarea">
	<div>
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
	
	<table class="result_table">
		<tr id="results_list_actions">
			<td colspan="7">
				<div class="left"><p><a id="select_all">Select All</a></p> <p><a id="deselect_all">Deselect All</a></p></div>
				<div class="right"><input type="Button" value="Delete" id="delete_selected" class="ajaxCallbackButton"></input>&nbsp;&nbsp;<input type="Button" value="Publish Selected" id="publish_selected" class="ajaxCallbackButton"></input><input type="Button" value="Unpublish Selected" id="unpublish_selected" class="ajaxCallbackButton"></input></div>
			</td>
		</tr>
		<tr>
			<th></th>
			<th></th>
			<th>Title</th>
			<th>Creator</th>
			<th>Added</th>
			<th>Modified</th>
			<th></th>
		</tr>
		<c:forEach items="${resultResponse.resultList}" var="metadata" varStatus="status">
			<c:set var="metadata" scope="request" value="${metadata}"/>
			<c:import url="search/resultEntry.jsp"/>
		</c:forEach>
	</table>
</div>

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