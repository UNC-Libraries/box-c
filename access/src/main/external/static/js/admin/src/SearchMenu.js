define('SearchMenu', [ 'jquery', 'jquery-ui', 'URLUtilities', 'StructureView'], function(
		$, ui, URLUtilities) {
	$.widget("cdr.searchMenu", {
		options : {
			container : null,
			containerPath : null,
			filterParams : '',
			resultUrl : null, 
			selectedId : false,
			queryPath : 'list',
			template : "tpl!../templates/admin/searchMenu"
		},
		
		_create : function() {
			var self = this;
			
			require([this.options.template], function(template){
				
				self.$contents = $(template({container : self.options.container, filterParams : self.options.filterParams,
					containerPath : self.options.containerPath, resultUrl : self.options.resultUrl}));
				
				self.element.append(self.$contents);
				
				self.element.children('.filter_menu').accordion({
					header: "> div > h3",
					heightStyle: "content",
					collapsible: true,
					active: false,
					beforeActivate: function(event, ui) {
						if (ui.newPanel.attr('data-href') != null && !ui.newPanel.data('contentLoaded')) {
							var isStructureBrowse = (ui.newPanel.attr('id') == "structure_facet");
							self.updatePanel(ui.newPanel, isStructureBrowse);
						}
					},
					activate : $.proxy(self._adjustHeight, self)
				}).accordion('option', 'active', 1);
			
				self.element.resizable({
					handles: 'e',
					alsoResize : ".structure.inset.facet",
					minWidth: 300,
					maxWidth: 600
				}).css('visibility', 'visible');
			
				$(window).resize($.proxy(self._adjustHeight, self));
			});
		},
		
		_adjustHeight : function () {
			var activeMenu = this.element.find(".filter_menu .ui-accordion-content-active");
			if (activeMenu.length == 0) {
				return;
			}
			var self = this;
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
			}
		},
		
		changeFolder : function(uuid) {
			if (this.$structureView) {
				this.$structureView.structureView("changeFolder", uuid);
			}
			if (uuid) {
				$(".search_folder", this.element).removeClass("hidden");
			} else {
				$(".search_folder", this.element).addClass("hidden");
			}
			
			$(".container_id", this.element).val(uuid);
		},
		
		updateFacets : function(url, containerId) {
			var panel = $(".limits_panel", this.element);
			
			var filters = "";
			if (url.indexOf("?") != -1) {
				filters = url.substring(url.indexOf("?") + 1);
			}
			panel.attr('data-href', "facets" + (containerId? "/" + containerId : "") 
					+ "?facetSelect=" + panel.attr('data-facets') + (filters? "&" + filters : ""));
			
			if (panel.hasClass("ui-accordion-content-active")) {
				this.updatePanel(panel, false);
			}
		},

		updatePanel : function(panel, isStructureBrowse) {
			var self = this;
			
			$.ajax({
				url : URLUtilities.uriEncodeParameters(panel.attr('data-href')),
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
						if (self.options.resultTableView) {
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
						
						self.$structureView = $structureView;
					} else {
						if ($(".facets", data).length == 0) {
							data = "No additional filters";
						}
					}
					panel.html(data);
					panel.data('contentLoaded', true);
					self._adjustHeight();
					
					self.scrollToSelectedContainer();
				},
				error : function() {
					panel.html("");
				}
			});
		},
		
		scrollToSelectedContainer : function() {
			var selectedContainer = $(".entry_wrap .selected", self.element);
			if (selectedContainer.length > 0) {
				var parent = this.$structureView.parent();
				parent.animate({scrollTop: selectedContainer[0].offsetTop - parent[0].offsetTop}, 100)
			}
		}
	});
});