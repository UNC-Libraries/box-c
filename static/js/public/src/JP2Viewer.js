define("JP2Viewer", [ 'jquery', 'jquery-ui', 'leaflet' ], function($, ui, L) {
	$.widget("cdr.jp2Viewer", {
		options : {
			context : "",
			show : false,
			url : undefined
		},
		
		_create : function() {
			this.initialized = false;
			if (this.options.show) {
				this.show();
			}
		},
		
		show : function() {
			if (!this.initialized) {
				var self = this;
				require(['leaflet-IIIF', 'leafletFullscreen'], function(){
					self.element.show();
					self._initLeaflet();
				});
			} else {
				this.element.show();
			}
		},
		
		hide : function() {
			this.element.hide();
		},
		
		isVisible : function() {
			return this.element.is(":visible");
		},
		
		_initLeaflet : function() {
			var self = this;
			
			this.initialized = true;
			this.element.addClass('not_loaded');
		
			$.ajax({
				dataType: 'json',
				url: 'jp2Metadata/' + this.options.url + '/IMAGE_JP2000'
			}).done(function(data) {
				if (data !== null) {
					var viewer = L.map('jp2_viewer', {
						attributionControl: false,
						fullscreenControl: true,
						center: [0, 0],
						crs: L.CRS.Simple,
						zoom: 0
					});

					var iiifLayers = {'img': L.tileLayer.iiif(data['@id'] + '/info.json')};
					iiifLayers['img'].addTo(viewer);
				} else {
					self.element.removeClass("not_loaded").height("30px")
					.html("<div class='error'>Sorry, an error occurred while loading the image.</div>");
				}
							 
				self.element.removeClass("not_loaded");
			}).fail(function(jqXHR, textStatus) {
				self.element.removeClass("not_loaded").height("30px")
					.html("<div class='error'>Sorry, an error occurred while loading the image.</div>");
				$(document.body).removeClass("full_screen");
			});
		}
	});
});