<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>

<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
<link rel="stylesheet" type="text/css" href="/static/css/admin/jquery.xmleditor.css" />

<s:eval var="viewRecordUrl" expression=
	"T(edu.unc.lib.boxc.common.util.URIUtil).join(accessBaseUrl, 'record', resultObject.id)" />
<s:eval var="originalSubpath" expression=
	"T(edu.unc.lib.boxc.web.common.utils.DatastreamUtil).getOriginalFileUrl(resultObject)" />
<s:eval var="originalFileUrl" expression=
	"T(edu.unc.lib.boxc.common.util.URIUtil).join(accessBaseUrl, originalSubpath)" />

<script>
	var require = {
		config: {
			'editDescription' : {
				'recordUrl' : '${viewRecordUrl}',
				'originalUrl' : '${originalFileUrl}'
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