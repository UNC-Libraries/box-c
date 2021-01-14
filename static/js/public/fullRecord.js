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
		'VideoPlayer' : 'cdr-access',
		'AudioPlayer' : 'cdr-access',
		'dataTables': '/static/plugins/DataTables/datatables.min',
		'leaflet': '/static/plugins/leaflet/leaflet',
		'leafletFullscreen': '/static/plugins/Leaflet-fullscreen/dist/Leaflet.fullscreen',
		'leaflet-IIIF' : '/static/plugins/Leaflet-IIIF/leaflet-iiif',
		'audiojs' : '/static/plugins/audiojs/audio',
		'promise': 'lib/promise-polyfill.min',
		'fetch' : 'lib/fetch-polyfill.min'
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
define('fullRecord', ['module', 'jquery', 'JP2Viewer', 'StructureView', 'dataTables', 'AudioPlayer', 'thumbnails'], function(module, $) {
	var $jp2Window = $(".jp2_imageviewer_window"),
		$audioPlayer = $(".audio_player"),
		$videoPlayer = $(".video_player"),
		$structureView = $(".structure.aggregate"),
		$childFilesTable = $("#child-files"),
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
	
	if ($audioPlayer.length > 0) {
		loadViewer($audioPlayer, 'audioPlayer');
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

	if ($childFilesTable.length > 0) {
		var excluded_columns = [0, 4, 5];
		var column_defs = [
			{ orderable: false, targets: excluded_columns },
			{ searchable: false, target: excluded_columns },
			{ type: 'file-size', targets: 3 },
			{ width: '5%', targets: [0, 4, 5] },
			{ width: '20%', targets: [2, 3] },
			{ width: '40%', targets: 1 },
			{ render: function (data, type, row) {
				var img;

				if ('thumbnail_url' in row) {
					img = '<img src="' + row.thumbnail_url + '" alt="Thumbnail image for ' + row.title + '">';
				} else {
					img = '<i class="fa fa-file default-img-icon" title="Default thumbnail image"></i>';
				}

				var trashBadge = showBadge(row).markDeleted;
				var lockBadge = showBadge(row).restricted;

				if (trashBadge || lockBadge) {
					var whichBadge = '';

					if (trashBadge) {
						whichBadge = 'trash';
					} else if (lockBadge) {
						whichBadge = 'lock';
					}

					img += '<div class="thumbnail-badge thumbnail-badge-' + whichBadge + '">' +
							'<div class="fa-stack">' +
								'<i class="fas fa-circle fa-stack-2x background"></i>' +
								'<i class="fas fa-' + whichBadge + ' fa-stack-1x foreground"></i>' +
							'</div>' +
						'</div>';
				}

				return img
				}, targets: 0
			},
			{ render: function (data, type, row) { return '<a href="/record/' + row.id + '" + aria-label="View ' + row.title +'">' +row.title + '</a>'; }, targets: 1 },
			{ render: function (data, type, row) { return getOriginalFileValue(row.datastream, 'file_type'); }, targets: 2 },
			{ render: function (data, type, row) { return getOriginalFileValue(row.datastream, 'file_size');  }, targets: 3 },
			{ render: function (data, type, row) { return '<a href="/record/' + row.id + '" aria-label="View ' + row.title +'">' +
					'<i class="fa fa-search-plus is-icon"' + ' title="View"></a>'; },
				targets: 4
			},
			{ render: function (data, type, row) { return '<a href="/indexablecontent/' + row.id + '?dl=true" aria-label="Download ' + row.title +'">' +
					'<i class="fa fa-download is-icon" title="Download"></a>'; },
				targets: 5
			}
		];

		// Check if user can see edit button
		if ($('#child-files th').length === 7) {
			excluded_columns.push(6); // edit button

			// Add to orderable, searchable exclusions
			[0, 1].forEach(function(d) {
				column_defs[d].targets = excluded_columns;
			});

			// Add edit btn to 10% width group
			column_defs[2].targets = [4, 5, 6];

			column_defs.push(
				{ render: function (data, type, row) {
						return '<a href="/admin/describe/' + row.id + '" aria-label="Edit ' + row.title +'">' +
							'<i class="fa fa-edit is-icon" title="Edit"></i></a>'
					},
					targets: 6
				});
		}

		$childFilesTable.DataTable({
			ajax: {
				processing: true,
				url: '/listJson/' + $childFilesTable.attr('data-pid'),
				dataSrc: function(d) {
					return d.metadata;
				}
			},
			bLengthChange: false, // Remove option to show different number of results
			columnDefs: column_defs,
			language: { search: '', searchPlaceholder: 'Search for keywords' },
			order: [[1, 'asc']],
			rowCallback: function(row, data) {
				if (showBadge(data).markDeleted) {
					$(row).addClass('deleted');
				}
			}
		});

		$('#child-files_filter input').addClass('input');
		$('.child-records h3').css('margin-bottom', '-30px'); // adjust margin to line up with search box

		function getOriginalFileValue(datastream_info, type) {
			for (var i in datastream_info) {
				var ds_parts = datastream_info[i].split("\|");
				if (ds_parts.length < 5 || ds_parts[0] !== "original_file") {
					continue;
				}
				if (type === 'file_type') {
					return ds_parts[3];
				} else {
					return bytesToSize(ds_parts[4])
				}
			}
			return "";
		}

		function showBadge(data) {
			var markedForDeletion = false;
			var restrictedAccess = true;

			if (data.status !== undefined) {
				var restrictions = data.status.join(',').toLowerCase();
				markedForDeletion = /marked.*?deletion/.test(restrictions);
				restrictedAccess = data.status.indexOf("Public Access") === -1;
			}

			return { markDeleted: markedForDeletion, restricted: restrictedAccess };
		}

		function bytesToSize(bytes) {
			var fileBytes = parseInt(bytes);
			if (isNaN(fileBytes) || fileBytes === 0) {
				return '0 B';
			}

			var k = 1024;
			var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
			var i = Math.floor(Math.log(fileBytes) / Math.log(k));
			var val = (fileBytes / Math.pow(k, i)).toFixed(1);
			var floored_val = Math.floor(val);

			if (val - floored_val === 0) {
				return floored_val + ' ' + sizes[i];
			}

			return val + ' ' + sizes[i];
		}
	}
});
