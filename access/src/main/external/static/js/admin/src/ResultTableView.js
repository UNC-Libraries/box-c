define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'URLUtilities', 
		'ResultObjectActionMenu', 'ResultTableActionMenu', 'ConfirmationDialog', 'MoveDropLocation', 'detachplus'], 
		function($, ui, ResultObjectList, URLUtilities, ResultObjectActionMenu, ResultTableActionMenu, ConfirmationDialog, MoveDropLocation) {
	$.widget("cdr.resultTableView", {
		options : {
			enableSort : true,
			ajaxSort : false,
			metadataObjects : undefined,
			enableArrange : false,
			enableMove : false,
			pagingActive : false,
			container : undefined,
			resultTableTemplate : "tpl!../templates/admin/resultTableView",
			resultEntryTemplate : "tpl!../templates/admin/resultEntry",
			resultFields : undefined,
			resultHeader : undefined,
			postRender : undefined,
			resultActions : undefined,
			headerHeightClass : ''
		},
		
		_create : function() {
			// Instantiate the result table view and add it to the page
			var self = this;
			
			this.actionHandler = this.options.actionHandler;
			this.actionHandler.addToBaseContext('resultTable', this);
			
			require([this.options.resultTableTemplate, this.options.navigationBarTemplate], function(resultTableTemplate, navigationBarTemplate){
				var headerHeightClass = self.options.headerHeightClass;
				if (self.options.container) {
					if (self.options.container.ancestorPath)
						headerHeightClass += " with_path";
					else
						headerHeightClass += " with_container";
				}
				
				self.$resultView = $(resultTableTemplate({resultFields : self.options.resultFields, container : self.options.container,
						resultHeader : self.options.resultHeader, headerHeightClass : headerHeightClass}));
				self.$resultTable = self.$resultView.find('.result_table').eq(0);
				self.$resultHeaderTop = self.$resultView.find('.result_header_top').eq(0);
				self.element.append(self.$resultView);
			
				if (self.options.postRender)
					self.options.postRender(self);
			
				self.resultUrl = self.options.resultUrl;
			
				// Generate result entries
				var fragment = $(document.createDocumentFragment());
				self.resultObjectList = new ResultObjectList({
					'metadataObjects' : self.options.metadataObjects, 
					parent : self.$resultTable.children('tbody'),
					resultEntryTemplate : self.options.resultEntryTemplate
				});
			
				// No results message
				if (self.options.metadataObjects.length == 0) {
					self.$resultTable.after("<div class='no_results'>No matching results</div>");
				}
			
				// Activate sorting
				if (self.options.enableSort)
					self._initSort();
				
				// Initialize batch operation buttons
				self._initBatchOperations();
			
				self._initEventHandlers();
			
				// Activate the result entry context menus, on the action gear and right clicking
				self.contextMenus = [new ResultObjectActionMenu({
					selector : ".action_gear",
					containerSelector : ".res_entry,.container_entry",
					actionHandler : self.actionHandler
				}), new ResultObjectActionMenu({
					trigger : 'right',
					positionAtTrigger : false,
					selector : ".res_entry td",
					containerSelector : ".res_entry,.container_entry",
					actionHandler : self.actionHandler
				})];
			
				// Initialize click and drag operations
				self._initMoveLocations();
				self._initReordering();
			});
		},
		
		// Initialize sorting headers according to whether or not paging is active
		_initSort : function() {
			var $resultTable = this.$resultTable;
			var self = this;
			if (this.options.pagingActive) {
				// Paging active, so need to make server callback to perform sort
				var sortParam = URLUtilities.getParameter('sort');
				var sortOrder = true;
				if (sortParam != null) {
					sortParam = sortParam.split(",");
					if (sortParam.length > 1)
						sortOrder = "reverse" != sortParam[1];
					if (sortParam.length > 0)
						sortParam = sortParam[0];
				}
				
				$("th.sort_col", $resultTable).each(function(){
					var $this = $(this);
					$this.addClass('sorting');
					var sortField = $this.attr('data-field');
					if (sortField) {
						if (sortParam == sortField) {
							if (sortOrder) {
								$this.addClass('asc');
							} else {
								$this.addClass('desc');
							}
						}
						var sortUrl = URLUtilities.setParameter(self.resultUrl, 'sort', sortField + (sortOrder? ",reverse" : ""));
						this.children[0].href = sortUrl;
					}
				});
			} else {
				// Paging off, perform sorting locally
				$("th.sort_col", $resultTable).each(function(){
					var $th = $(this),
					thIndex = $th.index(),
					dataType = $th.attr("data-type");
					$th.addClass('sorting');
					
					$th.click(function(){
						if (!$th.hasClass('sorting')) return;
						//console.time("Sort total");
						var inverse = $th.hasClass('desc');
						$('.sorting', $resultTable).removeClass('asc desc');
						if (inverse)
							$th.addClass('asc');
						else 
							$th.addClass('desc');
						
						// Apply sort function based on data-type
						if (dataType == 'index') {
							self._originalOrderSort(inverse);
						} else if (dataType == 'title') {
							self._titleSort(inverse);
						} else {
							self._alphabeticSort(thIndex, inverse);
						}
						inverse = !inverse;
						//console.timeEnd("Sort total");
					});
				});
			}
		},
		
		// Base row sorting function
		_sortEntries : function($entries, matchMap, getSortable) {
			//console.time("Reordering elements");
			var $resultTable = this.$resultTable;
			
			$resultTable.detach(function(){
				var fragment = document.createDocumentFragment();
				if (matchMap) {
					if ($.isFunction(getSortable)) {
						for (var i = 0, length = matchMap.length; i < length; i++) {
							fragment.appendChild(getSortable.call($entries[matchMap[i].index]));
						}
					} else {
						for (var i = 0, length = matchMap.length; i < length; i++) {
							fragment.appendChild($entries[matchMap[i].index].parentNode);
						}
					}
				} else {
					if ($.isFunction(getSortable)) {
						for (var i = 0, length = $entries.length; i < length; i++) {
							fragment.appendChild(getSortable.call($entries[i]));
						}
					} else {
						for (var i = 0, length = $entries.length; i < length; i++) {
							fragment.appendChild($entries[i].parentNode);
						}
					}
				}
				var resultTable = $resultTable[0];
				resultTable.appendChild(fragment);
			});
			
			//console.timeEnd("Reordering elements");
		},
		
		// Simple alphanumeric result entry sorting
		_alphabeticSort : function(thIndex, inverse) {
			var $resultTable = this.$resultTable;
			var matchMap = [];
			//console.time("Finding elements");
			var $entries = $resultTable.find('tr.res_entry').map(function() {
				return this.children[thIndex];
			});
			//console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				matchMap.push({
					index : i,
					value : $entries[i].children[0].innerHTML.toUpperCase()
				});
			}
			//console.time("Sorting");
			matchMap.sort(function(a, b){
				if(a.value == b.value)
					return 0;
				return a.value > b.value ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			//console.timeEnd("Sorting");
			this._sortEntries($entries, matchMap);
		},
		
		// Sort by the order the items appeared at page load
		_originalOrderSort : function(inverse) {
			//console.time("Finding elements");
			var $entries = [];
			for (var index in this.resultObjectList.resultObjects) {
				var resultObject = this.resultObjectList.resultObjects[index];
				$entries.push(resultObject.getElement()[0]);
			}
			if (inverse)
				$entries = $entries.reverse();
			
			//console.timeEnd("Finding elements");

			this._sortEntries($entries, null, function(){
				return this;
			});
		},
		
		// Sort with a combination of alphabetic and number detection
		_titleSort : function(inverse) {
			var $resultTable = this.$resultTable;
			var titleRegex = new RegExp('(\\d+|[^\\d]+)', 'g');
			var matchMap = [];
			//console.time("Finding elements");
			var $entries = $resultTable.find('.res_entry > .itemdetails');
			//console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				var text = $entries[i].children[0].children[0].innerHTML.toUpperCase();
				var textParts = text.match(titleRegex);
				matchMap.push({
					index : i,
					text : text,
					value : (textParts == null) ? [] : textParts
				});
			}
			//console.time("Sorting");
			matchMap.sort(function(a, b) {
				if (a.text == b.text)
					return 0;
				var i = 0;
				for (; i < a.value.length && i < b.value.length && a.value[i] == b.value[i]; i++);
				
				// Whoever ran out of entries first, loses
				if (i == a.value.length)
					if (i == b.value.length)
						return 0;
					else return inverse ? 1 : -1;
				if (i == b.value.length)
					return inverse ? -1 : 1;
				
				// Do int comparison of unmatched elements
				var aInt = parseInt(a.value[i]);
				if (!isNaN(aInt)) {
						var bInt = parseInt(b.value[i]);
						if (!isNaN(bInt))
							return aInt > bInt ?
									inverse ? -1 : 1
									: inverse ? 1 : -1;
				}
				return a.text > b.text ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			//console.timeEnd("Sorting");
			this._sortEntries($entries, matchMap);
		},
		
		_initBatchOperations : function() {
			var self = this;
			
			$(".select_all").click(function(){
				var checkbox = $(this).children("input");
				var toggleFn = checkbox.prop("checked") ? "select" : "unselect";
				var resultObjects = self.resultObjectList.resultObjects;
				for (var index in resultObjects) {
					resultObjects[index][toggleFn]();
				}
				self.selectionUpdated();
			}).children("input").prop("checked", false);

			this.actionMenu = new ResultTableActionMenu({
				resultObjectList : this.resultObjectList, 
				groups : this.options.resultActions,
				actionHandler : this.actionHandler
			}, $(".result_table_action_menu", this.$resultHeaderTop));
		},
		
		_initEventHandlers : function() {
			var self = this;
			$(document).on('click', ".res_entry", function(e){
				$(this).data('resultObject').toggleSelect();
				self.selectionUpdated();
			});
			this.$resultTable.on('click', ".res_entry a", function(e){
				e.stopPropagation();
			});
		},
		
		selectionUpdated : function() {
			this.actionMenu.selectionUpdated();
		},
		
		//Initializes the droppable elements used in move operations
		_initMoveLocations : function() {
			// Jquery result containing all elements to use as move drop zones
			this.addMoveDropLocation(this.$resultTable, ".res_entry.container.move_into .title", function($dropTarget){
				var dropObject = $dropTarget.closest(".res_entry").data("resultObject");
				// Needs to be a valid container with sufficient perms
				if (!dropObject || !dropObject.isContainer || $.inArray("addRemoveContents", dropObject.metadata.permissions) == -1) return false;
				return dropObject.metadata;
			});
		},
		
		deactivateMove : function() {
			this.dragTargets = null;
			this.dropActive = false;
			this.move = false;
			for (var index in this.dropLocations) {
				this.dropLocations[index].setMoveActive(false);
			}
		},
		
		activateMove : function() {
			this.move = true;
			for (var index in this.dropLocations) {
				this.dropLocations[index].setMoveActive(true);
			}
		},
		
		addMoveDropLocation : function($dropLocation, dropTargetSelector, dropTargetGetDataFunction) {
			if (!this.dropLocations)
				this.dropLocations = [];
			var dropLocation = new MoveDropLocation($dropLocation, {
				dropTargetSelector : dropTargetSelector,
				dropTargetGetDataFunction : dropTargetGetDataFunction,
				manager : this,
				actionHandler : this.actionHandler
			});
			this.dropLocations.push(dropLocation);
		},
		
		// Initializes draggable elements used in move and reorder operations
		_initReordering : function() {
			var self = this;
			var arrangeMode = false;
			var $resultTable = this.$resultTable;
			
			function setSelected(element) {
				var resultObject = element.closest(".res_entry").data("resultObject");
				if (resultObject.selected) {
					var selecteResults = self.resultObjectList.getSelected();
					self.dragTargets = selecteResults;
				} else {
					self.dragTargets = [resultObject];
				}
			}
			
			$resultTable.sortable({
				delay : 200,
				items: '.res_entry',
				cursorAt : { top: -2, left: -5 },
				forceHelperSize : false,
				scrollSpeed: 100,
				connectWith: '.result_table, .structure_content',
				placeholder : 'arrange_placeholder',
				helper: function(e, element){
					if (!self.dragTargets)
						setSelected(element);
					var representative = element.closest(".res_entry").data("resultObject");
					var metadata = representative.metadata;
					// Indicate how many extra items are being moved
					var additionalItemsText = "";
					if (self.dragTargets.length > 1)
						additionalItemsText = " (and " + (self.dragTargets.length - 1) + " others)";
					// Return helper for representative entry
					var helper = $("<div class='move_helper'><span><img src='/static/images/admin/type_" + metadata.type.toLowerCase() + ".png'/>" + metadata.title + "</span>" + additionalItemsText + "</div>");
					//helper.width(300);
					return helper;
				},
				appendTo: document.body,
				start: function(e, ui) {
					// Hide the original items for a reorder operation
					if (self.dragTargets && false) {
						$.each(self.dragTargets, function() {
							this.element.hide();
						});
					} else {
						ui.item.show();
					}
					// Set the table to move mode and enable drop zone hover highlighting
					self.activateMove();
				},
				stop: function(e, ui) {
					// Move drop mode overrides reorder
					if (self.dropActive) {
						return false;
					}
					if (self.dragTargets) {
						$.each(self.dragTargets, function() {
							this.element.show();
						});
						self.dragTargets = null;
					}
					self.deactivateMove();
					return false;
					
					/*if (!moving && !arrangeMode)
						return false;
					var self = this;
					if (this.selected) {
						$.each(this.selected, function(index){
							if (index < self.itemSelectedIndex)
								ui.item.before(self.selected[index]);
							else if (index > self.itemSelectedIndex)
								$(self.selected[index - 1]).after(self.selected[index]);
						});
					}*/
				},
				update: function (e, ui) {
					/*if (!moving && !arrangeMode)
						return false;
					if (ui.item.hasClass('selected') && this.selected.length > 0)
						this.selected.hide().show(300);
					else ui.item.hide().show(300);*/
				}
			});
		},
		
		setEnableSort : function(value) {
			this.options.enableSort = value;
			if (value) {
				$("th.sort_col").removeClass("sorting");
			} else {
				$("th.sort_col").addClass("sorting");
			}
		}
	});
});
	