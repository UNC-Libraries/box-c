require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui',
		'text' : 'text',
		'underscore' : 'underscore',
		'tpl' : 'tpl',
		'qtip' : 'jquery.qtip.min',
		
		'StatusMonitorManager' : 'squish',
		'AbstractStatusMonitor' : 'squish',
		'IngestMonitor' : 'squish',
		'URLUtilities' : 'squish',
		'moment' : 'moment.min'
	},
	shim: {
		'jquery-ui' : ['jquery'],
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