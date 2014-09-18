require.config({
	urlArgs: "v=3.4-SNAPSHOT",
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
		'DepositMonitor' : 'cdr-admin',
		'IndexingMonitor' : 'cdr-admin',
		'EnhancementMonitor' : 'cdr-admin',
		'URLUtilities' : 'cdr-admin',
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
			username : module.config().username,
			isAdmin : module.config().isAdmin
		};
	var statusMonitorManager = new StatusMonitorManager($("#status_monitor"), options);
	statusMonitorManager.activate();
});
