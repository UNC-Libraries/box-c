define("AudioPlayer", [ 'jquery', 'jquery-ui'], function($, ui) {
	$.widget("cdr.audioPlayer", {
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
				this.initialized = true;
				require(['audiojs'], function(audiojs) {
					self.player = audiojs.create(self.element[0]);
					$(self.player.wrapper).fadeIn(100);
				});
			} else {
				$(this.player.wrapper).fadeIn(100);
			}
		},
		
		hide : function() {
			$(this.player.wrapper).fadeOut(100);
		},
		
		isVisible : function() {
			return this.initialized && $(this.player.wrapper).is(":visible");
		}
	});
});