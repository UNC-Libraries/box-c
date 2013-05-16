require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'thumbnail' : 'thumbnail',
		'StructureEntry' : 'src/StructureEntry',
		'StructureView' : 'src/StructureView',
		'JP2Viewer' : 'src/JP2Viewer',
		'VideoPlayer' : 'src/VideoPlayer',
		'AudioPlayer' : 'src/AudioPlayer',
		'openLayers' : '/static/plugins/OpenLayers/OpenLayers',
		'flowPlayer' : '/static/plugins/flowplayer/flowplayer.min',
		'audiojs' : '/static/plugins/audiojs/audio'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'thumbnail' : ['jquery'],
		'audiojs' : {
			exports : 'audiojs'
		}
	}
});
define('fullRecord', ['module', 'jquery', 'JP2Viewer', 'StructureView', 'VideoPlayer', 'AudioPlayer'], function(module, $) {
	var $jp2Window = $(".jp2_imageviewer_window"),
		$audioPlayer = $(".audio_player"),
		$videoPlayer = $(".video_player"),
		$structureView = $(".structure.aggregate");
	
	function toggleViewer($viewer, widgetName, $toggleLink, hashValue, showLabel) {
		var showOnLoad = window.location.hash.replace("#", "") == hashValue;
		$viewer[widgetName].call($viewer, {
			show : showOnLoad,
			url : $viewer.attr("data-url")
		});
		if (showOnLoad)
			$toggleLink.html("Hide");
		$toggleLink.click(function(event){
			if ($viewer[widgetName].call($viewer, 'isVisible')) {
				$viewer[widgetName].call($viewer, "hide");
				$(this).html(showLabel);
				window.location.hash = '';
			} else {
				$viewer[widgetName].call($viewer, "show");
				$(this).html("Hide");
				window.location.hash = '#' + hashValue;
			}
			return false;
		});
	}
	
	if ($jp2Window.length > 0)
		toggleViewer($jp2Window, 'jp2Viewer', $(".jp2_viewer_link"), 'showJP2', 'View');
	
	if ($videoPlayer.length > 0)
		toggleViewer($videoPlayer, 'videoPlayer', $(".video_player_link"), 'showVideo', 'View');
	
	if ($audioPlayer.length > 0)
		toggleViewer($audioPlayer, 'audioPlayer', $(".audio_player_link"), 'showAudio', 'Listen');
	
	if ($(".inline_viewer_link").length > 0){
		$(".thumb_link").bind("click", function(){
			$(".inline_viewer_link").trigger("click");
			return false;
		});
	}
	
	if ($structureView.length > 0) {
		$structureView.structureView({
			showResourceIcons : true,
			indentSuppressed : true
		});
	}
});