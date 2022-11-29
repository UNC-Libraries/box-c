<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<div class="onecol gray shadowtop">
	<div class="contentarea">
		<h2 class="link-list">Related Items (neighbors in this collection/folder)</h2>
		<c:forEach items="${neighborList}" var="neighbor" varStatus="status">
			<c:url var="fullRecordUrl" scope="page" value="record/${neighbor.id}">
			</c:url>
			<c:set var="currentItemClass" scope="page">
				<c:if test="${briefObject.id == neighbor.id}"> current_item</c:if>
			</c:set>
			<c:set var="markedForDeletion"  value="${cdr:contains(neighbor.status, 'Marked For Deletion')}" scope="request"/>
			<c:choose>
				<c:when test="${markedForDeletion}">
					<c:set var="neighborIsDeleted" value="deleted" scope="request"/>
				</c:when>
				<c:otherwise>
					<c:set var="neighborIsDeleted" value="" scope="request"/>
				</c:otherwise>
			</c:choose>

			<div class="relateditem ${currentItemClass}">
				<div class="relatedthumb ${neighborIsDeleted}">
					<c:set var="thumbnailObject" value="${neighbor}" scope="request" />
					<c:import url="common/thumbnail.jsp">
						<c:param name="target" value="record" />
						<c:param name="size" value="small" />
					</c:import>
				</div>
				<p><a href="<c:out value='${fullRecordUrl}' />"><c:out value="${cdr:truncateText(neighbor.title, 50)}" /></a></p>
			</div>
		</c:forEach>
	</div>
</div>