define([ 'jquery', 'jquery-ui'], function($, ui) {
	$.widget("cdr.resizableAccordionMenu", {
		options : {
			subMenu : "div",
			alsoResize : "",
			minWidth: 300,
			maxWidth: 600
		},
		
		_create : function() {
			this.subMenus = $(this.element).children("div").clone();
			
			this.subMenus.accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true,
				active: false,
				activate: function(event, ui) {
					if (ui.newPanel.attr('data-href') != null && !ui.newPanel.data('contentLoaded')) {
						ui.newPanel.load(ui.newPanel.attr('data-href'));
						ui.newPanel.data('contentLoaded', true);
					}
				}
			}).accordion('activate', 0);
			
			$(this.element).html(this.subMenus);
			
			$(this.element).resizable({
				handles: 'e',
				alsoResize : this.options.alsoResize,
				minWidth: this.options.minWidth,
				maxWidth: this.options.maxWidth
			}).css('visibility', 'visible');
		}
	});
});