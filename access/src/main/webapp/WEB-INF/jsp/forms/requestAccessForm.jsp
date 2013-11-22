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

<div class="content-wrap">
<div class="onecol">
	<div class="contentarea">
		<h2>Request Access
			<c:if test="${not empty metadata}">
				to ${metadata.title}
			</c:if>
		</h2>
		
		<c:if test="${not empty metadata && not empty metadata.parentCollection}">
			<c:url var="parentUrl" scope="page" value="/search">
				<c:param name="${searchSettings.searchStateParams['FACET_FIELDS']}" 
						value="${searchSettings.searchFieldParams['ANCESTOR_PATH']}:${metadata.parentCollectionObject.limitToValue}"/>
			</c:url>
			<p>From collection: <a href="${parentUrl}">${metadata.parentCollectionObject.displayValue }</a></p>
		</c:if>
		
		<c:choose>
			<c:when test="${requestScope.success}">
				<p>Your request has been submitted.  You will receive an update on the status of your request within two business days.</p>
			</c:when>
			<c:otherwise>
				<form:form commandName="requestAccessForm" class="user_form">
					<p>Use this form to request access to materials held in the Carolina Digital Repository that are restricted from public view.</p>

					<p>A restricted status does not necessarily mean that a collection cannot be viewed, it simply means that those wishing to access 
					the collection must first obtain permission from the collection owner.  
					Please note that requesting access does not guarantee that access will be granted.</p>

					<p>When your request is submitted, Library staff will forward your request to the collection owner for consideration.  
					The collection owner will then determine if access to the requested content can be granted and will get in touch with you directly.</p>
					<br/>
					<div class="form_section">
						<label for="name">Name</label>
						<form:input path="personalName" size="40"/>&nbsp;<span>*<form:errors path="personalName" /></span>
					</div>
					<div class="form_section">
						<label for="name">UNC Username/onyen</label>
						<form:input path="username" size="40"/>
					</div>
					<div class="form_section">
						<label for="name">Email Address</label>
						<form:input path="emailAddress" size="40"/>&nbsp;<span>*<form:errors path="emailAddress" /></span>
					</div>
					<div class="form_section">
						<label for="name">Affiliation</label>
						<form:input path="affiliation" size="40"/>
					</div>
					<div class="form_section">
						<label for="name">Phone Number</label>
						<form:input path="phoneNumber" size="40"/>
					</div>
					<div class="form_section">
						<p>Please provide a brief description of the materials you are requesting and include a brief explanation of why you are 
						requesting access to the materials.</p>
						<form:textarea path="comments" cols="90" rows="8"/>
					</div>
					<div class="form_section">
						${requestScope.reCaptcha}
						<span><form:errors path="recaptcha_challenge_field" /></span>
					</div>
					<div class="form_section">
						<input type="submit" name="submit" value="Submit request"/>
					</div>
					
					<form:hidden path="requestedId" value="${metadata.id}"/>
					<form:hidden path="requestedTitle" value="${metadata.title}"/>
				</form:form>
			</c:otherwise>
		</c:choose>
	</div>
</div>
</div>