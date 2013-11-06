require.config({
	urlArgs: "v=3.4-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access',
		'thumbnails' : 'cdr-access',
		'text' : 'lib/text',
		'tpl' : 'lib/tpl',
		'underscore' : 'lib/underscore',
		'preload' : 'cdr-access',
		'StructureEntry' : 'cdr-access',
		'StructureView' : 'cdr-access',
		'JP2Viewer' : 'cdr-access',
		'VideoPlayer' : 'cdr-access',
		'AudioPlayer' : 'cdr-access',
		'openLayers' : '/static/plugins/OpenLayers/OpenLayers',
		'flowPlayer' : '/static/plugins/flowplayer/flowplayer.min',
		'audiojs' : '/static/plugins/audiojs/audio'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'thumbnails' : ['jquery', 'preload'],
		'audiojs' : {
			exports : 'audiojs'
		},
		'underscore': {
			exports: '_'
		}
	}
});
define('fullRecord', ['module', 'jquery', 'JP2Viewer', 'StructureView', 'VideoPlayer', 'AudioPlayer', 'thumbnails'], function(module, $) {
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
				try {
					$viewer[widgetName].call($viewer, "show");
					$(this).html("Hide");
					window.location.hash = '#' + hashValue;
				} catch (e) {
					console.log(e);
				}
				
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
		$.ajax({
			url: "/structure/" + $structureView.attr('data-pid') + "/json?files=true",
			dataType : 'json',
			success: function(data){
				$structureView.structureView({
					hideRoot : true,
					showResourceIcons : true,
					showParentLink : false,
					rootNode : data.root,
					queryPath : 'list',
					secondaryActions : true,
					seeAllLinks : false,
					excludeIds : $structureView.attr('data-exclude')
				});
			},
			error: function(e){
				console.log("Failed to load", e);
			}
		});
	}
});
