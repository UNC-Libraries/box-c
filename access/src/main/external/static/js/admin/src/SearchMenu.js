define('SearchMenu', [ 'jquery', 'jquery-ui', 'URLUtilities', 'StructureView'], function(
		$, ui, URLUtilities) {
	$.widget("cdr.searchMenu", {
		options : {
			filterParams : '',
			selectedId : false,
			queryPath : 'list'
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
										queryPath : self.options.queryPath,
										filterParams : self.options.filterParams,
										selectedId : self.options.selectedId,
										onChangeEvent : $.proxy(self._adjustHeight, self)
									});
									$structureView.addClass('inset facet');
									// Inform the result view that the structure browse is ready for move purposes
									if (self.options.resultTableView)
										self.options.resultTableView.resultTableView('addMoveDropLocation', 
											$structureView.find(".structure_content"),
											'.entry > .primary_action', 
											function($dropTarget){
												var dropObject = $dropTarget.closest(".entry_wrap").data("structureEntry");
												// Needs to be a valid container with sufficient perms
												if (!dropObject || dropObject.options.isSelected || $.inArray("addRemoveContents", dropObject.metadata.permissions) == -1)
													return false;
												return dropObject.metadata;
											});
									data = $structureView;
								}
								ui.newPanel.html(data);
								ui.newPanel.data('contentLoaded', true);
								self._adjustHeight();
							},
							error : function() {
								ui.newPanel.html("");
							}
						});
					}
				},
				activate : $.proxy(self._adjustHeight, self)
			}).accordion('option', 'active', 0);
			
			this.element.resizable({
				handles: 'e',
				alsoResize : ".structure.inset.facet",
				minWidth: 300,
				maxWidth: 600
			}).css('visibility', 'visible');
			
			$(window).resize($.proxy(self._adjustHeight, self));
		},
		
		_adjustHeight : function () {
			var activeMenu = this.element.find(".filter_menu .ui-accordion-content-active");
			if (activeMenu.length == 0) {
				return;
			}
			var top = activeMenu.offset().top;
			var innerHeight = activeMenu.innerHeight();
			var height = activeMenu.height();
			var verticalPadding = innerHeight - height;
			var windowHeight = $(window).height();
			var siblingHeight = 0;
			activeMenu.parent().nextAll().each(function(){
				siblingHeight += $(this).outerHeight() + 4;
			});
			if ((top + innerHeight + siblingHeight) > windowHeight) {
				activeMenu.height(windowHeight - top - siblingHeight - verticalPadding);
			} else {
				activeMenu.height('auto');
			}
		}
	});
});