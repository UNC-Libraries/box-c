<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI"%>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>

<c:choose>
    <c:when test="${not empty briefObject.countMap}">
        <c:set var="childCount" value="${briefObject.countMap.child}"/>
    </c:when>
    <c:otherwise>
        <c:set var="childCount" value="0"/>
    </c:otherwise>
</c:choose>

<div class="full_record_top">
    <div class="collinfo_metadata browse-header aggregate-record">
        <c:import url="fullRecord/navigationBar.jsp" />
        <div class="columns">
            <div class="column is-8">
                <h2 class="item-title ${isDeleted}"><c:out value="${briefObject.title}" /></h2>
                <div class="columns columns-resize aggregate-info">
                    <div class="column is-narrow-tablet ${isDeleted}">
                        <c:set var="thumbnailObject" value="${briefObject}" scope="request" />
                        <c:import url="common/thumbnail.jsp">
                            <c:param name="target" value="file" />
                            <c:param name="size" value="large" />
                        </c:import>
                    </div>
                    <div class="column">
                        <ul>
                            <c:if test="${not empty briefObject.dateAdded}">
                                <li><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['DATE_ADDED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateAdded}" /></li>
                            </c:if>
                            <c:if test="${not empty briefObject.parentCollection && briefObject.ancestorPathFacet.highestTier > 0}">
                                <li>
                                    <c:url var="parentUrl" scope="page" value="record/${briefObject.parentCollectionId}" />
                                    <span class="has-text-weight-bold">Collection:</span>
                                    <a href="<c:out value='${parentUrl}' />"><c:out value="${briefObject.parentCollectionName}"/></a>
                                </li>
                            </c:if>
                            <c:if test="${not empty collectionId}">
                                <li>
                                    <span class="has-text-weight-bold">Archival Collection ID: </span>
                                    <c:out value="${collectionId}"></c:out>
                                </li>
                            </c:if>
                            <li><span class="has-text-weight-bold">Finding Aid: </span>
                                <c:choose>
                                    <c:when test="${not empty findingAidUrl}">
                                        <a href="<c:out value="${findingAidUrl}"/>"><c:out value="${findingAidUrl}"/></a>
                                    </c:when>
                                    <c:otherwise>Doesn’t have a finding aid</c:otherwise>
                                </c:choose>
                            </li>
                            <c:if test="${not empty briefObject.creator}">
                                <li>
                                    <span class="has-text-weight-bold">Creator<c:if test="${fn:length(briefObject.creator) > 1}">s</c:if>:</span>
                                    <c:forEach var="creatorObject" items="${briefObject.creator}" varStatus="creatorStatus">
                                        <c:out value="${creatorObject}"/><c:if test="${!creatorStatus.last}">; </c:if>
                                    </c:forEach>
                                </li>
                            </c:if>

                            <c:choose>
                                <c:when test="${not empty briefObject.fileFormatType}">
                                    <li><span class="has-text-weight-bold">File Type:</span> <c:out value="${cdr:getFileType(briefObject)}" /></li>
                                    <c:if test="${briefObject.filesizeSort != -1}"> <li><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['FILESIZE']}:</span> <c:out value="${cdr:formatFilesize(briefObject.filesizeSort, 1)}"/></li></c:if>
                                </c:when>
                                <c:otherwise>
                                    <li><span class="has-text-weight-bold">Contains:</span> ${childCount} item<c:if test="${childCount != 1}">s</c:if></li>
                                </c:otherwise>
                            </c:choose>

                            <c:if test="${not empty briefObject.dateCreated}"><li><span class="has-text-weight-bold">${searchSettings.searchFieldLabels['DATE_CREATED']}:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${briefObject.dateCreated}" /></li></c:if>
                            <c:if test="${not empty embargoDate}"><li><span class="has-text-weight-bold">Embargoed Until:</span> <fmt:formatDate pattern="yyyy-MM-dd" value="${embargoDate}" /></li></c:if>
                            <c:if test="${not empty briefObject.abstractText}">
                                <c:set var="truncatedAbstract" value="${cdr:truncateText(briefObject.abstractText, 350)}"/>
                                <c:choose>
                                    <c:when test="${fn:length(briefObject.abstractText) > 350}">
                                        <li id="truncated-abstract" class="abstracts"><c:out value="${truncatedAbstract}"/>... <a class="abstract-text" id="show-abstract" href="#">Read more</a></li>
                                        <li id="full-abstract" class="hidden abstracts"><c:out value="${briefObject.abstractText}"/> <a class="abstract-text" id="hide-abstract" href="#">Read less</a></li>
                                    </c:when>
                                    <c:otherwise>
                                        <li class="abstracts"><c:out value="${briefObject.abstractText}"/></li>
                                    </c:otherwise>
                                </c:choose>
                            </c:if>
                            <c:if test="${not empty exhibits }">
                                <li><span class="has-text-weight-bold">Related Digital Exhibits:</span>
                                    <c:forEach var="exhibit" items="${exhibits}" varStatus="status">
                                        <a href="${exhibit.value}">${exhibit.key}</a><c:if test="${not status.last}">; </c:if>
                                    </c:forEach>
                                </li>
                            </c:if>
                        </ul>
                    </div>
                </div>
            </div>
            <div class="column is-narrow-desktop action-btn item-actions">
                <c:if test="${hasRestrictedContent && empty cdr:getUsername()}">
                    <c:import url="fullRecord/accessInfo.jsp" />
                </c:if>
                <c:if test="${permsHelper.hasEditAccess(accessGroupSet, briefObject)}">
                    <s:eval var="editDescriptionUrl" expression=
                            "T(edu.unc.lib.boxc.common.util.URIUtil).join(adminBaseUrl, 'describe', briefObject.id)" />
                    <div class="actionlink right"><a class="button" href="${editDescriptionUrl}"><i class="fa fa-edit"></i> Edit</a></div>
                </c:if>

                <c:choose>
                    <c:when test="${not empty dataFileUrl}">
                        <div class="actionlink right download">
                            <a class="button" href="${dataFileUrl}?dl=true"><i class="fa fa-download"></i> Download</a>
                        </div>
                    </c:when>
                    <c:when test="${not empty embargoDate && not empty dataFileUrl}">
                        <div class="noaction right">
                            Available after <fmt:formatDate value="${embargoDate}" pattern="d MMMM, yyyy"/>
                        </div>
                    </c:when>
                </c:choose>
            </div>
        </div>

    </div>

    <div class="clear">
        <c:choose>
            <c:when test="${viewerType == 'uv'}">
                <div class="clear_space"></div>
                <link rel="stylesheet" href="/static/plugins/uv/uv.css">
                <div id="jp2_viewer" class="jp2_imageviewer_window" data-url="${briefObject.id}"></div>
            </c:when>
            <c:when test="${viewerType == 'pdf'}">
                <c:import url="fullRecord/pdfViewer.jsp" />
            </c:when>
            <c:when test="${viewerType == 'audio'}">
                <div class="clear_space"></div>
                <audio class="audio_player inline_viewer" src="${dataFileUrl}">
                </audio>
            </c:when>
        </c:choose>
    </div>
</div>

<%-- Reenable once child counts are working --%>
<c:if test="${childCount > 0}">
    <link rel="stylesheet" href="/static/plugins/DataTables/datatables.min.css">
    <link rel="stylesheet" href="/static/plugins/DataTables/Responsive-2.2.2/css/responsive.dataTables.css">

    <div class="child-records">
        <h3>List of Items in This Work (${childCount})</h3>
        <table id="child-files" class="responsive" data-pid="${briefObject.id}">
            <thead>
            <tr>
                <th><span class="sr-only">Thumbnail for file</span></th>
                <th>Title</th>
                <th>File Type</th>
                <th>File Size</th>
                <th><span class="sr-only">View file</span></th>
                <th><span class="sr-only">Download file</span></th>
                <c:if test="${permsHelper.hasEditAccess(accessGroupSet, briefObject)}">
                    <th><span class="sr-only">Edit MODS</span></th>
                </c:if>
            </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>
</c:if>
<c:if test="${permsHelper.hasDescriptionAccess(accessGroupSet, briefObject)}">
    <h2 class="full-metadata">Detailed Metadata</h2>
    <div id="mods_data_display" data-pid="${briefObject.id}"></div>
</c:if>
<c:import url="fullRecord/neighborList.jsp" />
<script type="text/javascript" src="/static/js/public/abstractDisplay"></script>