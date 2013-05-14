define([ 'jquery', 'jquery-ui'], function($, ui) {
	$.widget("cdr.videoPlayer", {
		options : {
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
				require(['flowPlayer'], function() {
					self._initPlayer();
					self.element.fadeIn('fast');
				});
			} else {
				this.element.fadeIn('fast');
			}
		},
		
		hide : function() {
			this.element.fadeOut('fast');
		},
		
		isVisible : function() {
			return this.element.is(":visible");
		},
		
		_initPlayer : function() {
			this.initialized = true;
			this.element.flowplayer({
				swf : '/static/plugins/flowplayer-5.4/flowplayer.swf'
			});
		}
	});
});