require.config({
	urlArgs: "v=5.0-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-admin',
		'jquery-ui' : 'cdr-admin',
		'text' : 'lib/text',
		'underscore' : 'lib/underscore',
		'tpl' : 'lib/tpl',
		'qtip' : 'lib/jquery.qtip.min',
		
		'StatusMonitorManager' : 'cdr-admin',
		'AbstractStatusMonitor' : 'cdr-admin',
		'ActionEventHandler' : 'cdr-admin',
		'DepositMonitor' : 'cdr-admin',
		'URLUtilities' : 'cdr-admin',
		
		"editable" : "admin/lib/jqueryui-editable.min",
		'moment' : 'cdr-admin'
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
