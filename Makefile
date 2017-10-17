VERSION = ""

build-all: build-admin build-access

build-admin:
	cat static/js/lib/jquery.min.js > static/js/cdr-admin.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> static/js/cdr-admin.js
	cat static/js/lib/jquery-ui.min.js >> static/js/cdr-admin.js
	echo "});" >> static/js/cdr-admin.js
	cat static/js/admin/lib/jquery.detachplus.js \
		static/js/lib/moment.min.js \
		static/js/public/src/Structure* \
		static/js/admin/src/*.js \
		static/js/admin/src/*/*.js \
		>> static/js/cdr-admin.js
	
	cat static/css/reset.css \
		static/css/cdr_common.css \
		static/css/admin/jquery-ui.css \
		static/css/admin/jquery.qtip.css \
		static/css/admin/jquery.contextMenu.css \
		static/css/font-awesome.min.css \
		static/css/admin/cdradmin.css \
		static/css/admin/search_results.css \
		static/css/admin/admin_forms.css \
		static/css/admin/collector.css \
		static/css/structure_browse.css \
		> static/css/cdr_admin.css
	
ifneq ($(VERSION), "")
	for i in static/js/admin/*.js; do \
		sed "s/\(urlArgs *: *\)\".*\"/\1\"v=$(VERSION)\"/" $$i > $$i.temp; \
		mv $$i.temp $$i; \
	done
endif

build-access:
	cat static/js/lib/jquery.min.js > static/js/cdr-access.js
	echo "define('jquery-ui', ['jquery'], function ($$) {" >> static/js/cdr-access.js
	cat static/js/lib/jquery-ui-access.min.js >> static/js/cdr-access.js
	echo "});" >> static/js/cdr-access.js
	cat \
		static/js/public/src/*.js \
		>> static/js/cdr-access.js
		
	cat static/css/reset.css \
		static/css/cdr_common.css \
		static/css/cdrui_styles.css \
		static/css/fluid_cap.css \
		static/css/font-awesome.min.css \
		static/css/structure_browse.css \
		static/front/front.css \
		static/front/peek.css \
		> static/css/cdr_access.css

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
