define([ 'jquery', 'jquery-ui', 'StructureView'], function(
		$, ui) {
	$.widget("cdr.searchMenu", {
		_create : function() {
			this.element.children('.query_menu').accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true
			});
			this.element.children('.filter_menu').accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true,
				active: false,
				activate: function(event, ui) {
					if (ui.newPanel.attr('data-href') != null && !ui.newPanel.data('contentLoaded')) {
						$.ajax({
							url : ui.newPanel.attr('data-href'),
							success : function(data) {
								if (ui.newPanel.attr('id') == "structure_facet") {
									var $structureView = $(data);
									$structureView.structureView({
										showResourceIcons : true,
										showParentLink : true
									});
									$structureView.addClass('inset facet');
									data = $structureView;
								}
								ui.newPanel.html(data);
								ui.newPanel.data('contentLoaded', true);
							}
						});
						
					}
				}
			}).accordion('activate', 0);
			
			this.element.resizable({
				handles: 'e',
				alsoResize : ".structure.inset.facet",
				minWidth: 300,
				maxWidth: 600
			}).css('visibility', 'visible');
		}
	});
});