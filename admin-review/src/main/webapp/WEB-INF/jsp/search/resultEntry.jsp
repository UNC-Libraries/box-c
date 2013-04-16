<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<tr class="entry" data-pid="${metadata.id}">
	<td class="check_box">
		<input type="checkbox">
	</td>
	<td class="type">
		<c:choose>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeFile}">
				<img src="/static/images/admin/type_file.png" />
			</c:when>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeFolder}">
				<img src="/static/images/admin/type_folder.png" />
			</c:when>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeCollection}">
				<img src="/static/images/admin/type_coll.png" />
			</c:when>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeAggregate}">
				<img src="/static/images/admin/type_aggr.png" />
			</c:when>
		</c:choose>
	</td>
	<td class="itemdetails">
		<c:choose>
			<c:when test="${metadata.resourceType == searchSettings.resourceTypeFile}">
				<a href="/record?id=${metadata.id}" target="_new" class="has_tooltip"
						title="View details for <c:out value='${metadata.title}'/>."><c:out value='${metadata.title}'/></a>
			</c:when>
			<c:otherwise>
				<a href="list/${metadata.pid.path}" class="has_tooltip"
						title="View contents of <c:out value='${metadata.title}'/>.">
					<c:out value='${metadata.title}'/>
				</a>
				<c:set var="childCount" value="${metadata.countMap.child}"/>
				<span class="searchitem_container_count">
					<c:choose>
						<c:when test="${not empty childCount}">
							(${childCount} item<c:if test="${childCount != 1}">s</c:if>)
						</c:when>
						<c:otherwise>(0 items)</c:otherwise>
					</c:choose>
				</span>
			</c:otherwise>
		</c:choose>
		<c:if test="${metadata.datastreamObjects.contains('DATA_FILE')}">
			&nbsp;<a target="_preview" href="${cdr:getDatastreamUrl(metadata, 'DATA_FILE', fedoraUtil)}" class="preview">(preview ${metadata.getDatastreamObject("DATA_FILE").extension})</a>
		</c:if>	
		<!-- Tags -->
		<c:forEach var="tag" items="${metadata.tags}">
			<a class="status${tag.emphasis ? ' emphasis' : ''}" ${tag.link == null ? '' : 'href="' + tag.link + "'"} title="${tag.text}" ><c:out value="${tag.label}"/></a>
		</c:forEach>
	</td>
	<td class="creator">
		<c:choose>
			<c:when test="${not empty metadata.creator}">
				<c:out value="${metadata.creator[0]}"/>
				<c:if test="${fn:length(metadata.creator) > 1}">
					&nbsp;et al
				</c:if>
			</c:when>
			<c:otherwise>-</c:otherwise>
		</c:choose>
	</td>
	<td class="date_added">
		<c:choose>
			<c:when test="${not empty metadata.dateAdded}">
				<fmt:formatDate value="${metadata.dateAdded}" pattern="MM/dd/yyyy"/>
			</c:when>
			<c:otherwise>-</c:otherwise>
		</c:choose>
	</td>
	<td class="date_added">
		<c:choose>
			<c:when test="${not empty metadata.dateUpdated}">
				<fmt:formatDate value="${metadata.dateUpdated}" pattern="MM/dd/yyyy"/>
			</c:when>
			<c:otherwise>-</c:otherwise>
		</c:choose>
	</td>
	<td class="menu_box">
		<img src="/static/images/admin/gear.png"/>
		<ul class='action_menu'>
			<li class="publish_link">
				<c:choose>
					<c:when test="${metadata.status.contains('Unpublished')}">Publish</c:when>
					<c:otherwise>Unpublish</c:otherwise>
				</c:choose>
			</li>
			<li class="edit_access">Edit Access Control</li>
			<li class='edit_description'><a href="describe/${metadata.pid.path}">
				<c:choose>
					<c:when test="${metadata.datastreamObjects.contains('MD_DESCRIPTIVE')}">
						Edit Description
					</c:when>
					<c:otherwise>
						Add Description
					</c:otherwise>
				</c:choose>
			</a></li>
			<li class="delete_link">Delete</li>
		</ul>
	</td>
</tr>