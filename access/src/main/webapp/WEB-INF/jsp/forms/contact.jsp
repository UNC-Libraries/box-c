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
		<h2>Contact Us</h2>
		<c:choose>
			<c:when test="${requestScope.success}">
				<p>Your message has been submitted.</p>
			</c:when>
			<c:otherwise>
				<p>Please describe any problem(s) you are having with website and/or suggestions for improvement.</p>
				<br/>
				<form:form commandName="contactForm" class="user_form">
					<div class="form_section">
						<label for="name">Name</label>
						<form:input path="personalName" size="40"/>&nbsp;<span>*<form:errors path="personalName" /></span>
					</div>
					<div class="form_section">
						<label for="email">Email Address</label>
						<form:input path="emailAddress" size="40"/>&nbsp;<span>*<form:errors path="emailAddress" /></span>
					</div>
					<div class="form_section">
						<label for="dept">Affiliation</label>
						<form:input path="affiliation" size="40"/>&nbsp;<span><form:errors path="affiliation" /></span>
					</div>
					<div class="form_section">
						<label for="comments">Comments *</label>&nbsp;<span><form:errors path="comments" /></span><br/>
						<form:textarea path="comments" cols="90" rows="8"/>
					</div>
					<div class="form_section">
						${requestScope.reCaptcha}
						<span><form:errors path="recaptcha_challenge_field" /></span>
					</div>
					<div class="form_section">
						<input type="submit" name="submit" value="Submit request"/>
					</div>
					<form:hidden path="referrer"/>
				</form:form>
			</c:otherwise>
		</c:choose>
	</div>
</div>
</div>