require.config({
	urlArgs: "v=4.3.1",
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
		'IngestMonitor' : 'cdr-admin',
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

define('statusMonitor', ['jquery', 'StatusMonitorManager'], function($, StatusMonitorManager) {
	var statusMonitorManager = new StatusMonitorManager($("#status_monitor"));
	statusMonitorManager.activate();
});
