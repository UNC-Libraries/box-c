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
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<%--
	Renders a navigable facet list for refining a result set.

	facetFields - Object of type edu.unc.lib.dl.search.solr.model.FacetFieldList which contains the
		list of facets to render.  Required.
	currentContainer
	queryPath - override the servlet that facet links will send the user to.  Default "search"
	additionalLimitActions - additional actions which will be appended to any add facet actions.
--%>

<c:choose>
	<c:when test="${not empty param.queryMethod}">
		<c:set var="queryMethod" value="${param.queryMethod}"/>
	</c:when>
	<c:otherwise>
		<c:set var="queryMethod" value="search"/>
	</c:otherwise>
</c:choose>

<c:set var="queryPath" value="${queryMethod}"/>
<c:if test="${not empty selectedContainer}">
	<c:set var="queryPath" value="${queryPath}/${selectedContainer.id}"/>
</c:if>

<c:if test="${not empty param.additionalLimitActions}">
	<c:set var="additionalLimitActions" value="${param.additionalLimitActions}|"/>
</c:if>
<c:forEach items="${facetFields}" var="facetField">
	<div id="facet_field_${searchSettings.searchFieldParams[facetField.name]}">
		<c:choose>
			<c:when test="${empty facetField.values && not empty searchState.facetLimits[facetField.name] && searchState.facetLimits[facetField.name] == 0}">
				<c:url var="facetOpenUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
					<c:param name="a.${searchSettings.actions['REMOVE_FACET_LIMIT']}" value='${searchSettings.searchFieldParams[facetField.name]}'/>
				</c:url>
				<h3 class="facet_name"><a href="<c:out value="${facetOpenUrl}"/>">&#9654; <c:out value="${searchSettings.searchFieldLabels[facetField.name]}" /></a></h3>
			</c:when>
			<c:when test="${not empty facetField.values}">
				<c:url var="facetCollapseUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
					<c:param name="a.${searchSettings.actions['SET_FACET_LIMIT']}" value='${searchSettings.searchFieldParams[facetField.name]},0'/>
				</c:url>
				<h3 class="facet_name"><a href="<c:out value="${facetCollapseUrl}"/>">&#9660; <c:out value="${searchSettings.searchFieldLabels[facetField.name]}" /></a></h3>
			</c:when>
		</c:choose>
		<c:if test="${not empty facetField.values}">
			<c:if test="${facetField.name == 'ANCESTOR_PATH'}">
				<div id="facet_field_${searchSettings.searchFieldParams[facetField.name]}_structure" class="hidden">
					<c:if test="${not empty currentContainer}"><c:set var="containerPath" value="/${currentContainer}"/></c:if>
					<c:url var="structureUrl" scope="page" value='structure${containerPath}/collection?${searchStateUrl}'>
						<c:param name="view" value="facet"/>
						<c:param name="queryp" value="list"/>
						<c:param name="files" value="false"/>
					</c:url>
					<a href="<c:out value="${structureUrl}" />"><img src="/static/images/ajax_loader.gif"/></a>
				</div>
			</c:if>
			<ul id="facet_field_<c:out value="${searchSettings.searchFieldParams[facetField.name]}_list" />" class="facets">
				<c:forEach items="${facetField.values}" var="facetValue" varStatus="status">
					<c:choose>
						<c:when test="${status.count == searchSettings.facetsPerGroup && (empty searchState.facetLimits[facetField.name] || status.count == searchState.facetLimits[facetField.name])}">
							<c:url var="facetExpandUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
								<c:param name="a.${searchSettings.actions['SET_FACET_LIMIT']}" value='${searchSettings.searchFieldParams[facetField.name]}:${searchSettings.expandedFacetsPerGroup}'/>
							</c:url>
							<li class="facet_view_expand_toggle"><a href="<c:out value="${facetExpandUrl}"/>">Show more...</a></li>
						</c:when>
						<c:otherwise>
							<c:if test="${not empty facetValue.displayValue && not empty facetValue.searchValue}">
								<li>
									<c:choose>
										<c:when test="${facetField.name == 'PARENT_COLLECTION'}">
											<c:url var="facetActionUrl" scope="page" value='${queryMethod}/${fn:substringAfter(facetValue.searchValue, ",")}?${searchStateUrl}'>
												<c:if test='${not empty additionalLimitActions}'>
													<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${additionalLimitActions}'/>
												</c:if>
											</c:url>
										</c:when>
										<c:otherwise>
											<c:url var="facetActionUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
												<c:param name="a.${searchSettings.actions['SET_FACET']}" value='${searchSettings.searchFieldParams[facetValue.fieldName]}:${facetValue.limitToValue}'/>
											</c:url>
										</c:otherwise>
									</c:choose>
									<a href="<c:out value="${facetActionUrl}"/>"><c:out value="${facetValue.displayValue}" /></a> (<c:out value="${facetValue.count}" />)
								</li>
							</c:if>
							<c:if test="${status.last && status.count >= searchSettings.facetsPerGroup && not empty searchState.facetLimits[facetValue.fieldName]}">
								<c:url var="facetReduceUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
									<c:param name="a.${searchSettings.actions['REMOVE_FACET_LIMIT']}" value='${searchSettings.searchFieldParams[facetValue.fieldName]}'/>
								</c:url>
								<li class="facet_view_expand_toggle"><a href="<c:out value="${facetReduceUrl}"/>">...Show less</a></li>
							</c:if>
						</c:otherwise>
					</c:choose>
				</c:forEach>
			</ul>
		</c:if>
	</div>
</c:forEach>