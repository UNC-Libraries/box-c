<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cdr" uri="http://cdr.lib.unc.edu/cdrUI" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page trimDirectiveWhitespaces="true" %>
<div class="result_page contentarea">
	<div class="search_menu"></div>

	<div class="result_area">
		<div>
		</div>
	</div>
</div>

<script>
	var require = {
			config: {
				'reviewList' : {
					'resultUrl' : '${currentRelativeUrl}',
					'accessBaseUrl' : '${accessBaseUrl}',
					'adminBaseUrl' : '${adminBaseUrl}'
				}
			}
	};
</script>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/admin/reviewList"></script>