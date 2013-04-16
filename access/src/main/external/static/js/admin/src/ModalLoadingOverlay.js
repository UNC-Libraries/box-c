define([ 'jquery', 'jquery-ui', 'editable', 'moment', 'qtip'], function($) {
	$.widget("cdr.modalLoadingOverlay", {
		options : {
			'text' : null,
			'iconSize' : 'large',
			'iconPath' : '/static/images/admin/loading-small.gif',
			'autoOpen' : true
		},
		
		_create : function() {
			if (this.options.text == null)
				this.overlay = $('<div class="load_modal icon_' + (this.options.iconSize) + '"></div>');
			else {
				this.overlay = $('<div class="load_modal"></div>');
				this.textSpan = $('<span>' + this.options.text + '</span>');
				this.overlay.append(this.textSpan);
				if (this.options.iconPath)
					this.textIcon = $('<img src="' + this.options.iconPath + '" />').appendTo(this.overlay);
			}
			
			this.overlay.appendTo(document.body);
			
			$(window).resize($.proxy(this.resize, this));
			
			if (this.options.autoOpen)
				this.show();
			else this.hide();
		},
		
		close : function() {
			this.overlay.remove();
		},
		
		show : function() {
			this.overlay.css({'visibility': 'hidden', 'display' : 'block'});
			if (this.element != $(document)) {
				this.resize();
				if (this.textSpan) {
					var topOffset = (this.element.innerHeight() - this.textSpan.outerHeight()) / 2;
					this.textSpan.css('top', topOffset);
					this.textIcon.css('top', topOffset);
				}
			}
			this.overlay.css('visibility', 'visible');
		},
		
		resize : function() {
			this.overlay.css({'width' : this.element.innerWidth(), 'height' : this.element.innerHeight(),
				'top' : this.element.offset().top, 'left' : this.element.offset().left});
		},
		
		hide : function() {
			this.overlay.hide();
		},
		
		setText : function(text) {
			this.textSpan.html(text);
		}
	});
});