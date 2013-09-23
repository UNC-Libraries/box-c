require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-admin',
		 : 
		'text' : 'text',
		'underscore' : 'underscore',
		'tpl' : 'tpl',
		'qtip' : 'jquery.qtip.min',
		
		'StatusMonitorManager' : 'cdr-admin',
		'AbstractStatusMonitor' : 'cdr-admin',
		'IngestMonitor' : 'cdr-admin',
		'IndexingMonitor' : 'cdr-admin',
		'EnhancementMonitor' : 'cdr-admin',
		'URLUtilities' : 'cdr-admin',
		'moment' : 'cdr-admin'
	},
	shim: {
		 : ['jquery'],
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