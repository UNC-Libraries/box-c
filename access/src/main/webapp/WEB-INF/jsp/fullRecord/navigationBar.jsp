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

<div class="onecol">
	<div class="contentarea">
		<div class="bottomline">
			<div id="full_record_trail" class="left">
				<c:set var="objectPath" scope="request" value="${briefObject.objectPath}"/>
				<c:import url="/jsp/util/pathTrail.jsp">
					<c:param name="hideLast"><c:choose><c:when test="${briefObject.resourceType == searchSettings.resourceTypeFile}">true</c:when><c:otherwise>false</c:otherwise></c:choose></c:param>
					<c:param name="linkLast">true</c:param>
					<c:param name="queryPath">list</c:param>
				</c:import>
			</div>
			<c:if test="${not empty previousNext}">
				<ul id="prevNext">
					<c:choose>
						<c:when test="${not empty previousNext.previous}">
							<li><a href="<c:out value='record/${previousNext.previous.id}' />"><i class="fa fa-arrow-left" aria-hidden="true"></i> Previous</a></li>
						</c:when>
						<c:otherwise>
							<li><i class="fa fa-arrow-left"aria-hidden="true"></i> Previous</li>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${not empty previousNext.next}">
							<li><a href="<c:out value='record/${previousNext.next.id}' />">Next <i class="fa fa-arrow-right" aria-hidden="true"></i></a></li>
						</c:when>
						<c:otherwise>
							<li>Next <i class="fa fa-arrow-right" aria-hidden="true"></i><li>
						</c:otherwise> 
					</c:choose>
				</ul>
			</c:if>
		</div>
	</div>
</div>
