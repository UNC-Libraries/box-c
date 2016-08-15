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
						if (ui.newPanel.data('href') != null && !ui.newPanel.data('contentLoaded')) {
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
				
				self.element.on("click", ".refresh_facet", function(e) {
					var url = $(this).attr("href");
					var pathParts = url.match(/(search|list)\/(.+)\?/);
					var uuid = pathParts != null? pathParts[2] : null;
					
					if (history.pushState) {
						history.pushState({}, "", url);
					}
					self.updateFacets(uuid);
					
					e.preventDefault();
					e.stopPropagation();
				});
			
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
			var innerHeight = activeMenu.children().innerHeight();
			var height = activeMenu.children().height();
			var verticalPadding = innerHeight - height;
			var windowHeight = $(window).height();
			var siblingHeight = 0;
			activeMenu.parent().nextAll().each(function(){
				siblingHeight += $(this).outerHeight();
			});
			activeMenu.height(windowHeight - top - verticalPadding);
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
			
			this.updateFacets(uuid);
			
		},
		
		updateFacets : function(uuid) {
			var limitsPanel = $(".limits_panel", this.element);
			var limitsRegex = /(.*facets)\/?([^\?]+)?(\?.+)?/;
			var limitsPath = limitsPanel.data("href");
			var pathParts = limitsPath.match(limitsRegex);
			var newLimitPath;
			if (uuid) {
				newLimitPath = pathParts[1] + "/" + uuid + pathParts[3];
			} else {
				newLimitPath = limitsPath;
			}
			
			limitsPanel.data("href", newLimitPath).removeData("contentLoaded");
			
			if (limitsPanel.hasClass("ui-accordion-content-active")) {
				this.updatePanel(limitsPanel, false);
			}
		},

		updatePanel : function(panel, isStructureBrowse) {
			var self = this;
			var params = "";
			var filters;

			if (/facet/.test(panel.data('href'))) {
				filters = location.search;
				if (filters) {
					params = "&" + filters.substring(1);
				}
			}

			$.ajax({
				url : URLUtilities.uriEncodeParameters(panel.data('href') + params),
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
							self.options.resultTableView.addMoveDropLocation(
								$structureView.find(".structure_content"),
								'.entry > .primary_action', 
								function($dropTarget){
									var dropObject = $dropTarget.closest(".entry_wrap").data("structureEntry");
									// Needs to be a valid container with sufficient perms
									if (!dropObject || dropObject.options.isSelected || $.inArray("addRemoveContents", dropObject.metadata.permissions) == -1)
										return false;
									return dropObject.metadata;
								}
							);
							data = $structureView;
						}
						
						self.$structureView = $structureView;
						panel.html(data);
					} else {
						if ($(".facets", data).length == 0) {
							data = "No additional filters";
						}
						panel.html("<div>" + data + "</div>");
					}
					
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