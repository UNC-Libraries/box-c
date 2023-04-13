require.config({
	urlArgs: "v=5.0-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access',
		'thumbnails' : 'cdr-access',
		'text' : 'lib/text',
		'tpl' : 'lib/tpl',
		'underscore' : 'lib/underscore',
		'StructureEntry' : 'cdr-access',
		'StructureView' : 'cdr-access',
		'JP2Viewer' : 'cdr-access',
		'uvOffline': '/static/plugins/uv/lib/offline',
		'uvHelpers': '/static/plugins/uv/helpers',
		'uv': '/static/plugins/uv/uv'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'underscore': {
			exports: '_'
		}
	},
	waitSeconds: 120
});
define('fullRecord', ['module', 'jquery', 'JP2Viewer', 'StructureView', 'thumbnails'], function(module, $) {
	let $jp2Window = $(".jp2_imageviewer_window");

	function loadViewer($viewer, widgetName) {
		$viewer[widgetName].call($viewer, {
			show : true,
			url : $viewer.attr("data-url")
		});
	}

	if ($jp2Window.length > 0) {
		loadViewer($jp2Window, 'jp2Viewer', $(".jp2_viewer_link"));
	}
});
