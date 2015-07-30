define('ModalLoadingOverlay', [ 'jquery', 'jquery-ui', 'editable', 'moment', 'qtip'], function($) {
	var defaultOptions = {
		text : null,
		type : "icon", // text, icon, determinate
		iconSize : 'large',
		autoOpen : true,
		dialog : false
	};
	
	function ModalLoadingOverlay(element, options) {
		this.options = $.extend({}, defaultOptions, options);
		this.element = element;
		this.init();
	}
	
	ModalLoadingOverlay.prototype.init = function() {
		this.overlay = $('<div class="load_modal"></div>');
		if (this.options.type == "determinate") {
			this.loadingBar = $("<div></div>").progressbar({
				value : 0
			});
			this.overlay.append(this.loadingBar);
		} else if (this.options.type == "icon")
			this.overlay.addClass('icon_' + this.options.iconSize);
		else if (this.options.type == "text")
			this.textIcon = $('<div class="text_icon icon_' + this.options.iconSize + '"></div>').appendTo(this.overlay);
		
		if (this.options.text)
			this.setText(this.options.text);
		
		this.overlay.appendTo(document.body);
		
		$(window).resize($.proxy(this.resize, this));
		var self = this;
		if (this.options.dialog) {
			this.options.dialog.on("dialogdrag", function(event, ui){
				self.resize();
			});
		}
		
		if (this.options.autoOpen)
			this.open();
		else this.close();
	};
	
	ModalLoadingOverlay.prototype.close = function() {
		this.overlay.hide();
	};
	
	ModalLoadingOverlay.prototype.remove = function() {
		this.overlay.remove();
	};
	
	ModalLoadingOverlay.prototype.open = function() {
		this.overlay.css({'visibility': 'hidden', 'display' : 'block'});
		if (this.element != $(document))
			this.resize();
		this.overlay.css('visibility', 'visible');
	};
	
	ModalLoadingOverlay.prototype.resize = function() {
		this.overlay.css({'width' : this.element.innerWidth(), 'height' : this.element.innerHeight(),
			'top' : this.element.offset().top, 'left' : this.element.offset().left});
		this.adjustContentPosition();
	};
	
	ModalLoadingOverlay.prototype.adjustContentPosition = function() {
		if (this.options.type == "determinate") {
			var topOffset = (this.element.innerHeight() - this.loadingBar.outerHeight()) / 2,
				leftOffset = (this.element.innerWidth() - this.loadingBar.width()) / 2;
			this.loadingBar.css({top : topOffset, left : leftOffset});
		} else {
			if (this.textSpan) {
				var topOffset = (this.element.innerHeight() - this.textSpan.outerHeight()) / 2;
				this.textSpan.css('top', topOffset);
				if (this.textIcon)
					this.textIcon.css('top', topOffset);
			}
		}
	};
	
	ModalLoadingOverlay.prototype.setText = function(text) {
		if (!this.textSpan) {
			this.textSpan = $('<span>' + text + '</span>');
			if (this.options.type == "text")
				this.overlay.prepend(this.textSpan);
			else if (this.options.type == "determinate")
				this.loadingBar.append(this.textSpan);
			else
				this.overlay.append(this.textSpan);
		} else {
			this.textSpan.html(text);
		}
		this.adjustContentPosition();
	};
	
	// Updates the progress bar to a percentage of completion.  Only applies to determinate overlays
	ModalLoadingOverlay.prototype.setProgress = function(value) {
		this.loadingBar.progressbar("value", value);
	};
	
	return ModalLoadingOverlay;
});