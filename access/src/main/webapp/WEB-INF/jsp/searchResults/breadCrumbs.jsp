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

<c:choose>
	<c:when test="${not empty param.queryPath}"><c:set var="queryPath" value="${param.queryPath}"/></c:when>
	<c:otherwise><c:set var="queryPath" value="search"/></c:otherwise>
</c:choose>

<div class="contentarea">
	<c:if test="${not empty searchState.searchFields || not empty searchState.facets || not empty searchState.rangeFields || not empty searchState.accessTypeFilter}">
		<h2>Breadcrumbs</h2>
	</c:if>
	<ul class="crumblist">
		<c:if test="${not empty searchState.searchFields}">
			<c:forEach items="${searchState.searchFields}" var="field">
				<c:if test="${not empty field.value}">
					<c:url var="removeUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
						<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["REMOVE_SEARCH_FIELD"]}:${searchSettings.searchFieldParams[field.key]}'/>
					</c:url>
					<li>
						(<a href="<c:out value="${removeUrl}"/>">x</a>)
						<c:out value="${searchSettings.searchFieldLabels[field.key]}" />: <c:out value="${field.value}" />
					</li>
				</c:if>
			</c:forEach>
		</c:if>
		<c:if test="${not empty searchState.facets}">
			<c:forEach items="${searchState.facets}" var="field">
				<c:url var="removeUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["REMOVE_FACET"]}:${searchSettings.searchFieldParams[field.key]}'/>
				</c:url>
				<li>
					(<a href="<c:out value="${removeUrl}"/>">x</a>)
					<c:out value="${searchSettings.searchFieldLabels[field.key]}" />: 
					<c:choose>
						<c:when test='${field.value.getClass().name == "edu.unc.lib.dl.search.solr.model.CutoffFacet" 
								|| field.value.getClass().name == "edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet"}'>
							<c:set var="facetNodes" scope="request" value="${field.value.facetNodes}"/>
							<c:import url="WEB-INF/jsp/common/hierarchyTrail.jsp">
								<c:param name="fieldKey"><c:out value="${field.key}"/></c:param>
								<c:param name="linkLast">false</c:param>
								<c:param name="queryPath" value="${queryPath}"/>
							</c:import>
						</c:when>
						<c:otherwise>
							<c:out value="${field.value}" />
						</c:otherwise>
					</c:choose>
				</li>
			</c:forEach>
		</c:if>
		<c:if test="${not empty searchState.rangeFields}">
			<c:forEach items="${searchState.rangeFields}" var="field">
				<c:url var="removeUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
					<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["REMOVE_RANGE_FIELD"]}:${searchSettings.searchFieldParams[field.key]}'/>
				</c:url>
				<li>
					(<a href="<c:out value="${removeUrl}"/>">x</a>)
					<c:out value="${searchSettings.searchFieldLabels[field.key]}" />: 
					<c:choose>
						<c:when test="${empty field.value.leftHand && empty field.value.rightHand}">
						</c:when>
						<c:when test="${empty field.value.leftHand}">
							Before <c:out value="${field.value.rightHand}" />
						</c:when>
						<c:when test="${empty field.value.rightHand}">
							After <c:out value="${field.value.leftHand}" />
						</c:when>
						<c:otherwise>
							<c:out value="${field.value.leftHand}" /> to <c:out value="${field.value.rightHand}" />
						</c:otherwise>
					</c:choose>
				</li>
			</c:forEach>
		</c:if>
		<c:if test="${not empty searchState.accessTypeFilter}">
			<c:url var="removeUrl" scope="page" value='${queryPath}?${searchStateUrl}'>
				<c:param name="${searchSettings.searchStateParams['ACTIONS']}" value='${searchSettings.actions["REMOVE_ACCESS_FILTER"]}'/>
			</c:url>
			<li>
				(<a href="<c:out value="${removeUrl}"/>">x</a>)
				Access Level: <c:out value="${searchSettings.searchFieldLabels[searchState.accessTypeFilter]}"/>
			</li>
		</c:if>
	</ul>
</div>