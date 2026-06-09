require.config({
	urlArgs: "v=5.0-SNAPSHOT",
	baseUrl: "/static/",

	paths: {
		"jquery-legacy": "js/admin/lib/jquery-legacy-global",
		"jquery-ui-legacy" : "js/admin/lib/jquery-ui.min",
		"text": "js/admin/lib/text",
		"underscore": "js/admin/lib/underscore",
		"tpl": "js/admin/lib/tpl",
		"qtip": "js/admin/lib/jquery.qtip.min",
		"dompurify": "js/admin/lib/dompurify.min",

		"StatusMonitorManager": "assets/admin/cdr-admin",
		"AbstractStatusMonitor": "assets/admin/cdr-admin",
		"ActionEventHandler": "assets/admin/cdr-admin",
		"DepositMonitor": "assets/admin/cdr-admin",
		"URLUtilities": "assets/admin/cdr-admin",

		"editable": "js/admin/lib/jqueryui-editable.min",
		"moment": "assets/admin/cdr-admin"
	},

	map: {
		"*": {
			"jquery": "jquery-legacy",
			"jquery-ui": "jquery-ui-legacy"
		}
	},

	shim: {
		"qtip": ["jquery"],
		"underscore": {
			exports: "_"
		}
	}
});

define("statusMonitor", ["module", "jquery", "dompurify", "StatusMonitorManager"], function (module, $, DomPurify, StatusMonitorManager) {
	if (!$.widget) {
		throw new Error(
			"Legacy jQuery UI is not attached to the RequireJS jQuery instance. " +
			"jQuery version: " + ($.fn && $.fn.jquery ? $.fn.jquery : "unknown")
		);
	}

	var options = {
		username: module.config().username,
		DomPurify: DomPurify
	};

	var statusMonitorManager = new StatusMonitorManager($("#status_monitor"), options);
	statusMonitorManager.activate();
});