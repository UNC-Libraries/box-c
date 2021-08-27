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

<style>
#survey-banner {
	position: relative;
	background: #ff9;
	height: 36px;
	line-height: 36px;
	font-size: 18px;
	font-family: helvetica;
	text-align: center;
}

#survey-banner .hide {
	position: absolute;
	top: 0;
	right: 8px;
	height: 36px;
	line-height: 36px;
}

#survey-banner a {
	color: #000;
	text-decoration: underline;
}
</style>

<div id="survey-banner" style="display: none;">
	Help us improve the CDR. <a href="https://unc.az1.qualtrics.com/SE/?SID=SV_8HrpGlGgdAdieWx">Take a short survey</a>.
	<div class="hide"><a href="javascript:void(0)">Hide</a></div>
</div>

<script>
if (document.cookie.indexOf("hideSurveyBanner=yes") == -1) {
	var banner = document.getElementById("survey-banner");
	banner.style.display = "";
	
	var links = banner.getElementsByTagName("a");
	for (var i = 0; i < links.length; i++) {
		links.item(i).onclick = function() {
			var date = new Date();
			date.setTime(date.getTime()+(120*24*60*60*1000));
			document.cookie = "hideSurveyBanner=yes; expires="+date.toGMTString()+"; path=/";
			banner.style.display = "none";
		}
	}
}
</script>
