VERSION = ""

build-all: build-admin build-access

build-admin:
	# Build vue permissions application files
	npm --prefix static/js/admin/vue-permissions-editor install

ifeq ($(DEPLOY_TYPE), prod)
	npm --prefix static/js/admin/vue-permissions-editor run build
else
	npm --prefix static/js/admin/vue-permissions-editor run build-dev
endif

	cat /dev/null > static/js/vue-permissions.js

	cat static/js/admin/vue-permissions-editor/dist/js/chunk-vendors*js > static/js/vue-permissions.js


	# Add new line so app*.js doesn't get commented out
	echo >> static/js/vue-permissions.js

	cat static/js/admin/vue-permissions-editor/dist/js/app*js >> static/js/vue-permissions.js

	cat static/js/lib/jquery.min.js > static/js/cdr-admin.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> static/js/cdr-admin.js
	cat static/js/lib/jquery-ui.min.js >> static/js/cdr-admin.js
	echo "});" >> static/js/cdr-admin.js
	cat static/js/admin/lib/jquery.detachplus.js \
		static/js/lib/moment.min.js \
		static/js/public/src/Structure* \
		static/js/public/src/ResourceTypeUtilities.js \
		static/js/admin/src/*.js \
		static/js/admin/src/*/*.js \
		>> static/js/cdr-admin.js

	sass static/css/sass/cdr_vue_modal_styles.scss  static/css/cdr_vue_modal_styles.css --style "expanded"

	cat static/css/reset.css \
		static/css/cdr_common.css \
		static/css/admin/jquery-ui.css \
		static/css/admin/jquery.qtip.css \
		static/css/admin/jquery.contextMenu.css \
		static/css/admin/cdradmin.css \
		static/css/admin/search_results.css \
		static/css/admin/admin_forms.css \
		static/css/admin/collector.css \
		static/css/admin/fontawesome/all.min.css \
		static/css/structure_browse.css \
		static/css/cdr_vue_modal_styles.css \
		> static/css/cdr_admin.css

ifeq ($(DEPLOY_TYPE), prod)
	cat static/js/admin/vue-permissions-editor/dist/css/app*css >> static/css/cdr_admin.css
endif

ifneq ($(VERSION), "")
	for i in static/js/admin/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif

build-access:
# Build vue application(s) files
	npm --prefix static/js/vue-cdr-access install

ifeq ($(DEPLOY_TYPE), prod)
	npm --prefix static/js/vue-cdr-access run build
else
	npm --prefix static/js/vue-cdr-access run build-dev
endif

	# Make sure files are empty
	cat /dev/null > static/js/vue-access.js
	cat /dev/null > static/css/sass/cdr-ui.scss

	cat static/css/sass/cdr_homepage.scss \
		static/css/sass/cdr_ui_styles.scss \
		>> static/css/sass/cdr-ui.scss
	sass static/css/sass/cdr-ui.scss  static/css/cdr-ui.css --style "expanded"
	cat static/js/lib/jquery.min.js > static/js/cdr-access.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> static/js/cdr-access.js
	cat static/js/lib/jquery-ui-access.min.js >> static/js/cdr-access.js
	echo "});" >> static/js/cdr-access.js

	cat static/js/vue-cdr-access/dist/js/chunk-vendors*js >> static/js/vue-access.js

	# Add new line so app*.js doesn't get commented out
	echo >> static/js/vue-access.js
	cat static/js/vue-cdr-access/dist/js/app*js >> static/js/vue-access.js

	cat \
		static/js/public/src/*.js \
		>> static/js/cdr-access.js

	cat static/css/reset.css \
		static/css/cdr_common.css \
		static/css/cdrui_styles.css \
		static/css/fluid_cap.css \
		static/css/structure_browse.css \
		static/css/cdr-ui.css \
		static/css/cdr_vue_modal_styles.css \
		> static/css/cdr_access.css

ifeq ($(DEPLOY_TYPE), prod)
	cat static/js/vue-cdr-access/dist/css/app*css >> static/css/cdr_access.css
endif
SUSPEND = "n"

run-access:
	cd access && export MAVEN_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=48008,server=y,suspend=$(SUSPEND)"; mvn jetty:run

run-admin:
	cd admin && export MAVEN_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=48009,server=y,suspend=$(SUSPEND)"; mvn jetty:run
	
run-deposit:
	cd deposit && export MAVEN_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=48010,server=y,suspend=$(SUSPEND)"; mvn jetty:run
	
run-services-camel:
	cd services-camel && export MAVEN_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=48011,server=y,suspend=$(SUSPEND)"; mvn jetty:run

ifneq ($(VERSION), "")
	for i in static/js/public/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif
