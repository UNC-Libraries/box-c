VERSION = ""

build-all: build-admin build-access

build-admin: setup-build-dirs build-admin-npm build-admin-concat

build-access: setup-build-dirs build-access-npm build-access-concat

setup-build-dirs:
	mkdir -p static/build/admin/
	mkdir -p static/assets/admin/
	mkdir -p static/build/access/
	mkdir -p static/assets/access/

build-admin-concat:
	rm -rf static/build/admin/*
	rm -f static/assets/admin/cdr-admin.*

	cat static/js/admin/lib/jquery.min.js > static/build/admin/cdr-admin.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> static/build/admin/cdr-admin.js
	cat static/js/admin/lib/jquery-ui.min.js >> static/build/admin/cdr-admin.js
	echo "});" >> static/build/admin/cdr-admin.js
	awk 'FNR==1 && NR!=1 {print ""} {print}' \
		static/js/admin/lib/jquery.detachplus.js \
		static/js/admin/lib/moment.min.js \
		static/js/public/src/Structure* \
		static/js/public/src/ResourceTypeUtilities.js \
		static/js/admin/src/*.js \
		static/js/admin/src/*/*.js \
		>> static/build/admin/cdr-admin.js
	sass static/css/sass/cdr_vue_modal_styles.scss static/build/admin/cdr_vue_modal_styles.css --style "expanded"

	awk 'FNR==1 && NR!=1 {print ""} {print}' \
		static/css/reset.css \
		static/css/cdr_common.css \
		static/css/admin/jquery-ui.css \
		static/css/admin/jquery.qtip.css \
		static/css/admin/jquery.contextMenu.css \
		static/css/admin/jquery.xmleditor.css \
		static/css/admin/cdradmin.css \
		static/css/admin/search_results.css \
		static/css/admin/admin_forms.css \
		static/css/fontawesome.min.css \
		static/css/structure_browse.css \
		static/css/admin/status_monitor.css \
		static/build/admin/cdr_vue_modal_styles.css \
		static/js/admin/vue-cdr-admin/dist/assets/index.css \
		> static/build/admin/cdr-admin.css

	cp static/build/admin/cdr-admin.js static/assets/admin/
	cp static/build/admin/cdr-admin.css static/assets/admin/
	cp -r static/css/images static/assets/admin/

ifneq ($(VERSION), "")
	for i in static/js/admin/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif

build-admin-npm:
	rm -f static/assets/admin/vue-admin-index.js

	# Build vue admin application files
	npm --prefix static/js/admin/vue-cdr-admin ci
	npm --prefix static/js/admin/vue-cdr-admin run build

	cp static/js/admin/vue-cdr-admin/dist/assets/vue-admin-index.js static/assets/admin/vue-admin-index.js

build-access-concat:
	rm -rf static/build/access/*
	rm -f static/assets/access/cdr-access.*

	awk 'FNR==1 && NR!=1 {print ""} {print}' \
		static/css/sass/cdr_homepage.scss \
		static/css/sass/cdr_ui_styles.scss \
		static/css/sass/cdr_vue_modal_styles.scss \
		> static/build/access/cdr-ui.scss
	sass static/build/access/cdr-ui.scss  static/build/access/cdr-ui.css --style "expanded"

	awk 'FNR==1 && NR!=1 {print ""} {print}' \
		static/css/bulma-no-dark-mode.min.css \
		static/css/cdrui_styles.css \
		static/build/access/cdr-ui.css \
		static/css/fontawesome.min.css \
		> static/build/access/cdr-access.css

	cp static/build/access/cdr-access.css static/assets/access/
	cp -r static/webfonts static/assets/

SUSPEND = "n"

build-access-npm:
	rm -f static/assets/access/vue-access-index.js

	# Build vue application(s) files
	npm --prefix static/js/vue-cdr-access ci
	npm --prefix static/js/vue-cdr-access run build

	# Minify viewer.js file for pdf viewer (Uncomment the lines below if changes are made to the viewer.js file. Requires nodejs 15.x or higher)
	# npm install minify -g
	# minify static/plugins/pdfjs/web/viewer.js > static/plugins/pdfjs/web/viewer.min.js

	cp static/js/vue-cdr-access/dist/assets/access/vue-access-index.js static/assets/access/vue-access-index.js
	cp static/js/vue-cdr-access/dist/assets/access/index.css static/assets/access/index.css

SUSPEND = "n"

build-bxc:
	mvn clean install -DskipTests -pl common-utils,access,access-common,admin,deposit,fcrepo-clients,metadata,model-api,model-fcrepo,persistence,auth-api,auth-fcrepo,services,services-camel,indexing-solr,solr-search,sword-server,integration

verify-bxc:
	mvn verify -pl common-utils,access,access-common,admin,deposit,fcrepo-clients,metadata,model-api,model-fcrepo,persistence,auth-api,auth-fcrepo,services,services-camel,indexing-solr,solr-search,sword-server,integration

ifneq ($(VERSION), "")
	for i in static/js/public/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif
