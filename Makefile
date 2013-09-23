build-admin:
	cat access/src/main/external/static/js/lib/jquery.min.js > access/src/main/external/static/js/cdr-admin.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> access/src/main/external/static/js/cdr-admin.js
	cat access/src/main/external/static/js/lib/jquery-ui.min.js >> access/src/main/external/static/js/cdr-admin.js
	echo "});" >> access/src/main/external/static/js/cdr-admin.js
	cat access/src/main/external/static/js/admin/lib/jquery.detachplus.js \
		access/src/main/external/static/js/lib/moment.min.js \
		access/src/main/external/static/js/src/Structure* \
		access/src/main/external/static/js/admin/src/*.js \
		access/src/main/external/static/js/admin/src/*/*.js \
		>> access/src/main/external/static/js/cdr-admin.js
	
	cat access/src/main/external/static/css/reset.css \
		access/src/main/external/static/css/cdr_common.css \
		access/src/main/external/static/css/admin/jquery-ui.css \
		access/src/main/external/static/css/admin/jquery.qtip.css \
		access/src/main/external/static/css/admin/cdradmin.css \
		access/src/main/external/static/css/admin/search_results.css \
		access/src/main/external/static/css/admin/admin_forms.css \
		access/src/main/external/static/css/structure_browse.css \
		> access/src/main/external/static/css/cdr_admin.css