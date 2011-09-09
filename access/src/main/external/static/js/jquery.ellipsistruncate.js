; (function($) {
	var $ellipsis = $.ellipsis = function(target, settings){
		settings = $.extend({}, $ellipsis.defaults, settings);
		
		var parentW = 0;
		var parentLeft = 0;
		if (settings.parentSelector != null){
			parentW = $(settings.parentSelector).width();
			var parentPaddingLeft = parseInt($(settings.parentSelector).css("padding-left").replace("px", ""));
			parentLeft = Math.floor($(settings.parentSelector).offset().left + parentPaddingLeft);
		} else {
			if (settings.parentWidth != null)
				parentW = settings.parentWidth;
			if (settings.parentLeft != null)
				parentLeft = settings.parentLeft;
		}
		
		return target.each(function(){
			var el = $(this);
			
			var textObject = "";
			if (settings.textSelector == null){
				textObject = $(this);
			} else {
				textObject = $(this).children(settings.textSelector);
			}
			var originalText = textObject.html();
			var w = el.outerWidth();
			
			var trailingOffset = 0;
			if (settings.trailingSelector != null)
				trailingOffset = textObject.next(settings.trailingSelector).outerWidth();
			
			var leftOffset = Math.floor(el.offset().left - parentLeft + trailingOffset);
			
			if (w + leftOffset - 4 > parentW){
				var t = $(this.cloneNode(true)).hide().css({
                    'position': 'relative',
                    'width': 'auto',
                    'overflow': 'visible',
                    'max-width': 'inherit'
                });
				el.after(t);
				
				var tTextObject = t;
				if (settings.textSelector != null){
					tTextObject = t.children(settings.textSelector);
				}
				
				var text = originalText;
				
				var start = 0;
				var end = text.length;
				var pivot = Math.floor(text.length / 2);
				
				while (end > start + 5){
					pivot = Math.floor((start + end) / 2);
					text = originalText.substr(0, pivot);
					tTextObject.html(text + "&hellip;");
					
					if (t.width() + leftOffset > parentW){
						end = pivot;
					} else {
						start = pivot;
					}
				}
				
				text = originalText.substr(0, start);
				tTextObject.html(text + "&hellip;");
				
				for (pivot = start; pivot < end && pivot < originalText.length - 1 && (t.width() + leftOffset + 2 < parentW); pivot++){
					text = originalText.substr(0, pivot + 1);
					tTextObject.html(text + "&hellip;");
				}
				
				textObject.html(originalText.substr(0, pivot) + "<span class='" + settings.ellipsisClass + "'>&hellip;</span><span class='" + settings.segmentClass + "'>"
						+ originalText.substr(pivot, originalText.length - 1) + "</span>");
				
				t.remove();
			}
		});
	};
	$.fn.ellipsis = function(settings){
		$ellipsis(this, settings);
		return this;
	};
	$ellipsis.defaults = {
		parentSelector: null,
		ellipsisClass: "ellipsis",
		segmentClass: "segment",
		trailingSelector: null,
		textSelect: null,
		parentWidth: 0,
		parentLeft: 0
	};
	
	$.fn.setEllipsisDefaults = function(settings){
		settings = $.extend({}, $ellipsis.defaults, settings);
		$ellipsis.defaults = settings;
	}
})(jQuery);
