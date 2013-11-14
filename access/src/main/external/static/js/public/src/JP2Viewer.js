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
				require(['openLayers'], function(){
					self.element.show();
					self._initDjatokaLayers();
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
		
		_initDjatokaLayers : function() {
			var metadataUrl = 'jp2Metadata/' + this.options.url + '/IMAGE_JP2000',
				self = this;
			
			this.initialized = true;
			this.element.addClass('not_loaded');
			
			OpenLayers.Layer.OpenURL.djatokaURL = this.options.context + '/jp2Region/' + this.options.url + '/IMAGE_JP2000';
			OpenLayers.Layer.OpenURL.viewerWidth = this.element.width(); // Use viewer width
			OpenLayers.Layer.OpenURL.viewerHeight = this.element.height(); // Use viewer height
			
			
			// define the layer (content, filetypes, etc)
			var OUlayer = new OpenLayers.Layer.OpenURL("OpenURL", "", {
					layername: 'basic',
					format:'image/jpeg',
					rft_id: "ignore",
					height: 200,
					width: 200,
					metadataUrl: metadataUrl
				});
			var metadata = OUlayer.getImageMetadata();
			// bail out if no metadata could be retrieved
			if ($.isEmptyObject(metadata)) {
				this.element.hide();
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
				self.element.removeClass("not_loaded");
			});
			
			OpenLayers.Util.onImageLoadError = function(){
				self.element.removeClass("not_loaded").height("30px")
					.html("<div class='error'>Sorry, an error occurred while loading the image.</div>");
				$(document.body).removeClass("full_screen");
			};

			// Create the image_viewer
			var map = new OpenLayers.Map(this.element.attr('id'), options);
			map.addLayer(OUlayer);
			var lon = metadata.width / 2;
			var lat = metadata.height / 2;
			map.setCenter(new OpenLayers.LonLat(lon, lat), 0);
			
			var fullScreen = new OpenLayers.Control.Button({title: "Toggle full screen mode",
			    displayClass: "ol_fullscreen", trigger: function(){
			    	if ($(document.body).hasClass("full_screen")){
			    		$(document.body).removeClass("full_screen");
			    		$(document).unbind("keyup");
			    	} else {
			    		$(document.body).addClass("full_screen");
			    		$(document).keyup(function(e) {
			    			if (e.keyCode == 27) {
			    				$(document.body).removeClass("full_screen");
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
	});
});