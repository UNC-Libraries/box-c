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
			this._assignOriginalIndex();
			this._initBatchOperations();
			this._initEventHandlers();
		},
		
		_assignOriginalIndex : function() {
			console.time("Indexes");
			$('.res_entry', this.element).each(function(i){
				$(this).data('original_index', i);
			});
			console.timeEnd("Indexes");
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
					console.time("Sorting");
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
					console.timeEnd("Sorting");
				});
			});
		},
		
		_sortEntries : function($entries, matchMap, getSortable) {
			var $resultTable = this.element;
			$resultTable.detach(true, function(reattach){
				var resultRows = $resultTable[0].children[0];
				if ($.isFunction(getSortable)) {
					for (var i = 0, length = matchMap.length; i < length; i++) {
						resultRows.insertBefore(getSortable.call($entries[matchMap[i].index]), null);
					}
				} else {
					for (var i = 0, length = matchMap.length; i < length; i++) {
						resultRows.insertBefore($entries[matchMap[i].index].parentNode, null);
					}
				}
				reattach();
			});
		},
		
		_alphabeticSort : function(thIndex, inverse) {
			var $resultTable = this.element;
			var matchMap = [];
			var $entries = $resultTable.find('td').filter(function(){
				return $(this).index() === thIndex;
			});
			for (var i = 0, length = $entries.length; i < length; i++) {
				matchMap.push({
					index : i,
					value : $entries[i].innerHTML.toUpperCase()
				});
			}
			matchMap.sort(function(a, b){
				if(a.value == b.value)
					return 0;
				return a.value > b.value ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			this._sortEntries($entries, matchMap);
		},
		
		_originalOrderSort : function(inverse) {
			var $resultTable = this.element;
			var matchMap = [];
			var $entries = $resultTable.find('tr.res_entry');
			for (var i = 0, length = $entries.length; i < length; i++) {
				matchMap.push({
					index : i,
					value : $entries.eq(i).data('original_index')
				});
			}
			matchMap.sort(function(a, b){
				return (a.value > b.value) ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			this._sortEntries($entries, matchMap, function(){
				return this;
			});
		},
		
		_titleSort : function(inverse) {
			var $resultTable = this.element;
			var titleRegex = new RegExp('(\\d+|[^\\d]+)', 'g');
			var matchMap = [];
			var $entries = $resultTable.find('.itemdetails');
			for (var i = 0, length = $entries.length; i < length; i++) {
				var text = $entries[i].children[0].innerHTML.toUpperCase();
				matchMap.push({
					index : i,
					text : text,
					value : text.match(titleRegex)
				});
			}
			
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
			this._sortEntries($entries, matchMap);
		},
		
		_initBatchOperations : function() {
			var self = this;
			$(".select_all", self.element).click(function(){
				$(".res_entry", self.element).resultObject('select');
			});
			
			$(".deselect_all", self.element).click(function(){
				$(".res_entry", self.element).resultObject('unselect');
			});
			
			$(".publish_selected", self.element).publishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.resultObject('setStatusText', 'Publishing...');
						this.resultObject('updateOverlay', 'show');
					}, 
				'followupFunction' : function() {
					this.resultObject('setStatusText', 'Publishing....');
				}, 
				'completeFunction' : function(){
					this.resultObject('refresh', true);
				}
			});
			$(".unpublish_selected", self.element).unpublishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
					this.resultObject('setStatusText', 'Unpublishing...');
					this.resultObject('updateOverlay', 'show');
					}, 
				'followupFunction' : function() {
					this.resultObject('setStatusText', 'Unpublishing....');
				}, 
				'completeFunction' : function(){
					this.resultObject('refresh', true);
				}
			});
			$(".delete_selected", self.element).deleteBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
					this.resultObject('setStatusText', 'Deleting...');
					this.resultObject('updateOverlay', 'show');
					}, 
				'followupFunction' : function() {
					this.resultObject('setStatusText', 'Cleaning up...');
				}, 
				'completeFunction' : 'deleteElement'
			});
		},
		
		_initEventHandlers : function() {
			this.element.on('click', ".menu_box img", function(e){
				$(this).parents(".entry").resultObject('activateActionMenu');
				e.stopPropagation();
			});
			this.element.on('click', ".res_entry", function(e){
				$(this).resultObject('toggleSelect');
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
	