<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<div class="onecol">
	<div class="contentarea">
		<div class="bottomline">
			<div id="full_record_trail" class="left">
				<c:set var="objectPath" scope="request" value="${briefObject.objectPath}"/>
				<c:import url="/jsp/util/pathTrail.jsp">
					<c:param name="hideLast"><c:choose><c:when test="${briefObject.resourceType == searchSettings.resourceTypeFile}">true</c:when><c:otherwise>false</c:otherwise></c:choose></c:param>
					<c:param name="linkLast">true</c:param>
				</c:import>
			</div>
		</div>
	</div>
</div>
