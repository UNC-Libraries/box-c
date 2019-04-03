require.config({
	urlArgs: "v=4.0-SNAPSHOT",
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
		'VideoPlayer' : 'cdr-access',
		'AudioPlayer' : 'cdr-access',
		'dataTables': '/static/plugins/DataTables/datatables.min',
		'leaflet': '/static/plugins/leaflet/leaflet',
		'leafletFullscreen': '/static/plugins/Leaflet-fullscreen/dist/Leaflet.fullscreen',
		'leaflet-IIIF' : '/static/plugins/Leaflet-IIIF/leaflet-iiif',
		'audiojs' : '/static/plugins/audiojs/audio'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'audiojs' : {
			exports : 'audiojs'
		},
		'underscore': {
			exports: '_'
		}
	}
});
define('fullRecord', ['module', 'jquery', 'JP2Viewer', 'StructureView', 'AudioPlayer', 'thumbnails'], function(module, $) {
	var $jp2Window = $(".jp2_imageviewer_window"),
		$audioPlayer = $(".audio_player"),
		$videoPlayer = $(".video_player"),
		$structureView = $(".structure.aggregate"),
		$childFilesTable = $("#child-files");
	
	function loadViewer($viewer, widgetName) {
		$viewer[widgetName].call($viewer, {
			show : true,
			url : $viewer.attr("data-url")
		});
	}
	
	if ($jp2Window.length > 0) {
		loadViewer($jp2Window, 'jp2Viewer', $(".jp2_viewer_link"));
	}
	
	if ($audioPlayer.length > 0) {
		loadViewer($audioPlayer, 'audioPlayer', $(".audio_player_link"));
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

	if ($childFilesTable.length > 0) {
		// Check if user can see edit button
		var searchable_columns = ($('#child-files th').length === 7) ? [4, 5, 6] : [4, 5];

		$childFilesTable.DataTable({
			bLengthChange: false, // Remove option to show different number of results
			columnDefs: [ { orderable: false, searchable: false, targets: searchable_columns } ],
			language: { search: '', searchPlaceholder: 'Search for keywords' }
		});

		$('#child-files_filter input').addClass('input'); // Pull in bulma styling
		$('.child-records h3').css('margin-bottom', '-30px'); // adjust margin to line up with search box
	}
});
