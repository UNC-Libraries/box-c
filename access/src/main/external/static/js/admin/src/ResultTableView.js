define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'URLUtilities', 'PublishBatchButton', 'UnpublishBatchButton', 'DeleteBatchButton', 'detachplus'], 
		function($, ui, ResultObjectList, URLUtilities) {
	$.widget("cdr.resultTableView", {
		options : {
			enableSort : true,
			ajaxSort : false,
			metadataObjects : undefined,
			enableArrange : false,
			enableMove : false,
			pagingActive : false
		},
		
		_create : function() {
			this.$resultTable = this.element.find('.result_table').eq(0);
			var fragment = $(document.createDocumentFragment());
			this.resultObjectList = new ResultObjectList({'metadataObjects' : this.options.metadataObjects, parent : this.$resultTable.children('tbody')});
			//this.$resultTable.children('tbody').append(fragment);
			
			if (this.options.enableSort)
				this._initSort();
			this._initBatchOperations();
			this._initEventHandlers();
			//this._initReordering();
		},
		
		_initSort : function() {
			var $resultTable = this.$resultTable;
			var self = this;
			if (this.options.pagingActive) {
				var sortParam = URLUtilities.getParameter('sort');
				var sortOrder = URLUtilities.getParameter('sortOrder');
				$("th.sort_col", $resultTable).each(function(){
					var $this = $(this);
					$this.addClass('sorting');
					var sortField = $this.attr('data-field');
					if (sortField) {
						var order = '';
						//console.log(sortParam + "|" + sortField + "|" + sortOrder + "|" + (!sortOrder));
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
						//console.log(sortUrl);
					}
				});
			} else {
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
			
			$(".publish_selected", self.element).publishBatchButton({
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
			});
			$(".unpublish_selected", self.element).unpublishBatchButton({
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
			});
			$(".delete_selected", self.element).deleteBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.setStatusText('Deleting...');
						this.updateOverlay('show');
					}, 
				'followupFunction' : function() {
						this.setStatusText('Cleaning up...');
					}, 
				'completeFunction' : 'deleteElement'
			});
		},
		
		_initEventHandlers : function() {
			this.$resultTable.on('click', ".menu_box img", function(e){
				$(this).parents(".res_entry").data('resultObject').activateActionMenu();
				e.stopPropagation();
			});
			this.$resultTable.on('click', ".res_entry", function(e){
				$(this).data('resultObject').toggleSelect();
				e.stopPropagation();
			});
			this.$resultTable.on('click', ".res_entry a", function(e){
				e.stopPropagation();
			});
		},
		
		_initReordering : function() {
			var arrangeMode = true;
			var $resultTable = this.$resultTable;
			$resultTable.sortable({
				delay : 200,
				items: '.res_entry',
				cursorAt : { top: -2, left: -5 },
				forceHelperSize : false,
				scrollSpeed: 100,
				/*connectWith: '.hier_entry, .entry.container',*/
				placeholder : 'arrange_placeholder',
				helper: function(e, element){
					var title = $($('.itemdetails a', element)[0]).html();
					if ($(element).hasClass('selected')) {
						this.selected = element.parent().children(".selected");
						if (this.selected.length > 1) {
							return $("<div class='move_helper'><img src='/static/images/admin/type_folder.png'/><span>" + title + "</span> (and " + (this.selected.length - 1) + " others)</div>");
						}
					}
					return $("<div class='move_helper'><span><img src='/static/images/admin/type_folder.png'/>" + title + "</span></div>");
				},
				appendTo: document.body,
				start: function(e, ui) {
					moving = false;
					ui.item.show();
					var self = this;
					if (this.selected) {
						$.each(this.selected, function(index){
							if (self.selected[index] === ui.item[0]) {
								self.itemSelectedIndex = index;
								return false;
							}
						});
					}
				},
				stop: function(e, ui) {
					if (!moving && !arrangeMode)
						return false;
					var self = this;
					if (this.selected) {
						$.each(this.selected, function(index){
							if (index < self.itemSelectedIndex)
								ui.item.before(self.selected[index]);
							else if (index > self.itemSelectedIndex)
								$(self.selected[index - 1]).after(self.selected[index]);
						});
					}
				},
				update: function (e, ui) {
					if (!moving && !arrangeMode)
						return false;
					if (ui.item.hasClass('selected') && this.selected.length > 0)
						this.selected.hide().show(300);
					else ui.item.hide().show(300);
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
	