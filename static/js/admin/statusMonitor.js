require.config({
	urlArgs: "v=5.0-SNAPSHOT",
	baseUrl: '/static/',
	paths: {
		'jquery' : 'assets/admin/cdr-admin',
		'jquery-ui' : 'assets/admin/cdr-admin',
		'text' : 'js/admin/lib/text',
		'underscore' : 'js/admin/lib/underscore',
		'tpl' : 'js/admin/lib/tpl',
		'qtip' : 'js/admin/lib/jquery.qtip.min',
		
		'StatusMonitorManager' : 'assets/admin/cdr-admin',
		'AbstractStatusMonitor' : 'assets/admin/cdr-admin',
		'ActionEventHandler' : 'assets/admin/cdr-admin',
		'DepositMonitor' : 'assets/admin/cdr-admin',
		'URLUtilities' : 'assets/admin/cdr-admin',
		
		"editable" : "js/admin/lib/jqueryui-editable.min",
		'moment' : 'assets/admin/cdr-admin'
	},
	shim: {
		'qtip' : ['jquery'],
		'underscore': {
			exports: '_'
		}
	}
});

define('statusMonitor', ['module', 'jquery', 'StatusMonitorManager'], function(module, $, StatusMonitorManager) {
	var options = {
			username : module.config().username
		};
	var statusMonitorManager = new StatusMonitorManager($("#status_monitor"), options);
	statusMonitorManager.activate();
});
