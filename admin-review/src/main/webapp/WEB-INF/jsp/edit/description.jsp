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
	var pid = new PID(resultObject.id);

	var modsDocument = null;
	
	$(document).ready(function() {
		$("#mods_editor").modsEditor({
			schemaObject : Mods,
			ajaxOptions : {
				modsRetrievalPath : "/admin/" + pid.getPath() + "/mods",
				modsRetrievalParams : {'pid' : pid.getPID()},
				modsUploadPath : "/admin/" + pid.getPath() + "/describe"
			}
		});
		$(window).resize();
	});
</script>

<div class="edit_desc_page contentarea">
	<div class="contentarea">
		<h2>Reviewing items</h2>
		<c:set var="facetNodes" scope="request" value="${resultObject.path.facetNodes}"/>
		<div class="results_header_hierarchy_path">
			<c:import url="/jsp/util/hierarchyTrail.jsp" />
		</div>
	</div>

	<div class="contentarea">
		<div id="mods_editor"></div>
	</div>
</div>