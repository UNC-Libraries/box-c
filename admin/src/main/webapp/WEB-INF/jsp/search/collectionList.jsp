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
<c:forEach items="${resultResponse.resultList}" var="metadata" varStatus="status">
	<div id="entry${metadata.id}" class="browseitem">
		<c:set var="thumbnailUrl" value="/services/api/thumb/${metadata.id}/large"/>
		<a href="/record/${metadata.id}" target="_blank">
			<c:choose>
				<c:when test="${metadata.datastreamObjects.contains('THUMB_LARGE')}"><img class="largethumb" src="${thumbnailUrl}" /></c:when>
				<c:otherwise><img class="largethumb" src="/static/images/placeholder/collection-large.png" /></c:otherwise>
			</c:choose>
		</a>

		<ul class="itemnavigation">
			<li>
				<c:set var="unpublishedCount" value="${metadata.countMap.unpublished}"/>
				<c:choose>
					<c:when test="${not empty unpublishedCount}">
						<a href="review/${metadata.id}">Review ${unpublishedCount} unpublished item<c:if test="${unpublishedCount != 1}">s</c:if></a>
					</c:when>
					<c:otherwise>No unpublished items</c:otherwise>
				</c:choose>
			</li>
		</ul>

		<div class="itemdetails">
			<h2>
				<a href="list/${metadata.id}"
					class="has_tooltip" title="View details for <c:out value='${metadata.title}'/>."><c:out value='${metadata.title}'/></a>
				<c:set var="childCount" value="${metadata.countMap.child}"/>
				<span class="searchitem_container_count">
					<c:choose>
						<c:when test="${not empty childCount}">
							(${childCount} item<c:if test="${childCount != 1}">s</c:if>)
						</c:when>
						<c:otherwise>(0 items)</c:otherwise>
					</c:choose>
				</span>
			</h2>
			<p>Last Updated: <c:out value='${metadata.dateUpdated}'/></p>
			<c:if test="${not empty metadata.abstractText}">
				<p>
					<c:out value="${metadata.abstractText}"/>
				</p>
			</c:if>
		</div>
	</div>
	<div class="clear"></div>
</c:forEach>