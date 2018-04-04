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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<%@ page import="edu.unc.lib.dl.model.ContainerSettings.ContainerView" %>

<c:choose>
	<c:when test="${not empty briefObject.countMap}">
		<c:set var="childCount" value="${briefObject.countMap.child}"/>
	</c:when>
	<c:otherwise>
		<c:set var="childCount" value="0"/>
	</c:otherwise>
</c:choose>

<div class="onecol container_record full_record_top" id="full_record">
	<c:url var="structureUrl" scope="page" value='structure/${briefObject.id}'></c:url>

	<div class="contentarea">
		<c:set var="thumbnailObject" value="${briefObject}" scope="request" />
		<c:import url="common/thumbnail.jsp">
			<c:param name="target" value="record" />
			<c:param name="size" value="large" />
		</c:import>
		<c:if test="${permsHelper.hasEditAccess(accessGroupSet, briefObject)}">
			<div class="actionlink right"><a href="${adminBaseUrl}/describe/${briefObject.id}">Edit</a></div>
		</c:if>
		
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
				<c:if test="${not empty briefObject.parentCollection && briefObject.parentCollection != briefObject.id && briefObject.ancestorPathFacet.highestTier > 0}">
					<li>
						<c:url var="parentUrl" scope="page" value="record/${briefObject.parentCollection}" />
						<span class="bold">Collection:</span> 
						<a href="<c:out value='${parentUrl}' />"><c:out value="${briefObject.parentCollectionName}"/></a>
					</li>
				</c:if>
			</ul>
			<c:if test="${not empty embargoDate}">
				<ul class="pipe_list smaller">
					<li><span class="bold">Embargoed Until:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${embargoDate}" /></li>
				</ul>
				<br class="clear" />
			</c:if>
			<c:if test="${briefObject['abstractText'] != null}">
					<p class="clear">
						<c:out value="${briefObject['abstractText']}" />
					</p>
			</c:if>
			<form id="collectionsearch" action="basicSearch" method="get" class="clear">
				<div id="csearch_inputwrap">
					<input type="text" name="query" id="csearch_text" placeholder="Search the ${fn:toLowerCase(briefObject.resourceType)}"><input type="submit" value="Go" id="csearch_submit">
				</div>
				<input type="hidden" name="queryType" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}"/>
				<input type="hidden" name="container" 
					value='${briefObject.id}'/>
			</form>
			
			<div class="clear"></div>
			<p class="full_record_browse">
				<c:url var="collectionResultsUrl" scope="page" value='browse/dept/${briefObject.id}'></c:url>
				<a href="list/<c:out value='${briefObject.id}' />">
					Browse all&nbsp;(<c:out value="${childCount}"/> items)
				</a>
			</p>
		</div>
	</div>
</div>
<div id="collection_tabs" class="tabbed_content">
	<nav class="tab_headers">
		<ul>
			<c:forEach items="${containerSettings.getViews()}" var="viewName">
				<li data-tabid="${viewName}"><a href="/record/${briefObject.id}#tab_${viewName}">${containerSettings.getViewDisplayName(viewName)}</a></li>
			</c:forEach>
		</ul>
	</nav>
	<c:if test="${containerSettings.getViews().contains('DESCRIPTION')}">
		<div data-tabid="DESCRIPTION" id="tab_DESCRIPTION">
			<c:if test="${hasFacetFields}">
				<div class="fourcol">
					<div id="facetList" class="contentarea">
						<c:set var="selectedContainer" scope="request" value="${briefObject}"/>
						<h2>Contents</h2>
						<c:import url="/jsp/util/facetList.jsp" />
					</div>
				</div>
			</c:if>
			<div class="${hasFacetFields? "threecol" : "onecol"} white">
				<div class="contentarea">
					<div class="metadata">
						${fullObjectView}
					</div>
				</div>
			</div>
		</div>
	</c:if>
	<c:if test="${containerSettings.getViews().contains('STRUCTURE')}">
		<div data-tabid="STRUCTURE" id="tab_STRUCTURE">
			<c:if test="${hasFacetFields}">
				<div class="fourcol">
					<div id="facetList" class="contentarea">
						<c:set var="selectedContainer" scope="request" value="${briefObject}"/>
						<h2>Contents</h2>
						<c:import url="/jsp/util/facetList.jsp">
						</c:import>
					</div>
				</div>
			</c:if>
			<div class="${hasFacetFields? "threecol" : "onecol"} white">
				<div id="hierarchical_view_full_record" class="contentarea">
					<h2>Folder Browse View</h2>
					<div class="structure" data-pid="${briefObject.id}"></div>
				</div>
			</div>
		</div>
	</c:if>
	<c:if test="${containerSettings.getViews().contains('DEPARTMENTS')}">
		<div data-tabid="DEPARTMENTS" id="tab_DEPARTMENTS">
			<div class="onecol">
				<c:set var="container" scope="request" value="${briefObject}"/>
				<c:import url="searchResults/departmentList.jsp" />
			</div>
		</div>
	</c:if>
	<c:if test="${containerSettings.getViews().contains('LIST_CONTENTS')}">
		<div data-tabid="LIST_CONTENTS" id="tab_LIST_CONTENTS">
			<c:set var="resultCount" scope="request" value="${contentListResponse.resultCount}"/>
			<c:choose>
				<c:when test="${resultCount > 0}">
					<c:set var="selectedContainer" scope="request" value="${briefObject}"/>
					<c:set var="resultResponse" scope="request" value="${contentListResponse}" />
					<c:set var="searchState" scope="request" value="${contentListResponse.searchState}"/>
					<c:set var="queryMethod" scope="request" value="listContents"/>
					<c:if test="${hasFacetFields}">
						<div class="fourcol">
							<div id="facetList" class="contentarea">
								<h2>Contents</h2>
								<c:import url="/jsp/util/facetList.jsp">
									<c:param name="queryMethod">listContents</c:param>
								</c:import>
							</div>
						</div>
					</c:if>
					<div class="${hasFacetFields? "threecol" : "onecol"}">
						<div class="contentarea">
							<c:import url="searchResults/resultsList.jsp">
								<c:param name="showSelectedContainer">false</c:param>
							</c:import>
						</div>
					</div>
				</c:when>
				<c:otherwise>
					<div class="onecol contentarea">This collection contains no contents at this time.</div>
				</c:otherwise>
			</c:choose>
		</div>
	</c:if>
	<c:if test="${containerSettings.getViews().contains('EXPORTS')}">
		<div data-tabid="EXPORTS" id="tab_EXPORTS">
			<div class="onecol">
				<c:import url="fullRecord/exports.jsp" />
			</div>
		</div>
	</c:if>
</div>
