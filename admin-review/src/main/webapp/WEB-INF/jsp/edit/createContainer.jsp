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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<div class="create_container">
	<h3>Create Container</h3>
	<div class="form_field">
		<label>Name</label>
		<form:input path="name" size="40"/>
	</div>
	<div class="form_field">
		<label>Type</label>
		<form:select path="type">
			<form:option value="folder">Folder</form:option>
			<form:option value="collection">Collection</form:option>
		</form:select>
	</div>
	<p>Optional</p>
	<div class="form_field">
		<label>Description</label>
		<input type="file" name="description"/>
	</div>
			
		<c:set var="publishedAttr" value="${targetACLs.getAttribute('published', aclNS)}" />
		<a class="boolean_toggle" data-field="published">
			<c:choose>
				<c:when test="${not empty publishedAttr && publishedAttr.value == 'false'}">No</c:when>
				<c:otherwise>Yes</c:otherwise>
			</c:choose>
		</a>
		<c:if test="${targetMetadata.status.contains('Parent Unpublished')}">
			&nbsp;(Parent is unpublished)
		</c:if>
	</div>
	