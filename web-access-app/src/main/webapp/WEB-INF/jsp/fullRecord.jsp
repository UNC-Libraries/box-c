<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<c:if test="${markedForDeletion}">
	<c:set var="isDeleted" value="deleted" scope="request"/>
</c:if>

<c:set var="allowsPublicAccess" value="${permsHelper.allowsPublicAccess(briefObject)}" />
<c:if test="${not empty briefObject && not allowsPublicAccess}">
	<c:set var="isProtected" value="protected" scope="page"/>
</c:if>

<c:set var="hasRestrictedContent" value="${not empty briefObject && not permsHelper.allowsPublicAccess(briefObject)}" scope="request"/>

<c:set var="badgeIcon" scope="request">
	<c:choose>
		<c:when test="${markedForDeletion}">
			trash
		</c:when>
		<c:when test="${hasRestrictedContent}">
			lock
		</c:when>
	</c:choose>
</c:set>

<div class="content-wrap full_record ${isDeleted}${' '}${isProtected}">

<c:choose>
	<c:when test="${briefObject.resourceType == searchSettings.resourceTypeAggregate}">
		<c:import url="fullRecord/aggregateRecord.jsp" />
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