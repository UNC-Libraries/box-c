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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page import="edu.unc.lib.boxc.search.api.requests.SearchState" %>
<%@ page import="edu.unc.lib.boxc.search.solr.responses.SearchResultResponse" %>
<%@ page import="edu.unc.lib.boxc.search.solr.config.SearchSettings" %>

<%
{
	//Calculating total number of pages and current page since jstl handles casting division to ints very poorly
	SearchSettings searchSettings = (SearchSettings)request.getAttribute("searchSettings");
	SearchResultResponse resultResponse = (SearchResultResponse) request.getAttribute("resultResponse");
	Long resultCount = resultResponse.getResultCount();
	SearchState searchState = resultResponse.getSearchState();

	if (resultCount == null || searchState == null)
		return;
	long totalPages = (long) Math.ceil(((double) resultCount) / searchState.getRowsPerPage());
	long currentPage = searchState.getStartRow() / searchState.getRowsPerPage() + 1;

	long sideGap = searchSettings.pagesToDisplay / 2;
	long left = currentPage - sideGap;
	long right = currentPage + sideGap;

	if (left < 1){
		right -= left;
		left = 1;
	}
	if (right > totalPages){
		left -= (right - totalPages);
		if (left < 1)
			left = 1;
		right = totalPages;
	}

	long pageEndCount = searchState.getStartRow() + searchState.getRowsPerPage();
	if (pageEndCount > resultResponse.getResultCount())
		pageEndCount = resultResponse.getResultCount();

	pageContext.setAttribute("left", left);
	pageContext.setAttribute("right", right);
	pageContext.setAttribute("currentPage", currentPage);
	pageContext.setAttribute("totalPages", totalPages);
	pageContext.setAttribute("pageEndCount", pageEndCount);
}
%>
<p>
<c:choose>
	<c:when test="${totalPages > 1}">
		<c:if test="${not empty resultResponse.selectedContainer}">
			<c:set var="containerPath" value="/${resultResponse.selectedContainer.id}"/>
		</c:if>
		<c:if test="${param.showPaginationText == true}">
			Showing
			<c:if test="${resultResponse.resultCount > 0}">
				<span class="has-text-weight-bold">${resultResponse.searchState.startRow+1}-${pageEndCount}</span>
				of
			</c:if>
			<span class="has-text-weight-bold">${resultResponse.resultCount}</span> results found
		</c:if>
		<c:if test="${param.showPaginationLinks == true}">
			<c:choose>
				<c:when test="${currentPage == 1}">
					<span class="has-text-weight-bold">&lt;&lt;</span>
				</c:when>
				<c:when test="${currentPage == 2}">
					<c:url var="firstPageUrl" scope="page" value='${param.queryMethod}${containerPath}?${searchStateUrl}'>
						<c:param name='a.setStartRow=1' value=''/>
					</c:url>
					<a class="has-text-weight-bold" href="<c:out value="${firstPageUrl}"/>">&lt;&lt;</a>
				</c:when>
				<c:otherwise>
					<c:url var="previousPageUrl" scope="page" value='${param.queryMethod}${containerPath}?${searchStateUrl}'>
						<c:param name='a.${searchSettings.actions["PREVIOUS_PAGE"]}' value=''/>
					</c:url>
					<c:url var="firstPageUrl" scope="page" value='${param.queryMethod}${containerPath}?${searchStateUrl}'>
						<c:param name='a.setStartRow=1' value=''/>
					</c:url>
					<a class="has-text-weight-bold" href="<c:out value="${previousPageUrl}"/>">&lt;&lt;</a>
					<a class="has-text-weight-bold search-result-num" href="<c:out value="${firstPageUrl}"/>">1</a>
				</c:otherwise>
			</c:choose>
			<c:if test="${left != 1}">
				<span class="has-text-weight-bold">...</span>
			</c:if>
			<c:forEach var="pageNumber" begin="${left}" end="${right}" step="1" varStatus ="status">
				<c:choose>
					<c:when test="${pageNumber == currentPage}">
						<span class="has-text-weight-bold search-result-selected">${pageNumber}</span>
					</c:when>
					<c:otherwise>
						<c:url var="pageJumpUrl" scope="page" value='${param.queryMethod}${containerPath}?${searchStateUrl}'>
							<c:param name='a.${searchSettings.actions["SET_START_ROW"]}' value='${(pageNumber - 1) * resultResponse.searchState.rowsPerPage}'/>
						</c:url>
						<a class="search-result-num" href="<c:out value="${pageJumpUrl}"/>"><c:out value="${pageNumber}" /></a>
					</c:otherwise>
				</c:choose>
			</c:forEach>
			<c:if test="${right != totalPages}">
				<span class="has-text-weight-bold">...</span>
			</c:if>
			<c:choose>
				<c:when test="${right == '0' || currentPage == right}">
					<span class="has-text-weight-bold">&gt;&gt;</span>
				</c:when>
				<c:when test="${totalPages == right}">
					<c:url var="nextPageUrl" scope="page" value='${param.queryMethod}${containerPath}?${searchStateUrl}'>
					<c:param name='a.${searchSettings.actions["SET_START_ROW"]}' value='${(totalPages - 1) * resultResponse.searchState.rowsPerPage}'/>
				</c:url>
					<a class="has-text-weight-bold" href="<c:out value="${nextPageUrl}"/>">&gt;&gt;</a>
				</c:when>
				<c:otherwise>
                    <c:url var="nextPageUrl" scope="page" value='${param.queryMethod}${containerPath}?${searchStateUrl}'>
                        <c:param name='a.${searchSettings.actions["SET_START_ROW"]}' value='${(totalPages - 1) * resultResponse.searchState.rowsPerPage}'/>
                    </c:url>
					<a class="has-text-weight-bold search-result-num" href="<c:out value="${nextPageUrl}"/>">${totalPages}</a>

					<c:if test="${(totalPages - 1) == right}">
						<a class="has-text-weight-bold" href="<c:out value="${nextPageUrl}"/>">&gt;&gt;</a>
					</c:if>

				</c:otherwise>
			</c:choose>
		</c:if>
	</c:when>
	<c:otherwise>
		<span class="has-text-weight-bold">${resultResponse.resultCount}</span> results found
	</c:otherwise>
</c:choose>
</p>