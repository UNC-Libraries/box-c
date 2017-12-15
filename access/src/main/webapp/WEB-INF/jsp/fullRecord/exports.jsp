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
<div class="contentarea">
	<h2>Export Metadata</h2>
	<ul>
		<c:if test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'RELS-EXT', briefObject)}">
			<li><a href="${cdr:getDatastreamUrl(briefObject.id, 'RELS-EXT', fedoraUtil)}">Fedora Object-to-Object Relationships</a></li>
		</c:if>
		<c:if test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'techmd_fits', briefObject)}">
			<li><a href="${cdr:getDatastreamUrl(briefObject.id, 'techmd_fits', fedoraUtil)}">FITS Extract</a></li>
		</c:if>
		<c:if test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'MD_DESCRIPTIVE', briefObject)}">
			<li><a href="${cdr:getDatastreamUrl(briefObject.id, 'MD_DESCRIPTIVE', fedoraUtil)}">MODS</a></li>
		</c:if>
		<c:if test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'DC', briefObject)}">
			<li><a href="${cdr:getDatastreamUrl(briefObject.id, 'DC', fedoraUtil)}">OAI Dublin Core</a></li>
		</c:if>
		<c:if test="${cdr:permitDatastreamAccess(requestScope.accessGroupSet, 'MD_EVENTS', briefObject)}">
			<li><a href="${cdr:getDatastreamUrl(briefObject.id, 'MD_EVENTS', fedoraUtil)}">PREMIS Events</a></li>
		</c:if>
	</ul>
</div>