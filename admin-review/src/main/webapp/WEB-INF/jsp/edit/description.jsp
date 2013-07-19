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


<link rel="stylesheet" type="text/css" href="/static/css/admin/jquery.xmleditor.css" />

<script>
	var require = {
		config: {
			'editDescription' : {
				'resultObject': ${cdr:objectToJSON(resultObject)},
				'originalUrl' : '${cdr:getDatastreamUrl(resultObject, "DATA_FILE", fedoraUtil)}'
			}
		}
	};
</script>
<script type="text/javascript" src="/static/js/require.js" data-main="/static/js/admin/editDescription"></script>

<div class="edit_desc_page contentarea">
	<div>
		<h2>Reviewing items</h2>
		<c:set var="facetNodes" scope="request" value="${resultObject.path.facetNodes}"/>
		<div class="results_header_hierarchy_path">
			<c:import url="/jsp/util/pathTrail.jsp">
				<c:param name="resultOperation">${sessionScope.resultOperation}</c:param>
			</c:import>
		</div>
	</div>

	<div>
		<div id="xml_editor"></div>
	</div>
</div>