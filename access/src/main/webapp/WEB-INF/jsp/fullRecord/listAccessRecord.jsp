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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> 
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<c:set var="defaultWebData" value="${briefObject.defaultWebData}"/>
<c:set var="defaultWebObjectID">
	<c:forEach items="${briefObject.datastreamObjects}" var="datastream">
		<c:if test="${datastream.name == 'DATA_FILE'}">
			<c:out value="${datastream.owner.pid}"/>
		</c:if>
	</c:forEach>
</c:set>

<div class="onecol full_record_top">
	<div class="contentarea">
		<c:set var="thumbnailObject" value="${briefObject}" scope="request" />
		<c:import url="common/thumbnail.jsp">
			<c:param name="target" value="record" />
			<c:param name="size" value="large" />
		</c:import>
		
		<div class="collinfo">
			<h2><c:out value="${briefObject.title}" /></h2>
			
			<ul class="pipe_list smaller">
				<c:if test="${not empty briefObject.creator}">
					<li>
						<span class="bold">Creator<c:if test="${fn:length(briefObject.creator) > 1}">s</c:if>:</span> 
						<c:forEach var="creatorObject" items="${briefObject.creator}" varStatus="creatorStatus">
							<c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
						</c:forEach>
					</li>
				</c:if>
				<c:if test="${not empty briefObject.parentCollection && briefObject.ancestorPathFacet.highestTier > 0}">
					<li>
						<c:url var="parentUrl" scope="page" value="record/${briefObject.parentCollection}" />
						<span class="bold">Collection:</span> 
						<a href="<c:out value='${parentUrl}' />"><c:out value="${briefObject.parentCollectionName}"/></a>
					</li>
				</c:if>
			</ul>
			
			<ul class="pipe_list smaller">
				<c:if test="${defaultWebData != null}">
					<li><span class="bold">File Type:</span> <c:out value="${defaultWebData.extension}" /></li>
					<li><c:if test="${briefObject.filesizeSort != -1}">  | <span class="bold">${searchSettings.searchFieldLabels['FILESIZE']}:</span> <c:out value="${cdr:formatFilesize(briefObject.filesizeSort, 1)}"/></c:if></li>
					
				</c:if>
				<c:if test="${not empty briefObject.dateAdded}"><li><span class="bold">${searchSettings.searchFieldLabels['DATE_ADDED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateAdded}" /></li></c:if>
				<c:if test="${not empty briefObject.dateCreated}"><li><span class="bold">${searchSettings.searchFieldLabels['DATE_CREATED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateCreated}" /></li></c:if>
			</ul>
			<br class="clear"/>
			<p class="smaller">
				<c:set var="facetNodes" scope="request" value="${briefObject.path.facetNodes}"/>
				<span class="bold">Path:&nbsp;</span>
				<c:import url="/jsp/util/pathTrail.jsp">
					<c:param name="hideLast"><c:choose><c:when test="${briefObject.resourceType == searchSettings.resourceTypeFile}">true</c:when><c:otherwise>false</c:otherwise></c:choose></c:param>
					<c:param name="linkLast">true</c:param>
					<c:param name="queryPath">list</c:param>
				</c:import>
			</p>
			
			<jsp:useBean id="now" class="java.util.Date" />
			<c:choose>
				<c:when test="${not empty embargoDate}">
					<div class="noaction left">
						Available after <fmt:formatDate value="${embargoDate}" pattern="d MMMM, yyyy"/>
					</div>
				</c:when>
				<c:otherwise>
					<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
				<c:param name="target" value="${currentAbsoluteUrl}" />
			</c:url>
			<c:if test="${empty pageContext.request.remoteUser}">
				<div class="actionlink left">
					<a href="<c:out value='${loginUrl}' />">Log in</a>
				</div>
			</c:if>
					<div class="actionlink left">
						<a href="${contactUrl}&requestpid=${briefObject.pid.pid}">Request Access</a>
					</div>
				</c:otherwise>
			</c:choose>
			
			<c:if test="${briefObject['abstractText'] != null}">
				<p class="clear">
					<c:out value="${briefObject['abstractText']}" />
				</p>
			</c:if>
		</div>
	</div>
</div>

<div class="onecol shadowtop">
	<div class="contentarea">
		<h2>Access by request</h2>
		<p>
			To access the contents of this item you will need to
			<c:if test="${empty pageContext.request.remoteUser}">
				<c:url var="loginUrl" scope="request" value="https://${pageContext.request.serverName}/Shibboleth.sso/Login">
					<c:param name="target" value="${currentAbsoluteUrl}" />
				</c:url>
				<a href="<c:out value='${loginUrl}' />">log in</a> or
			</c:if>
			<a href="${contactUrl}&requestpid=${briefObject.pid.pid}">request access</a>.
		</p>
	</div>
</div>

<c:if test="${briefObject.resourceType == searchSettings.resourceTypeFile || briefObject.resourceType == searchSettings.resourceTypeAggregate}">
	<c:import url="fullRecord/neighborList.jsp" />
</c:if>
