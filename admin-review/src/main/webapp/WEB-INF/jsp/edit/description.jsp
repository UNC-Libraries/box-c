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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>


<link rel="stylesheet" type="text/css" href="/static/css/jquery.modseditor.css" />

<script src="/static/js/modseditor/lib/jquery.xmlns.js"></script>
<script src="/static/js/expanding.js"></script>
<script src="/static/js/modseditor/jquery.modseditor.js"></script>
<script src="/static/js/modseditor/lib/json2.js"></script>
<script src="/static/js/modseditor/lib/cycle.js"></script>
<script src="/static/js/modseditor/lib/ace/src-min/ace.js"></script>
<script src="/static/js/modseditor/lib/vkbeautify.0.98.01.beta.js"></script>
<script src="/static/js/admin/PID.js"></script>
<script src="/static/schemas/mods-3-4/mods-3-4.js"></script>

<script>
	var resultObject = ${cdr:objectToJSON(resultObject)};
	var originalUrl = '${cdr:getDatastreamUrl(resultObject, "DATA_FILE", fedoraUtil)}';
	
	var pid = new PID(resultObject.id);
	
	var menuEntries = (originalUrl)? [{
		insertPath : ["View"],
		label : 'View original document',
		enabled : true,
		binding : null,
		action : originalUrl
	}, {
		label : 'View Document',
		enabled : true, 
		itemClass : 'header_mode_tab',
		action : originalUrl
	}]: null;

	var modsDocument = null;
	
	$(document).ready(function() {
		$("#mods_editor").modsEditor({
			schemaObject : Mods,
			ajaxOptions : {
				modsRetrievalPath : "/admin/" + pid.getPath() + "/mods",
				modsRetrievalParams : {'pid' : pid.getPID()},
				modsUploadPath : "/admin/describe/" + pid.getPath()
			},
			'menuEntries': menuEntries
		});
		$(window).resize();
	});
</script>

<div class="edit_desc_page contentarea">
	<div class="contentarea">
		<h2>Reviewing items</h2>
		<c:set var="facetNodes" scope="request" value="${resultObject.path.facetNodes}"/>
		<div class="results_header_hierarchy_path">
			<c:import url="/jsp/util/pathTrail.jsp">
				<c:param name="resultOperation">${sessionScope.resultOperation}</c:param>
			</c:import>
		</div>
	</div>

	<div class="contentarea">
		<div id="mods_editor"></div>
	</div>
</div>