<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:forEach items="${resultResponse.resultList}" var="metadata" varStatus="status">
	<div id="entry${metadata.id}" class="browseitem">
		<div class="contentarea">
			<a href="/record?id=${metadata.id}" target="_blank"> <img
				class="largethumb" src="images/oldwell.jpg" />
			</a>

			<ul class="itemnavigation">
				<li><a href="review.html">Review 14 unpublished items</a></li>
			</ul>

			<div class="itemdetails">
				<h2>
					<a href="record?id=${metadata.id}" target="_blank"
						class="has_tooltip" title="View details for <c:out value='${metadata.title}'/>."><c:out value='${metadata.title}'/></a>
					<c:if test="${metadata.childCount > 1}">
						<p class="searchitem_container_count">(${metadata.childCount} item<c:if test="${metadata.childCount != 1}">s</c:if>)</p>
					</c:if>
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