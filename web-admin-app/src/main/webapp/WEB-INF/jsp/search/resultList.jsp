<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page trimDirectiveWhitespaces="true" %>
<div class="result_page contentarea">
	<div class="search_menu"></div>

	<div class="result_area">
		<div>
			Loading... <img src="/static/images/admin/loading_small.gif"/>
		</div>
	</div>
</div>

<script>
	var require = {
		config: {
			'resultList' : {
				'resultUrl' : '${currentRelativeUrl}',
				'accessBaseUrl' : '${accessBaseUrl}',
				'adminBaseUrl' : '${adminBaseUrl}',
				'formsBaseUrl' : '${formsBaseUrl}'
			}
		}
	};
</script>
<script type="text/javascript" src="/static/js/admin/lib/require.js" data-main="/static/js/admin/resultList"></script>