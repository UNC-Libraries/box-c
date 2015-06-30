VERSION = ""

build-all: build-admin build-access

build-admin:
	cat access/src/main/external/static/js/lib/jquery.min.js > access/src/main/external/static/js/cdr-admin.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> access/src/main/external/static/js/cdr-admin.js
	cat access/src/main/external/static/js/lib/jquery-ui.min.js >> access/src/main/external/static/js/cdr-admin.js
	echo "});" >> access/src/main/external/static/js/cdr-admin.js
	cat access/src/main/external/static/js/admin/lib/jquery.detachplus.js \
		access/src/main/external/static/js/lib/moment.min.js \
		access/src/main/external/static/js/public/src/Structure* \
		access/src/main/external/static/js/admin/src/*.js \
		access/src/main/external/static/js/admin/src/*/*.js \
		>> access/src/main/external/static/js/cdr-admin.js
	
	cat access/src/main/external/static/css/reset.css \
		access/src/main/external/static/css/cdr_common.css \
		access/src/main/external/static/css/admin/jquery-ui.css \
		access/src/main/external/static/css/admin/jquery.qtip.css \
		access/src/main/external/static/css/admin/jquery.contextMenu.css \
		access/src/main/external/static/css/admin/cdradmin.css \
		access/src/main/external/static/css/admin/search_results.css \
		access/src/main/external/static/css/admin/admin_forms.css \
		access/src/main/external/static/css/admin/collector.css \
		access/src/main/external/static/css/structure_browse.css \
		> access/src/main/external/static/css/cdr_admin.css
	
ifneq ($(VERSION), "")
	for i in access/src/main/external/static/js/admin/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif

build-access:
	cat access/src/main/external/static/js/lib/jquery.min.js > access/src/main/external/static/js/cdr-access.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> access/src/main/external/static/js/cdr-access.js
	cat access/src/main/external/static/js/lib/jquery-ui-access.min.js >> access/src/main/external/static/js/cdr-access.js
	echo "});" >> access/src/main/external/static/js/cdr-access.js
	cat \
		access/src/main/external/static/js/public/src/*.js \
		>> access/src/main/external/static/js/cdr-access.js
		
	cat access/src/main/external/static/css/reset.css \
		access/src/main/external/static/css/cdr_common.css \
		access/src/main/external/static/css/cdrui_styles.css \
		access/src/main/external/static/css/fluid_cap.css \
		access/src/main/external/static/css/font-awesome.min.css \
		access/src/main/external/static/css/structure_browse.css \
		access/src/main/external/static/front/front.css \
		access/src/main/external/static/front/peek.css \
		> access/src/main/external/static/css/cdr_access.css
	
ifneq ($(VERSION), "")
	for i in access/src/main/external/static/js/public/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif
