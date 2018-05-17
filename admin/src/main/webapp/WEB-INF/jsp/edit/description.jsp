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

<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
<link rel="stylesheet" type="text/css" href="/static/css/admin/jquery.xmleditor.css" />

<script>
	var require = {
		config: {
			'editDescription' : {
				'recordUrl' : '${accessBaseUrl}/record/${resultObject.id}',
				'originalUrl' : '${accessBaseUrl}/${cdr:getOriginalFileUrl(resultObject)}'
			}
		}
	};
</script>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/admin/editDescription"></script>

<div class="edit_desc_page contentarea">
	<div>
		<h2>Editing Description</h2>
		<div class="results_header_hierarchy_path"></div>
	</div>

	<div>
		<img id="loading-icon" class="hidden" src="/static/images/admin/loading_large.gif">
		<div id="xml_editor"></div>
	</div>
</div>