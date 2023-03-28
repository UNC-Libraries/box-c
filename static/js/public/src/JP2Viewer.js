define("JP2Viewer", [ 'jquery', 'jquery-ui', 'uvOffline', 'uvHelpers', 'uv'],
	function($, ui, uvOffline, uvHelpers, uv) {
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
				require(['promise', 'fetch', 'uvOffline', 'uvHelpers', 'uv'], function(){
					self.element.show();
					self._initUv();
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
		
		_initUv : function() {
			var self = this;

			this.initialized = true;
			this.element.addClass('not_loaded');
			var $ = window.jsviews || window.jQuery; // jsRender, used by UV, needs to own the $ variable
			var urlDataProvider = new UV.URLDataProvider();
			var uvRoot = '/static/plugins/uv';

			try {
				createUV('#jp2_viewer', {
					root: uvRoot,
					iiifResourceUri: '/jp2Proxy/' + this.options.url + '/jp2/manifest',
					configUri: uvRoot + '/unc-uv-config.json',
					collectionIndex: Number(urlDataProvider.get('c', 0)),
					manifestIndex: Number(urlDataProvider.get('m', 0)),
					sequenceIndex: Number(urlDataProvider.get('s', 0)),
					canvasIndex: 0,
					rangeId: urlDataProvider.get('rid', 0),
					rotation: Number(urlDataProvider.get('r', 0)),
					xywh: urlDataProvider.get('xywh', ''),
					embedded: true,
					locales: [{ name: "en-GB" }]
				}, urlDataProvider);

				_.defer(function() {
					self.element.removeClass("not_loaded");
				});
			} catch (e) {
				self.element.removeClass("not_loaded").height("30px")
					.html("<div class='error'>Sorry, an error occurred while loading the image.</div>");
				$(document.body).removeClass("full_screen");
			}
		}
	});
});
