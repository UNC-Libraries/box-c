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
		'uv': '/static/plugins/uv/uv',
		'audiojs' : '/static/plugins/audiojs/audio',
		'promise': 'lib/promise-polyfill.min',
		'fetch' : 'lib/fetch-polyfill.min'
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
	var $jp2Window = $(".jp2_imageviewer_window"),
		$modsDisplay = $("#mods_data_display");

	function loadViewer($viewer, widgetName) {
		$viewer[widgetName].call($viewer, {
			show : true,
			url : $viewer.attr("data-url")
		});
	}

	if ($jp2Window.length > 0) {
		loadViewer($jp2Window, 'jp2Viewer', $(".jp2_viewer_link"));
	}

	if ($modsDisplay.length > 0) {
		$.ajax({
			url: "/record/" + $modsDisplay.attr("data-pid") + "/metadataView",
			dataType: "html",
			success: function(data) {
				if (/^no.metadata/i.test($.trim(data))) {
					data = '<p class="no-mods">' + data + '</p>';
				}
				$modsDisplay.html(data);
			},
			error: function(e) {
				var msg = "Unable to retrieve MODS for this record";
				$modsDisplay.html("<p>" + msg + "</p>");
				console.log(msg, e);
			}
		});
	}
});
