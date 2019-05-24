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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<c:if test="${not empty briefObject && cdr:contains(briefObject.status, 'Deleted') || cdr:contains(briefObject.status, 'Parent Deleted')}">
	<c:set var="isDeleted" value="deleted" scope="page"/>
</c:if>
<c:if test="${not empty briefObject && (not cdr:hasPatronRoleForPublicGroup(briefObject) || not empty briefObject.activeEmbargo)}">
	<c:set var="isProtected" value="protected" scope="page"/>
</c:if>

<div class="content-wrap full_record ${isDeleted}${' '}${isProtected}">
<c:import url="fullRecord/navigationBar.jsp" />
<c:choose>
	<c:when test="${requestScope.listAccess == true}">
		<c:import url="fullRecord/listAccessRecord.jsp" />
	</c:when>
	<c:when test="${briefObject.resourceType == searchSettings.resourceTypeCollection || briefObject.resourceType == searchSettings.resourceTypeFolder}">
		<script>
			var require = {
				config: {
					'containerRecord' : {
						'containerSettings' : ${cdr:objectToJSON(containerSettings)}
					}
				}
			};
		</script>
		<c:import url="fullRecord/collectionRecord.jsp" />
		<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/containerRecord"></script>
	</c:when>
	<c:when test="${briefObject.resourceType == searchSettings.resourceTypeAggregate}">
		<c:import url="fullRecord/aggregateRecord.jsp" />
		<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/fullRecord"></script>
	</c:when>
	<c:when test="${briefObject.resourceType == searchSettings.resourceTypeFile}">
		<c:import url="fullRecord/fileRecord.jsp" />
		<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/fullRecord"></script>
	</c:when>
</c:choose>
</div>
<%-- Add record visit event to the google analytics commands to be run later --%>
<c:set var="collectionName"><c:out value='${briefObject.parentCollectionName}' /></c:set>
<c:if test="${empty collectionName && briefObject.resourceType == 'Collection'}">
	<c:set var="collectionName"><c:out value='${briefObject.title}' /></c:set>
</c:if>
<c:if test="${empty collectionName}">
	<c:set var="collectionName" value="(no collection)" />
</c:if>
<c:set var="gaCommands" scope="request">${gaCommands} ga('unc.send', 'event', '${collectionName}', 'record', '<c:out value="${briefObject.title}|${briefObject.pid}" />');</c:set>