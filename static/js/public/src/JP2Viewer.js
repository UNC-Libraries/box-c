define("JP2Viewer", [ 'jquery', 'jquery-ui'], function($, ui) {
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
				require(['openSeadragon'], function(){
					self.element.show();
					self._initOpenSeadragon();
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
		
		_initOpenSeadragon : function() {
			var self = this;
			
			this.initialized = true;
			this.element.addClass('not_loaded');
		
			$.ajax({
				dataType: 'json',
				url: 'jp2Metadata/' + this.options.url + '/IMAGE_JP2000'
			}).done(function(data) {
				if (data !== null) {
					 OpenSeadragon({
						 id: "jp2_viewer",
					     prefixUrl: "/static/plugins/openseadragon/images/",
					     preserveViewport:   true,
					     immediateRender: true,
					     visibilityRatio:    1,
					     minZoomLevel:       1,
					     defaultZoomLevel:   1,
					     sequenceMode: true,
					     tileSources: [data],
					     showNavigator: true,
					     autoHideControls: false,
					     showRotationControl: true,
					     showSequenceControl: false
					});
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