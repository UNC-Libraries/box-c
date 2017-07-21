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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>	

			<div class="content-wrap">
<div class="contentarea" id="student-papers-heading">
	<h2>Student Papers</h2>
</div>
	
			<div class="contentarea" id="student-papers-options">
	<h3>Honors Carolina Thesis</h3>
		<p class="button"><a href="/forms/honors-thesis">Deposit Paper</a></p>
							<hr>
				<h3>Master's Paper</h3>
				
				<h4>Department</h4>
				<form>
				<select id="mp-dept">
					<option value=""></option>
					<option value="/forms/art-mfa">Art MFA</option>
					<option value="/forms/sph_ese_technical_report">Gillings ESE Technical Report</option>
					<option value="/forms/sph_hb_cap_submission">Gillings HB Capstone</option>
					<option value="/forms/sph_hp">Gillings Health Policy Management</option>
					<option value="/forms/sph_mch_masters_papers">Gillings MCH</option>
					<option value="/forms/sph_nutrition">Gillings Department of Nutrition</option>
					<option value="/forms/sph_phlp_masters_papers">Gillings PHLP</option>
					<option value="/forms/silsmp">School of Information and Library Science</option>
					</select>
					</form>
			</div>

</div>
<link rel="stylesheet" type="text/css" href="/static/css/jquery-ui.css">
<link rel="stylesheet" type="text/css" href="/static/css/jquery.qtip.min.css">
<script async="" src="//www.google-analytics.com/analytics.js"></script><script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/public/studentPapers"></script>