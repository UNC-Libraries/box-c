define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'URLUtilities', 'ParentResultObject', 'AddMenu', 
		'ResultObjectActionMenu', 'PublishBatchButton', 'UnpublishBatchButton', 'DeleteBatchButton', 'ConfirmationDialog', 'detachplus'], 
		function($, ui, ResultObjectList, URLUtilities, ParentResultObject, AddMenu, ResultObjectActionMenu,
				PublishBatchButton, UnpublishBatchButton, DeleteBatchButton, ConfirmationDialog) {
	$.widget("cdr.resultTableView", {
		options : {
			enableSort : true,
			ajaxSort : false,
			metadataObjects : undefined,
			enableArrange : false,
			enableMove : false,
			pagingActive : false,
			container : undefined
		},
		
		_create : function() {
			this.$resultTable = this.element.find('.result_table').eq(0);
			var fragment = $(document.createDocumentFragment());
			this.resultObjectList = new ResultObjectList({
				'metadataObjects' : this.options.metadataObjects, 
				parent : this.$resultTable.children('tbody')
			});
			if (this.options.container) {
				this.containerObject = new ParentResultObject({metadata : this.options.container, 
						resultObjectList : this.resultObjectList, element : $(".container_entry")});
				this._initializeAddMenu();
			}
			
			if (this.options.enableSort)
				this._initSort();
			this._initBatchOperations();
			this._initEventHandlers();
			this.actionMenu = new ResultObjectActionMenu({
				selector : ".action_gear",
				containerSelector : ".res_entry,.container_entry"
			});
			this._initMoveLocations();
			this._initReordering();
		},
		
		// Initialize sorting headers according to whether or not paging is active
		_initSort : function() {
			var $resultTable = this.$resultTable;
			var self = this;
			if (this.options.pagingActive) {
				// Paging active, so need to make server callback to perform sort
				var sortParam = URLUtilities.getParameter('sort');
				var sortOrder = URLUtilities.getParameter('sortOrder');
				$("th.sort_col", $resultTable).each(function(){
					var $this = $(this);
					$this.addClass('sorting');
					var sortField = $this.attr('data-field');
					if (sortField) {
						var order = '';
						if (sortParam == sortField) {
							if (sortOrder) {
								$this.addClass('asc');
							} else {
								$this.addClass('desc');
								order = 'reverse';
							}
						}
						var sortUrl = URLUtilities.setParameter(self.options.resultUrl, 'sort', sortField);
						sortUrl = URLUtilities.setParameter(sortUrl, 'sortOrder', order);
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
						console.time("Sort total");
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
						console.timeEnd("Sort total");
					});
				});
			}
		},
		
		// Base row sorting function
		_sortEntries : function($entries, matchMap, getSortable) {
			console.time("Reordering elements");
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
			
			console.timeEnd("Reordering elements");
		},
		
		// Simple alphanumeric result entry sorting
		_alphabeticSort : function(thIndex, inverse) {
			var $resultTable = this.$resultTable;
			var matchMap = [];
			console.time("Finding elements");
			var $entries = $resultTable.find('tr.res_entry').map(function() {
				return this.children[thIndex];
			});
			console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				matchMap.push({
					index : i,
					value : $entries[i].children[0].innerHTML.toUpperCase()
				});
			}
			console.time("Sorting");
			matchMap.sort(function(a, b){
				if(a.value == b.value)
					return 0;
				return a.value > b.value ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			console.timeEnd("Sorting");
			this._sortEntries($entries, matchMap);
		},
		
		// Sort by the order the items appeared at page load
		_originalOrderSort : function(inverse) {
			console.time("Finding elements");
			var $entries = [];
			for (var index in this.resultObjectList.resultObjects) {
				var resultObject = this.resultObjectList.resultObjects[index];
				$entries.push(resultObject.getElement()[0]);
			}
			if (inverse)
				$entries = $entries.reverse();
			
			console.timeEnd("Finding elements");

			this._sortEntries($entries, null, function(){
				return this;
			});
		},
		
		// Sort with a combination of alphabetic and number detection
		_titleSort : function(inverse) {
			var $resultTable = this.$resultTable;
			var titleRegex = new RegExp('(\\d+|[^\\d]+)', 'g');
			var matchMap = [];
			console.time("Finding elements");
			var $entries = $resultTable.find('.res_entry > .itemdetails');
			console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				var text = $entries[i].children[0].children[0].innerHTML.toUpperCase();
				var textParts = text.match(titleRegex);
				matchMap.push({
					index : i,
					text : text,
					value : (textParts == null) ? [] : textParts
				});
			}
			console.time("Sorting");
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
			console.timeEnd("Sorting");
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
			}).children("input").prop("checked", false);
			
			var publishButton = $(".publish_selected", self.element);
			var publishBatch = new PublishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.setStatusText('Publishing...');
						this.updateOverlay('show');
					}, 
				'followupFunction' : function() {
					this.setStatusText('Publishing....');
				}, 
				'completeFunction' : function(){
					this.refresh(true);
				}
			}, publishButton);
			publishButton.click(function(){
				publishBatch.activate();
			});
			var unpublishButton = $(".unpublish_selected", self.element);
			var unpublishBatch = new UnpublishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.setStatusText('Unpublishing...');
						this.updateOverlay('show');
					}, 
				'followupFunction' : function() {
					this.setStatusText('Unpublishing....');
				}, 
				'completeFunction' : function(){
					this.refresh(true);
				}
			}, unpublishButton);
			unpublishButton.click(function(){
				unpublishBatch.activate();
			});
			var deleteButton = $(".delete_selected", self.element);
			var deleteBatch = new DeleteBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.setStatusText('Deleting...');
						this.updateOverlay('show');
					}, 
				'followupFunction' : function() {
						this.setStatusText('Cleaning up...');
					}, 
				'completeFunction' : 'deleteElement'
			}, deleteButton);
			deleteButton.click(function(){
				deleteBatch.activate();
			});
		},
		
		_initEventHandlers : function() {
			var self = this;
			$(document).on('click', ".res_entry", function(e){
				$(this).data('resultObject').toggleSelect();
			});
			this.$resultTable.on('click', ".res_entry a", function(e){
				e.stopPropagation();
			});
		},
		
		//Initializes the droppable elements used in move operations
		_initMoveLocations : function() {
			var self = this;
			this.$resultTable.droppable({
				drop : function(event, ui) {
					// Locate which element is being dropped on
					var $dropTarget = $(document.elementFromPoint(event.pageX - $(window).scrollLeft(), event.pageY - $(window).scrollTop()));
					var dropObject = $dropTarget.closest(".res_entry").data("resultObject");
					// Needs to be a valid container with sufficient perms
					if (!dropObject || !dropObject.isContainer || $.inArray("addRemoveContents", dropObject.permissions) != -1) return false;
					// Activate move drop mode
					self.dropActive = true;
					
					// Confirm the move operation before performing it
					var representative = ui.draggable.data("resultObject");
					var repTitle = representative.metadata.title;
					if (repTitle.length > 50) repTitle = repTitle.substring(0, 50) + "...";
					var destTitle = dropObject.metadata.title;
					if (destTitle.length > 50) destTitle = destTitle.substring(0, 50) + "...";
					var promptText = "Move \"<a class='result_object_link' data-id='" + representative.pid + "'>" + repTitle + "</a>\"";
					if (self.dragTargets.length > 1)
						promptText += " and " + (self.dragTargets.length - 1) + " other object" + (self.dragTargets.length - 1 > 1? "s" :"");
					promptText += " into \"<a class='result_object_link' data-id='" + dropObject.pid + "'>" + destTitle + "</a>\"?";
					var confirm = new ConfirmationDialog({
						promptText : promptText,
						modal : true,
						autoOpen : true,
						addClass : "move",
						dialogOptions : {
							width : 'auto',
							maxWidth : 400,
							position : "center center"
						},
						confirmFunction : function() {
							// Perform the move operation and clean up the result entries
							if (self.dragTargets) {
								var moveData = {
										newParent : dropObject.pid,
										ids : []
									};
								$.each(self.dragTargets, function() {
									moveData.ids.push(this.pid);
									this.element.hide();
								});
								// Store a reference to the targeted item list since moving happens asynchronously
								var moveObjects = self.dragTargets;
								$.ajax({
									url : "NOWHERE",
									type : "POST",
									data : JSON.stringify(moveData),
									contentType: "application/json; charset=utf-8",
									dataType: "json",
									success : function(data) {
										$.each(moveObjects, function() {
											this.deleteElement();
										});
										self.options.alertHandler.alertHandler("success", "Moved " + moveObjects.length + " object" + (moveObjects.length > 1? "s" : "") 
												+ " to " + destTitle);
									},
									error : function() {
										$.each(moveObjects, function() {
											this.element.show();
										});
										self.options.alertHandler.alertHandler("error", "Failed to move " + moveObjects.length + " object" + (moveObjects.length > 1? "s" : "") 
												+ " to " + destTitle);
										
									}
								});
							}
							self.dragTargets = null;
							self.$resultTable.removeClass("moving");
							self.dropActive = false;
						},
						cancelFunction : function() {
							// Cancel and revert the page state
							if (self.dragTargets) {
								$.each(self.dragTargets, function() {
									this.element.show();
								});
								self.dragTargets = null;
							}
							self.$resultTable.removeClass("moving");
							self.dropActive = false;
						}
					});
				},
				tolerance: 'pointer',
				over: function(event, ui) {
					$(".ui-sortable-placeholder").hide();
				},
				out: function(event, ui) {
					$(".ui-sortable-placeholder").show();
				}
			});
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
				connectWith: '.result_table, #structure_facet',
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
					$resultTable.addClass("moving")
						.on("mouseenter", ".res_entry.container.move_into .title", function() {
							console.log("Hovering");
							$(this).addClass("drop_hover");
						}).on("mouseleave", ".res_entry.container.move_into .title", function() {
							console.log("Blur");
							$(this).removeClass("drop_hover");
						});
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
					$resultTable.removeClass("moving");
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
		},
		
		// Initialize the menu for adding new items
		_initializeAddMenu : function() {
			this.addMenu = new AddMenu({
				container : this.options.container,
				selector : "#add_menu",
				alertHandler : this.options.alertHandler
			});
		}
	});
});
	