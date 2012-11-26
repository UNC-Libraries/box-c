<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<c:forEach items="${resultResponse.resultList}" var="metadata" varStatus="status">
	<div id="entry${metadata.id}" class="browseitem">
		<div class="contentarea">
			<c:set var="thumbnailUrl" value="${cdr:getDatastreamUrl(metadata, 'THUMB_LARGE', fedoraUtil)}"/>
			<a href="/record?id=${metadata.id}" target="_blank">
				<c:choose>
					<c:when test="${metadata.datastreamObjects.contains('THUMB_LARGE')}"><img class="largethumb" src="${thumbnailUrl}" /></c:when>
					<c:otherwise><img class="largethumb" src="/static/images/placeholder/large/oldwell.jpg" /></c:otherwise>
				</c:choose>
			</a>

			<ul class="itemnavigation">
				<li>
					<c:set var="unpublishedCount" value="${metadata.countMap.unpublished}"/>
					<c:choose>
						<c:when test="${not empty unpublishedCount}">
							<a href="uuid/${metadata.idWithoutPrefix}/review">Review ${unpublishedCount} unpublished item<c:if test="${unpublishedCount != 1}">s</c:if></a>
						</c:when>
						<c:otherwise>No unpublished items</c:otherwise>
					</c:choose>
				</li>
			</ul>

			<div class="itemdetails">
				<h2>
					<a href="/record?id=${metadata.id}" target="_blank"
						class="has_tooltip" title="View details for <c:out value='${metadata.title}'/>."><c:out value='${metadata.title}'/></a>
					<c:set var="childCount" value="${metadata.countMap.child}"/>
					<span class="searchitem_container_count">
						<c:choose>
							<c:when test="${not empty childCount}">
								<a href="">(${childCount} item<c:if test="${childCount != 1}">s</c:if>)</a>
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
	</div>
</c:forEach>