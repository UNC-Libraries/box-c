require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'cdrCommon' : 'cdrCommon',
		'thumbnail' : 'thumbnail',
		'PID' : 'admin/src/PID',
		'StructureEntry' : 'src/StructureEntry',
		'StructureView' : 'src/StructureView',
		'JP2Viewer' : 'src/JP2Viewer',
		'VideoPlayer' : 'src/VideoPlayer',
		'AudioPlayer' : 'src/AudioPlayer',
		'openLayers' : '/static/plugins/OpenLayers/OpenLayers',
		'flowPlayer' : '/static/plugins/flowplayer/flowplayer-3.2.6.min'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'qtip' : ['jquery'],
		'thumbnail' : ['jquery'],
		'cdrCommon' : ['jquery', 'qtip'],
		'flowPlayer' : {
			exports : '$f'
		}
	}
});
define('fullRecord', ['module', 'jquery', 'cdrCommon', 'JP2Viewer', 'StructureView', 'VideoPlayer', 'AudioPlayer'], function(module, $) {
	var $jp2Window = $(".jp2_imageviewer_window"),
		$audioPlayer = $(".audio_player"),
		$videoPlayer = $(".video_player"),
		$structureView = $(".structure");
	
	function toggleViewer($viewer, widgetName, $toggleLink, hashValue, showLabel) {
		$viewer[widgetName].call($viewer, {
			show : window.location.hash.replace("#", "") == hashValue,
			url : $viewer.attr("data-url")
		});
		$toggleLink.click(function(event){
			if ($viewer.is(":visible")) {
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
	
	if ($jp2Window.length > 0){
		toggleViewer($jp2Window, 'jp2Viewer', $(".jp2_viewer_link"), 'showJP2', 'View');
		/*$jp2Window.jp2Viewer({
			show : window.location.hash.replace("#", "") == "showJP2"
		});
		$(".jp2_viewer_link").click(function(event){
			if ($jp2Window.is(":visible")) {
				$jp2Window.jp2Viewer("hide");
				$(this).html("View");
				window.location.hash = '';
			} else {
				$jp2Window.jp2Viewer("show");
				$(this).html("Hide");
				window.location.hash = '#showJP2';
			}
			return false;
		});*/
	}
	
	if ($videoPlayer.length > 0){
		toggleViewer($videoPlayer, 'videoPlayer', $(".video_player_link"), 'showVideo', 'View');
	}
	
	if ($audioPlayer.length > 0){
		toggleViewer($audioPlayer, 'audioPlayer', $(".audio_player_link"), 'showAudio', 'Listen');
	}
	
	if ($(".inline_viewer_link").length > 0){
		$(".thumb_link").bind("click", function(){
			$(".inline_viewer_link").trigger("click");
			return false;
		});
	}
	
	if ($structureView.length > 0) {
		$structureView.structureView({
			showResourceIcons : true
		});
	}
});