define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'PublishBatchButton', 'UnpublishBatchButton', 'DeleteBatchButton', 'detachplus'], 
		function($, ui, ResultObjectList) {
	$.widget("cdr.resultTableView", {
		options : {
			enableSort : true,
			ajaxSort : false,
			metadataObjects : undefined,
			enableArrange : false,
			enableMove : false
		},
		
		_create : function() {
			this.resultObjectList = new ResultObjectList({'metadataObjects' : this.options.metadataObjects});
			
			if (this.options.enableSort)
				this._initSort();
			this._initBatchOperations();
			this._initEventHandlers();
		},

		_initSort : function() {
			var $resultTable = this.element;
			var self = this;
			$("th.sort_col", $resultTable).wrapInner('<span/>').each(function(){
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
		},
		
		_sortEntries : function($entries, matchMap, getSortable) {
			console.time("Reordering elements");
			var $resultTable = this.element;
			
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
			var $resultTable = this.element;
			var matchMap = [];
			console.time("Finding elements");
			var $entries = $resultTable.find('tr.res_entry').map(function() {
				return this.children[thIndex];
			});
			console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				matchMap.push({
					index : i,
					value : $entries[i].innerHTML.toUpperCase()
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
			
//			
//			var $entries = $resultTable.find('tr.res_entry');
//			
//			console.timeEnd("Finding elements");
//			for (var i = 0, length = $entries.length; i < length; i++) {
//				matchMap.push({
//					index : i,
//					value : $entries.eq(i).data('original_index')
//				});
//			}
//			console.time("Sorting");
//			matchMap.sort(function(a, b){
//				return (a.value > b.value) ?
//						inverse ? -1 : 1
//						: inverse ? 1 : -1;
//			});
//			console.timeEnd("Sorting");
//			this._sortEntries($entries, matchMap, function(){
//				return this;
//			});
		},
		
		_titleSort : function(inverse) {
			var $resultTable = this.element;
			var titleRegex = new RegExp('(\\d+|[^\\d]+)', 'g');
			var matchMap = [];
			console.time("Finding elements");
			var $entries = $resultTable.find('.itemdetails');
			console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				var text = $entries[i].children[0].innerHTML.toUpperCase();
				matchMap.push({
					index : i,
					text : text,
					value : text.match(titleRegex)
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
			
			$(".select_all", self.element).click(function(){
				var resultObjects = self.resultObjectList.resultObjects;
				for (var index in resultObjects) {
					resultObjects[index].select();
				}
			});
			
			
			$(".deselect_all", self.element).click(function(){
				var resultObjects = self.resultObjectList.resultObjects;
				for (var index in resultObjects) {
					resultObjects[index].unselect();
				}
			});
			
			$(".publish_selected", self.element).publishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						var resultObject = this.data('resultObject');
						resultObject.setStatusText('Publishing...');
						resultObject.updateOverlay('show');
					}, 
				'followupFunction' : function() {
					this.data('resultObject').setStatusText('Publishing....');
				}, 
				'completeFunction' : function(){
					this.data('resultObject').refresh(true);
				}
			});
			$(".unpublish_selected", self.element).unpublishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						var resultObject = this.data('resultObject');
						resultObject.setStatusText('Unpublishing...');
						resultObject.updateOverlay('show');
					}, 
				'followupFunction' : function() {
					this.data('resultObject').setStatusText('Unpublishing....');
				}, 
				'completeFunction' : function(){
					this.data('resultObject').refresh(true);
				}
			});
			$(".delete_selected", self.element).deleteBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						var resultObject = this.data('resultObject');
						resultObject.setStatusText('Deleting...');
						resultObject.updateOverlay('show');
					}, 
				'followupFunction' : function() {
						this.data('resultObject').setStatusText('Cleaning up...');
					}, 
				'completeFunction' : 'deleteElement'
			});
		},
		
		_initEventHandlers : function() {
			this.element.on('click', ".menu_box img", function(e){
				$(this).parents(".res_entry").data('resultObject').activateActionMenu();
				e.stopPropagation();
			});
			this.element.on('click', ".res_entry", function(e){
				$(this).data('resultObject').toggleSelect();
				e.stopPropagation();
			});
			this.element.on('click', ".res_entry a", function(e){
				e.stopPropagation();
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
	