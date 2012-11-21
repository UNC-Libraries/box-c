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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page trimDirectiveWhitespaces="true" %>
<div id="footer" class="darkest shadowtop">
	<div class="threecol dark shadowtop">
		<div class="contentarea">
			<p>
				<a href="${pageContext.request.contextPath}/">Home</a>&nbsp;|&nbsp;
				<a href="search?types=Collection">Browse Collections</a>&nbsp;|&nbsp;
				<a href="external?page=about.about">About</a>&nbsp;|&nbsp;
				<c:url var="contactUrl" scope="page" value="external">
					<c:param name="page" value="contact" />
					<c:choose>
						<c:when test="${param.page == 'contact'}">
							<c:param name="refer" value="${param.refer}"/>
						</c:when>
						<c:otherwise>
							<c:param name="refer" value="${currentAbsoluteUrl}"/>
						</c:otherwise>
					</c:choose>
				</c:url>
				<a href="<c:out value='${contactUrl}'/>">Contact Us</a>&nbsp;|&nbsp;
				<a href="http://www.lib.unc.edu">Library Home</a>&nbsp;|&nbsp;
				<a href="http://www.lib.unc.edu/aoffice/policies/privacy_policy.html">Privacy Policy</a>
			</p>
		</div>
	</div>
	<div class="fourcol darkest right shadowtop">
		<div class="contentarea"><p class="left"><a href="http://www.unc.edu">UNC Home </a></p><a href="http://www.unc.edu"><img src="/static/images/uncwell.png" id="footer_uncwell"></a></div>
	</div>
</div>