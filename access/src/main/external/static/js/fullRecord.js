// returns true if obj has no properties
// handy for testing the json response
function is_empty(obj) {
	for(var prop in obj) {
		if(obj.hasOwnProperty(prop)) {
			return false;
		}
	}
	return true;
}

function djatokalayersViewer(img_url, viewer_id, context) {
	// clear the map div
	var map_div = document.getElementById(viewer_id);
	map_div.innerHTML = '';

	// set some variables
	
	OpenLayers.Layer.OpenURL.djatokaURL = context + '/jp2Region';
	var viewer = $('#' + viewer_id);
	OpenLayers.Layer.OpenURL.viewerWidth = viewer.width(); // Get width from map_div
	OpenLayers.Layer.OpenURL.viewerHeight = viewer.height(); // Get height from map_div
	var metadataUrl = "jp2Metadata?id=" + img_url + "&ds=IMAGE_JP2000";
	
	// define the layer (content, filetypes, etc)
	var OUlayer = new OpenLayers.Layer.OpenURL("OpenURL", "", {
			layername: 'basic',
			format:'image/jpeg',
			rft_id: "ignore&id=" + img_url + "&ds=IMAGE_JP2000",
			height: 200,
			width: 200,
			metadataUrl: metadataUrl
		});
	var metadata = OUlayer.getImageMetadata();
	if (is_empty(metadata)) {
		$('#' + viewer_id).hide();
		$('#viewer_bar').hide();
		return;
	};
	var resolutions = OUlayer.getResolutions();
	var maxExtent = new OpenLayers.Bounds(0, 0, metadata.width, metadata.height);
	var tileSize = OUlayer.getTileSize();
	var options = {
		resolutions: resolutions,
		maxExtent: maxExtent,
		tileSize: tileSize,
		controls: [ new OpenLayers.Control.Navigation(),
					new OpenLayers.Control.UNCPanZoomBar(),
					new OpenLayers.Control.ArgParser(),
					new OpenLayers.Control.Attribution()
		]
	};
	
	OUlayer.events.register("loadend", OUlayer, function() {
		$("#" + viewer_id).removeClass("not_loaded");
    });
	
	OpenLayers.Util.onImageLoadError = function(){
		$("#" + viewer_id).removeClass("not_loaded");
		$("#" + viewer_id).height("30px");
		$("#" + viewer_id).html("<div class='error'>Sorry, an error occurred while loading the image.</div>");
	}

	// Create the image_viewer
	var map = new OpenLayers.Map(viewer_id, options);
	map.addLayer(OUlayer);
	var lon = metadata.width / 2;
	var lat = metadata.height / 2;
	map.setCenter(new OpenLayers.LonLat(lon, lat), 0);
	
	var fullScreen = new OpenLayers.Control.Button({title: "Toggle full screen mode",
	    displayClass: "ol_fullscreen", trigger: function(){
	    	if ($("#jp2_imageviewer_window").hasClass("full_screen")){
	    		$("#jp2_imageviewer_window").removeClass("full_screen");
	    		$(document).unbind("keyup");
	    	} else {
	    		$("#jp2_imageviewer_window").addClass("full_screen");
	    		$(document).keyup(function(e) {
	    			if (e.keyCode == 27) {
	    				$("#jp2_imageviewer_window").removeClass("full_screen");
	    				map.updateSize();
	    				$(document).unbind("keyup");
	    			}
	    		});
	    	}
	    	map.updateSize();
	    	window.scrollTo(0, 0);
	    }
	});
	var panel = new OpenLayers.Control.Panel({defaultControl: fullScreen, 
		displayClass: "ol_fullscreen_panel"});
	
	panel.addControls([fullScreen]);
	map.addControl(panel);
}

function displayJP2Viewer(event){
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
}

function displayAudioPlayer(event){
	$("#"+event.data.viewerId).toggle();
	if ($("#"+event.data.viewerId).is(":visible")){
		$(this).html("Hide");
		window.location.hash = '#showAudio';
	} else {
		$(this).html("Listen");
		window.location.hash = '';
	}
	if ($(this).hasClass("audio_player_link")){
		$f("audio_player", {src: "/static/plugins/flowplayer/flowplayer-3.2.7.swf", bgcolor: "#d7d7d7"}, {
			clip: {
				"url":escape(event.data.url),
				"autoPlay": true,
				"autoBuffering": false
			},
			canvas: {
				backgroundColor: "#D7D7D7"
			},
			plugins: {
				controls: {
					fullscreen: false,
					height: 30,
					autoHide: false,
					buttonColor: '#666677',
					buttonOverColor: '#555566',
					backgroundColor: '#D7D7D7',
					backgroundGradient: 'medium',
					sliderColor: '#FFFFFF',
					
					sliderBorder: '1px solid #808080',
					volumeSliderColor: '#FFFFFF',
					volumeBorder: '1px solid #808080',
					
					timeColor: '#000000',
					durationColor: '#535353'
				}
			}
		});
		$(this).removeClass("audio_player_link");
		return false;
	}
}

function displayVideoViewer(event){
	$("#"+event.data.viewerId).toggle();
	if ($("#"+event.data.viewerId).is(":visible")){
		$(this).html("Hide");
		window.location.hash = '#showVideo';
	} else {
		$(this).html("View");
		window.location.hash = '';
	}
	if ($(this).hasClass("video_viewer_link")){
		$f("video_player", "/static/plugins/flowplayer/flowplayer-3.2.7.swf", {
			clip: {
				"url":escape(event.data.url),
				"autoPlay": true,
				"autoBuffering": false
			}
		});
		$(this).removeClass("video_viewer_link");
	}
	return false;
}

$(function() {
	if ($(".inline_viewer_link").length > 0){
		$(".thumb_link").bind("click", function(){
			$(".inline_viewer_link").trigger("click");
			return false;
		});
	}
});