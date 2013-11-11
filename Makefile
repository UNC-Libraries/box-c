VERSION = ""

ADMIN_JS_SRC = \
	access/src/main/external/static/js/lib/jquery.min.js \
	access/src/main/external/static/js/lib/jquery-ui.min.wrap.js \
	access/src/main/external/static/js/admin/lib/jquery.detachplus.js \
	access/src/main/external/static/js/lib/moment.min.js \
	access/src/main/external/static/js/public/src/Structure* \
	access/src/main/external/static/js/admin/src/*.js \
	access/src/main/external/static/js/admin/src/*/*.js

ACCESS_JS_SRC = \
	access/src/main/external/static/js/lib/jquery.min.js \
	access/src/main/external/static/js/lib/jquery-ui.min.wrap.js \
	access/src/main/external/static/js/lib/jquery.preload-1.0.8-unc.js \
	access/src/main/external/static/js/public/src/*.js

ADMIN_CSS_SRC = \
	access/src/main/external/static/css/reset.css \
	access/src/main/external/static/css/cdr_common.css \
	access/src/main/external/static/css/admin/jquery-ui.css \
	access/src/main/external/static/css/admin/jquery.qtip.css \
	access/src/main/external/static/css/admin/cdradmin.css \
	access/src/main/external/static/css/admin/search_results.css \
	access/src/main/external/static/css/admin/admin_forms.css \
	access/src/main/external/static/css/structure_browse.css

ACCESS_CSS_SRC = \
	access/src/main/external/static/css/reset.css \
	access/src/main/external/static/css/cdr_common.css \
	access/src/main/external/static/css/cdrui_styles.css \
	access/src/main/external/static/css/fluid_cap.css \
	access/src/main/external/static/css/structure_browse.css \
	access/src/main/external/static/front/front.css \
	access/src/main/external/static/front/peek.css

OUT = \
	access/src/main/external/static/js/cdr-admin.js \
	access/src/main/external/static/js/cdr-access.js \
	access/src/main/external/static/css/cdr_admin.css \
	access/src/main/external/static/css/cdr_access.css


all: $(OUT) update-version-url-args

access/src/main/external/static/js/lib/jquery-ui.min.wrap.js: access/src/main/external/static/js/lib/jquery-ui.min.js
	( echo "define('jquery-ui', ['jquery'], function (\$$\$$) {"; cat $<; echo "});" ) > $@

access/src/main/external/static/js/cdr-admin.js: $(ADMIN_JS_SRC)
	cat $(ADMIN_JS_SRC) > $@

access/src/main/external/static/js/cdr-access.js: $(ACCESS_JS_SRC)
	cat $(ACCESS_JS_SRC) > $@

access/src/main/external/static/css/cdr_admin.css: $(ADMIN_CSS_SRC)
	cat $(ADMIN_CSS_SRC) > $@

access/src/main/external/static/css/cdr_access.css: $(ACCESS_CSS_SRC)
	cat $(ACCESS_CSS_SRC) > $@

update-version-url-args:
ifneq ($(VERSION), "")
	for i in access/src/main/external/static/js/admin/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif
