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
		'AudioPlayer' : 'cdr-access',
		'dataTables': '/static/plugins/DataTables/datatables.min',
		'uvOffline': '/static/plugins/uv/lib/offline',
		'uvHelpers': '/static/plugins/uv/helpers',
		'uv': '/static/plugins/uv/uv',
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
	},
	waitSeconds: 120
});
define('fullRecord', ['module', 'jquery', 'JP2Viewer', 'StructureView', 'dataTables', 'AudioPlayer', 'thumbnails'], function(module, $) {
	var $jp2Window = $(".jp2_imageviewer_window"),
		$audioPlayer = $(".audio_player"),
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
			{ render: function (data, type, row, meta) {
				var img;
				var hasIntersectionObserver = 'IntersectionObserver' in window;

				if ('thumbnail_url' in row  && (meta.row < 10 || !hasIntersectionObserver)) {
					img = '<img class="data-thumb" src="' + row.thumbnail_url + '" alt="Thumbnail image for ' + row.title + '">';
				} else if ('thumbnail_url' in row && hasIntersectionObserver) {
					img = '<img class="data-thumb lazy" data-src="' + row.thumbnail_url + '" alt="Thumbnail image for ' + row.title + '">';
				} else {
					img = '<i class="fa fa-file default-img-icon data-thumb" title="Default thumbnail image"></i>';
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
			{ render: function (data, type, row) { return '<a href="/record/' + row.id + '" aria-label="View ' + row.title +'">' +row.title + '</a>'; }, targets: 1 },
			{ render: function (data, type, row) { return getFileType(row); }, targets: 2 },
			{ render: function (data, type, row) { return getOriginalFileValue(row.datastream, 'file_size');  }, targets: 3 },
			{ render: function (data, type, row) { return '<a href="/record/' + row.id + '" aria-label="View ' + row.title +'">' +
					'<i class="fa fa-search-plus is-icon"' + ' title="View"></i></a>'; },
				targets: 4
			},
			{ render: function (data, type, row) {
					if (row.permissions.indexOf('viewOriginal') === -1) {
						return '<i class="fa fa-download is-icon no-download" title="Download Unavailable"></i>';
					}
					return '<a href="/indexablecontent/' + row.id + '?dl=true" aria-label="Download ' + row.title +'">' +
						'<i class="fa fa-download is-icon" title="Download"></i></a>';
				},
				targets: 5
			}
		];

		if ($('#child-files th').length === 7) {
			excluded_columns.push(6); // edit button

			// Add to orderable, searchable exclusions
			[0, 1].forEach(function(d) {
				column_defs[d].targets = excluded_columns;
			});

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
				url: '/listJson/' + $childFilesTable.attr('data-pid') + "?rows=2000",
				dataSrc: function(d) {
					return d.metadata;
				},
				data: function (d) {
					console.log(d);
				}
			},
			processing: true,
			serverSide: true,
			bLengthChange: false, // Remove option to show different number of results
			columnDefs: column_defs,
			language: { search: '', searchPlaceholder: 'Search for keywords' },
			order: [[1, 'asc']],
			rowCallback: function(row, data) {
				if (showBadge(data).markDeleted) {
					$(row).addClass('deleted');
				}
			},
			drawCallback: function() {
				lazyLoad();
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

		function getFileType(record) {
			let fileTypes = record.fileDesc;
			let fileType;
			if (fileTypes && fileTypes.length > 0) {
				fileType = fileTypes[0];
			}
			if (!fileType) {
				fileTypes = record.fileType;
				if (fileTypes && fileTypes.length > 0) {
					fileType = fileTypes[0];
				}
			}
			return fileType || '';
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

		function lazyLoad() {
			var lazyloadImages = document.querySelectorAll('.lazy');
			var imageObserver = new IntersectionObserver(function (entries, observer) {
				entries.forEach(function (entry) {
					if (entry.isIntersecting) {
						var image = entry.target;
						image.src = image.dataset.src;
						image.classList.remove('lazy');
						imageObserver.unobserve(image);
					}
				});
			});

			lazyloadImages.forEach(function (image) {
				imageObserver.observe(image);
			});
		}
	}
});
