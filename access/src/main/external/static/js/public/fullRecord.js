require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'cdrCommon' : 'cdrCommon',
		'thumbnail' : 'thumbnail',
		'StructureEntry' : 'src/StructureEntry',
		'StructureView' : 'src/StructureView',
		'openLayers' : '/static/plugins/OpenLayers/OpenLayers'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'qtip' : ['jquery'],
		'thumbnail' : ['jquery'],
		'cdrCommon' : ['jquery', 'qtip']
	}
});
define('fullRecord', ['module', 'jquery', 'jp2Viewer'], function(module, $) {
	
	$(".jp2 link").click(function(event){
		$("#"+event.data.viewerId).toggle();
		if ($("#"+event.data.viewerId).is(":visible")){
			$(this).html("Hide");
			window.location.hash = '#showJP2';
		} else {
			$(this).html("View");
			window.location.hash = '';
		}
		if ($(this).hasClass("jp2_viewer_link")){
			djatokalayersViewer(event.data.id, event.data.viewerId, event.data.viewerContext);
			$(this).removeClass("jp2_viewer_link");
		}
		return false;
	});
});