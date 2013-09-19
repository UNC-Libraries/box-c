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

<div class="onecol gray shadowtop">
	<div class="contentarea">
		<h2>Related Items (neighbors in this collection/folder)</h2>
		<c:forEach items="${neighborList}" var="neighbor" varStatus="status">
			<c:url var="fullRecordUrl" scope="page" value="record/${neighbor.id}">
			</c:url>
			<c:set var="currentItemClass" scope="page">
				<c:if test="${briefObject.id == neighbor.id}"> current_item</c:if>
			</c:set>
			<c:set var="hasListAccessOnly" value="${cdr:hasListAccessOnly(requestScope.accessGroupSet, neighbor)}"/>
			<div class="relateditem ${currentItemClass}">
				<div class="relatedthumb">
					<a href="<c:out value='${fullRecordUrl}' />">
						<div class="small thumb_container">
							<c:choose>
								<c:when test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'THUMB_SMALL', neighbor)
										&& (!hasListAccessOnly || (hasListAccessOnly && (neighbor.resourceType == searchSettings.resourceTypeFolder || neighbor.resourceType == searchSettings.resourceTypeCollection)))}">
									<img id="neighbor_thumb_${status.count}" class="smallthumb ph_small_${neighbor.contentTypeFacet[0].searchKey}" 
											src="${cdr:getDatastreamUrl(neighbor, 'THUMB_SMALL', fedoraUtil)}"/>
								</c:when>
								<c:when test="${neighbor.resourceType == searchSettings.resourceTypeAggregate && empty neighbor.contentTypeFacet[0].searchKey}">
									<img class="smallthumb" src="/static/images/placeholder/small/default.png"/>
								</c:when>
								<c:otherwise>
									<img id="neighbor_thumb_${status.count}" class="smallthumb ph_small_default" src="/static/images/placeholder/small/${neighbor.contentTypeFacet[0].searchKey}.png"/>
								</c:otherwise>
							</c:choose>
							<c:if test="${hasListAccessOnly}">
								<span><img src="/static/images/lockedstate.gif"/></span>
							</c:if>
						</div>
					</a>
				</div>
				<p><a href="<c:out value='${fullRecordUrl}' />"><c:out value="${cdr:truncateText(neighbor.title, 50)}" /></a></p>
			</div>
		</c:forEach>
	</div>
</div>