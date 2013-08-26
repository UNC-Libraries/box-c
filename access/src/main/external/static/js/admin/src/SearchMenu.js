define('SearchMenu', [ 'jquery', 'jquery-ui', 'URLUtilities', 'StructureView'], function(
		$, ui, URLUtilities) {
	$.widget("cdr.searchMenu", {
		options : {
			filterParams : ''
		},
		
		_create : function() {
			var self = this;
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
				beforeActivate: function(event, ui) {
					if (ui.newPanel.attr('data-href') != null && !ui.newPanel.data('contentLoaded')) {
						var isStructureBrowse = (ui.newPanel.attr('id') == "structure_facet");
						$.ajax({
							url : URLUtilities.uriEncodeParameters(ui.newPanel.attr('data-href')),
							dataType : isStructureBrowse? 'json' : null,
							success : function(data) {
								if (isStructureBrowse) {
									var $structureView = $('<div/>').html(data);
									$structureView.structureView({
										rootNode : data.root,
										showResourceIcons : true,
										showParentLink : true,
										queryPath : 'list',
										filterParams : self.options.filterParams
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
			}).accordion('option', 'active', 0);
			
			this.element.resizable({
				handles: 'e',
				alsoResize : ".structure.inset.facet",
				minWidth: 300,
				maxWidth: 600
			}).css('visibility', 'visible');
		}
	});
});