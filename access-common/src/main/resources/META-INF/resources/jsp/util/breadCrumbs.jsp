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
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>

<c:set var="searchState" scope="page" value="${resultResponse.searchState}"/>

<c:choose>
	<c:when test="${not empty param.queryMethod}"><c:set var="queryMethod" value="${param.queryMethod}"/></c:when>
	<c:otherwise><c:set var="queryMethod" value="search"/></c:otherwise>
</c:choose>

<c:set var="queryPath" value="${queryMethod}"/>
<c:if test="${not empty resultResponse.selectedContainer}">
	<c:set var="queryPath" value="${queryPath}/${resultResponse.selectedContainer.id}"/>
</c:if>

<c:choose>
	<c:when test="${param.searchStateParameters != null}">
		<c:set var="searchStateParameters" value="${param.searchStateParameters}"/>
	</c:when>
	<c:otherwise>
		<c:set var="searchStateParameters" value="${searchStateUrl}"/>
	</c:otherwise>
</c:choose>
<c:if test="${not empty searchStateParameters}">
	<c:set var="searchStateParameters" value="?${searchStateParameters}"/>
</c:if>

<ul class="crumblist">
	<c:if test="${not empty searchState.searchFields}">
		<c:forEach items="${searchState.searchFields}" var="field">
			<c:set var="fieldName" value="${searchSettings.searchFieldParams[field.key]}" />
			<c:if test="${not empty field.value}">
				<c:url var="removeUrl" scope="page" value='${queryPath}${cdr:removeParameter(searchStateParameters, fieldName)}'>
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
			<c:if test='${field.value.getClass().name != "edu.unc.lib.dl.search.solr.model.CutoffFacet"}'>
				<c:set var="fieldName" value="${searchSettings.searchFieldParams[field.key]}" />
				<c:choose>
					<c:when test="${field.key == 'ANCESTOR_PATH'}">
						<c:url var="removeUrl" scope="page" value='${queryMethod}${searchStateParameters}'></c:url>
					</c:when>
					<c:otherwise>
						<c:url var="removeUrl" scope="page" value='${queryPath}${cdr:removeParameter(searchStateParameters, fieldName)}'>
						</c:url>
					</c:otherwise>
				</c:choose>
				
				<c:if test="${field.value.getClass().name == 'java.lang.String' || not empty field.value.displayValue}">
					<li>
						(<a href="<c:out value="${removeUrl}"/>">x</a>)
						<c:out value="${searchSettings.searchFieldLabels[field.key]}" />: 
						<c:choose>
							<c:when test='${field.value.getClass().name == "edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet"}'>
								<c:set var="facetNodes" scope="request" value="${field.value.facetNodes}"/>
								<c:import url="/jsp/util/hierarchyTrail.jsp">
									<c:param name="fieldKey"><c:out value="${field.key}"/></c:param>
									<c:param name="linkLast">false</c:param>
									<c:param name="queryPath" value="${queryMethod}"/>
									<c:param name="limitToContainer">true</c:param>
									<c:param name="selectedContainer"><c:if test="${not empty resultResponse.selectedContainer}">${resultResponse.selectedContainer.id}</c:if></c:param>
									<c:param name="isPath">${field.value.getClass().name == "edu.unc.lib.dl.search.solr.model.CutoffFacet"}</c:param>
								</c:import>
							</c:when>
							<c:when test="${field.value.getClass().name == 'java.lang.String'}">
								<c:out value="${field.value}" />
							</c:when>
							<c:otherwise>
								<c:out value="${field.value.displayValue}" />
							</c:otherwise>
						</c:choose>
					</li>
				</c:if>
			</c:if>
		</c:forEach>
	</c:if>
	<c:if test="${not empty searchState.rangeFields}">
		<c:forEach items="${searchState.rangeFields}" var="field">
			<c:set var="fieldName" value="${searchSettings.searchFieldParams[field.key]}" />
			<c:url var="removeUrl" scope="page" value='${queryPath}${cdr:removeParameter(searchStateParameters, fieldName)}'>
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
</ul>