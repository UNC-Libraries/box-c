define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'URLUtilities', 'IsSorted',
		'ResultObjectActionMenu', 'ResultTableActionMenu', 'ConfirmationDialog', 'MoveDropLocation', 'detachplus'], 
		function($, ui, ResultObjectList, URLUtilities, IsSorted, ResultObjectActionMenu, ResultTableActionMenu, ConfirmationDialog, MoveDropLocation) {
	
	function ResultTableView(element, options) {
		this.element = element;
		this.options = $.extend({}, defaultOptions, options);
		
		// Instantiate the result table view and add it to the page
		this.actionHandler = this.options.actionHandler;
		this.actionHandler.addToBaseContext('resultTable', this);
		this.firstRender = true;
	}
	
	var defaultOptions = {
			enableSort : true,
			ajaxSort : false,
			enableArrange : false,
			enableMove : false,
			resultFields : undefined,
			resultHeader : undefined,
			resultActions : undefined,
			headerHeightClass : ''
		};
	
	// Figure out if the list should be sorted already. 
	// Reset sort applied to table variable if context menu or Browse tab are selected.
	var sorted = IsSorted;
	var sortedValues = sorted.getSorted();

	$(document).on('click', ".result_entry_context_menu, #mainmenu li", function() {
		if (sortedValues !== null) {
			sortedValues.reloadRun = false;
			sorted.setSorted(sortedValues);
		}
	});

	if (!sorted.stale() && !sortedValues.reloadRun) {
		location.replace(location.href + sortedValues.sortUrl);

		sortedValues.reloadRun = true;
		sortedValues.sortTime = Date.now();

		sorted.setSorted(sortedValues);
	}
	
	ResultTableView.prototype.render = function(data) {
		var self = this;
		
		require([this.options.resultTableTemplate, this.options.resultEntryTemplate, this.options.resultTableHeaderTemplate, this.options.navBarTemplate, this.options.pathTrailTemplate], function(resultTableTemplate, resultEntryTemplate, resultTableHeaderTemplate, navigationBarTemplate, pathTrailTemplate){
			
			self.element.html("");
			
			self.pagingActive = data.pageRows < data.resultCount;

			self.resultUrl = document.location.href;
			var container = data.container;

			var navigationBar = navigationBarTemplate({
				pageNavigation : data,
				resultUrl : self.resultUrl,
				URLUtilities : URLUtilities
			});
		
			var containerPath = null;
			if (container) {
				containerPath = pathTrailTemplate({
					objectPath : container.objectPath,
					queryMethod : 'list',
					filterParams : data.searchQueryUrl,
					skipLast : true
				});
			}
			
			var resultTableHeader = resultTableHeaderTemplate({
				data : data,
				container : container,
				navigationBar : navigationBar,
				containerPath : containerPath,
				queryMethod : data.queryMethod
			});
			
			var headerHeightClass = self.options.headerHeightClass;
			if (container && container.ancestorPath) {
				headerHeightClass += " with_path";
			} else {
				headerHeightClass += " with_container";
			}
			
			if (self.$resultView) {
				self.$resultView.remove();
			}
			self.$resultView = $(resultTableTemplate({resultFields : self.options.resultFields, container : container,
					resultHeader : resultTableHeader, headerHeightClass : headerHeightClass}));
			self.$resultTable = self.$resultView.find('.result_table').eq(0);
			self.$resultHeaderTop = self.$resultView.find('.result_header_top').eq(0);
			self.$noResults = self.$resultView.find('.no_results').eq(0);
			self.element.append(self.$resultView);
		
			self.populateResults(data.metadata);
		
			// Activate sorting
			if (self.options.enableSort)
				self._initSort();
			
			// Initialize batch operation buttons
			self._initBatchOperations();
		
			if (self.firstRender) {
				self._initEventHandlers();
			}
		
			// Activate the result entry context menus, on the action gear and  clicking
			self.contextMenus = [
				// Bind left and right click to open menu for the gear
				new ResultObjectActionMenu({
					trigger : 'left',
					selector : ".action_gear",
					containerSelector : ".res_entry,.container_entry",
					actionHandler : self.actionHandler,
					alertHandler : self.options.alertHandler
				}),
				new ResultObjectActionMenu({
					trigger : 'right',
					selector : ".action_gear",
					containerSelector : ".res_entry,.container_entry",
					actionHandler : self.actionHandler,
					alertHandler : self.options.alertHandler
				}),
				// Bind just right click for opening menu on rest of the entry
				new ResultObjectActionMenu({
					trigger : 'right',
					positionAtTrigger : false,
					selector : ".res_entry td:not(:last-child)",
					containerSelector : ".res_entry,.container_entry",
					actionHandler : self.actionHandler,
					alertHandler : self.options.alertHandler,
					multipleSelectionEnabled : true,
					resultList : self.resultObjectList,
					batchActions : self.options.resultActions
				})
			];
		
			// Initialize click and drag operations
			self._initMoveLocations();
			self._initReordering();
			
			self.firstRender = false;
		});
	};
	
	ResultTableView.prototype.populateResults = function(metadataObjects) {

		this.$resultTable.children('tbody').html("");
		
		// Generate result entries
		this.resultObjectList = new ResultObjectList({
			metadataObjects : metadataObjects, 
			parent : this.$resultTable.children('tbody'),
			resultEntryTemplate : this.options.resultEntryTemplate
		});
	
		// No results message
		if (metadataObjects.length == 0) {
			this.$noResults.removeClass("hidden");
		} else {
			this.$noResults.addClass("hidden");
		}
	};
	
	// Initialize sorting headers according to whether or not paging is active
	ResultTableView.prototype._initSort = function() {
		var $resultTable = this.$resultTable;
		var self = this;
		
		if (!self.sortType) {
			var sortOrder = true;
			var sortParam = URLUtilities.getParameter('sort');
			if (sortParam != null) {
				sortParam = sortParam.split(",");
				if (sortParam.length > 1)
					sortOrder = "reverse" != sortParam[1];
				if (sortParam.length > 0)
					sortParam = sortParam[0];
			}
			self.sortType = sortParam;
			self.sortOrder = sortOrder;
		}
		
		$("th.sort_col", $resultTable).each(function(){
			var $this = $(this);
			$this.addClass('sorting');
			var sortField = $this.attr('data-field');
			if (sortField) {
				// If the results are already sorted at init time, make the column reflect that
				var isCurrentSortField = self.sortType == sortField;
				if (isCurrentSortField) {
					if (self.sortOrder) {
						$this.addClass('desc');
					} else {
						$this.addClass('asc');
					}
				}
				// Set the sort URL for the column
				var orderParam = isCurrentSortField && self.sortOrder? ",reverse" : "";
				var sortUrl = URLUtilities.setParameter(self.resultUrl, 'sort', sortField + orderParam);
				this.children[0].href = sortUrl;
				
				// If we're in paging mode, make the column link trigger a retrieval from server
				if (self.pagingActive) {
					$("a", $this).addClass("res_link");
				}
				
				var $th = $(this);
				var thIndex = $th.index();
				var dataType = $th.attr("data-type");
				
				$th.click(function(){
					if (!$th.hasClass('sorting')) return;
					//console.time("Sort total");
					var inverse = $th.hasClass('desc');
					$('.sorting', $resultTable).removeClass('asc desc');
					if (inverse)
						$th.addClass('asc');
					else 
						$th.addClass('desc');
					
					self.sortType = $th.attr("data-field");
					if (!self.pagingActive) {
						self.sortOrder = !inverse;
						
						var sortUrl = URLUtilities.setParameter(self.resultUrl, 'sort', self.sortType + (!self.sortOrder? ",reverse" : ""));
						
						var sortSettings = {
							sortUrl: URLUtilities.getAllParameters(sortUrl),
							sortTime: Date.now(),
							reloadRun: false
						};

						IsSorted.setSorted(sortSettings);
						
						if (history.pushState) {
							history.pushState({}, "", sortUrl);
						}
				
						// Apply sort function based on data-type
						if (dataType == 'index') {
							self._originalOrderSort(inverse);
						} else if (dataType == 'title') {
							self._titleSort(inverse);
						} else {
							self._alphabeticSort(thIndex, inverse);
						}
						inverse = !inverse;
						return false;
					} else {
						self.sortOrder = !inverse;
					}
					//console.timeEnd("Sort total");
				});
				
				
			}
		});
	};
	
	ResultTableView.prototype.getCurrentSort = function() {
		return {type : this.sortType, order : this.sortOrder};
	};
	
	// Base row sorting function
	ResultTableView.prototype._sortEntries = function($entries, matchMap, getSortable) {
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
	};
	
	// Simple alphanumeric result entry sorting
	ResultTableView.prototype._alphabeticSort = function(thIndex, inverse) {
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
	};
	
	// Sort by the order the items appeared at page load
	ResultTableView.prototype._originalOrderSort = function(inverse) {
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
	};
	
	// Sort with a combination of alphabetic and number detection
	ResultTableView.prototype._titleSort = function(inverse) {
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
	};
	
	ResultTableView.prototype._initBatchOperations = function() {
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
	};
	
	ResultTableView.prototype._initEventHandlers = function() {
		var self = this;
		$(document).on('click', ".res_entry", function(e){
			$(this).data('resultObject').toggleSelect();
			self.selectionUpdated();
		});
	};
	
	ResultTableView.prototype.selectionUpdated = function() {
		this.actionMenu.selectionUpdated();
		var selectedCount = 0;
		for (var index in this.resultObjectList.resultObjects) {
			if (this.resultObjectList.resultObjects[index].isSelected()) selectedCount++;
		}
		this.contextMenus[1].setSelectedCount(selectedCount);
	};
	
	//Initializes the droppable elements used in move operations
	ResultTableView.prototype._initMoveLocations = function() {
		// Jquery result containing all elements to use as move drop zones
		this.addMoveDropLocation(this.$resultTable, ".res_entry.container.move_into .title", function($dropTarget){
			var dropObject = $dropTarget.closest(".res_entry").data("resultObject");
			// Needs to be a valid container with sufficient perms
			if (!dropObject || !dropObject.isContainer || $.inArray("move", dropObject.metadata.permissions) == -1) return false;
			return dropObject.metadata;
		});
	};
	
	ResultTableView.prototype.deactivateMove = function() {
		this.dragTargets = null;
		this.dropActive = false;
		this.move = false;
		for (var index in this.dropLocations) {
			this.dropLocations[index].setMoveActive(false);
		}
	};
	
	ResultTableView.prototype.activateMove = function() {
		this.move = true;
		for (var index in this.dropLocations) {
			this.dropLocations[index].setMoveActive(true);
		}
	};
	
	ResultTableView.prototype.addMoveDropLocation = function($dropLocation, dropTargetSelector, dropTargetGetDataFunction) {
		if (!this.dropLocations)
			this.dropLocations = [];
		var dropLocation = new MoveDropLocation($dropLocation, {
			dropTargetSelector : dropTargetSelector,
			dropTargetGetDataFunction : dropTargetGetDataFunction,
			manager : this,
			actionHandler : this.actionHandler
		});
		this.dropLocations.push(dropLocation);
	};
	
	// Initializes draggable elements used in move and reorder operations
	ResultTableView.prototype._initReordering = function() {
		var self = this;
		var arrangeMode = false;
		var $resultTable = this.$resultTable;
		
		function setSelected(element) {
			var resultObject = element.closest(".res_entry").data("resultObject");
			if (!resultObject) {
				return;
			}
			if (resultObject.selected) {
				var selecteResults = self.resultObjectList.getSelected();
				self.dragTargets = selecteResults;
			} else if (resultObject.isSelectable()){
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
			cancel : '.working, .moving',
			helper : function(e, element){
				if (!self.dragTargets) {
					setSelected(element);
				}

				var representative = element.closest(".res_entry").data("resultObject");
				var metadata = representative.metadata;
				
				// Indicate how many items are being moved
				var howManyItemsText;
				if (self.dragTargets.length == 1) {
					howManyItemsText = "";
				} else {
					howManyItemsText = " (" + self.dragTargets.length + " items)";
				}
				
				// Return helper for representative entry
				var helper = $("<div class='move_helper'><span><div class='resource_icon " + metadata.type.toLowerCase() + "'></div>" + metadata.title + "</span>" + howManyItemsText + "</div>");
				
				return helper;
			},
			appendTo: document.body,
			start: function(e, ui) {
				ui.item.show();
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
			}
		});
	};
	
	ResultTableView.prototype.setEnableSort = function(value) {
		this.options.enableSort = value;
		if (value) {
			$("th.sort_col").removeClass("sorting");
		} else {
			$("th.sort_col").addClass("sorting");
		}
	};
	
	ResultTableView.prototype.getResultObjectList = function () {
		return this.resultObjectList;
	};
	
	return ResultTableView;
});
	