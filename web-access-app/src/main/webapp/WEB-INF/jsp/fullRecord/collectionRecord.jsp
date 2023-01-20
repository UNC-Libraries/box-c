<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>

<c:choose>
    <c:when test="${not empty briefObject.countMap}">
        <c:set var="childCount" value="${briefObject.countMap.child}"/>
    </c:when>
    <c:otherwise>
        <c:set var="childCount" value="0"/>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${childCount == 1}">
        <c:set var="pluralCount" value="item"/>
    </c:when>
    <c:otherwise>
        <c:set var="pluralCount" value="items"/>
    </c:otherwise>
</c:choose>

<c:url var="fullRecordUrl" scope="page" value="record/${briefObject.id}"/>

<div class="contentarea">
    <div id="is-collection" class="browse-header">
        <div class="columns">
            <div class="column">
                <c:import url="fullRecord/navigationBar.jsp" />
            </div>
        </div>
        <div class="columns">
            <div class="column <c:if test="hasRestrictedContent">is-8</c:if>">
                <h2 class="${isDeleted}">
                    <c:set var="thumbnailObject" value="${briefObject}" scope="request" />
                    <c:import url="common/thumbnail.jsp">
                        <c:param name="size" value="large" />
                    </c:import>
                    <c:out value="${briefObject.title}"/> <span class="item_container_count"><c:out value="${childCount}" /> items</span>
                </h2>
                <c:if test="${not empty briefObject.dateAdded}">
                    <p><strong>${searchSettings.searchFieldLabels['DATE_ADDED']}:</strong> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateAdded}" /></p>
                </c:if>
                <c:if test="${not empty briefObject.collectionId}">
                    <p><strong>Archival Collection ID: </strong><c:out value="${briefObject.collectionId}"></c:out></p>
                </c:if>
                <p><strong>Finding Aid: </strong>
                    <c:choose>
                        <c:when test="${not empty findingAidUrl}">
                            <a href="<c:out value="${findingAidUrl}"/>"><c:out value="${findingAidUrl}"/></a>
                        </c:when>
                        <c:otherwise>Doesn’t have a finding aid</c:otherwise>
                    </c:choose>
                </p>
                <c:if test="${not empty briefObject.abstractText}">
                    <c:set var="truncatedAbstract" value="${cdr:truncateText(briefObject.abstractText, 350)}"/>
                    <c:choose>
                        <c:when test="${fn:length(briefObject.abstractText) > 350}">
                            <p id="truncated-abstract"><c:out value="${truncatedAbstract}"/>... <a class="abstract-text" id="show-abstract" href="#">Read more</a></p>
                            <p id="full-abstract" class="hidden"><c:out value="${briefObject.abstractText}"/> <a class="abstract-text" id="hide-abstract" href="#">Read less</a></p>
                        </c:when>
                        <c:otherwise>
                            <p><c:out value="${briefObject.abstractText}"/></p>
                        </c:otherwise>
                    </c:choose>
                </c:if>
                <c:if test="${not empty exhibits }">
                    <p><strong>Related Digital Exhibits:</strong>
                        <c:forEach var="exhibit" items="${exhibits}" varStatus="status">
                            <a href="${exhibit.value}">${exhibit.key}</a><c:if test="${not status.last}">; </c:if>
                        </c:forEach>
                    </p>
                </c:if>

                <p><a id="metadata-modal-link" href="#">View Additional Metadata</a></p>
            </div>
            <c:if test="${hasRestrictedContent && empty cdr:getUsername()}">
                <div class="column is-narrow-desktop item-actions">
                    <c:import url="fullRecord/accessInfo.jsp" />
                </div>
            </c:if>
        </div>
    </div>
    <script type="text/javascript" src="/static/js/public/abstractDisplay"></script>
    <div class="collection-info-bottom">
        <c:import url="fullRecord/browseView.jsp"/>
    </div>
</div>