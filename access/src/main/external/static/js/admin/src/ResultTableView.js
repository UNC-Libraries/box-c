define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'PublishBatchButton', 'UnpublishBatchButton', 'DeleteBatchButton', 'sortElements'], 
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
			$('tbody tr', this.element).each(function(i){
				$(this).data('original_index', i);
			});
		},

		_initSort : function() {
			var $resultTable = this.element;
			$("th.sort_col", $resultTable).wrapInner('<span/>').each(function(){
				var $th = $(this),
				thIndex = $th.index(),
				dataType = $th.attr("data-type");
				$th.addClass('sorting');
				
				$th.click(function(){
					if (!$th.hasClass('sorting')) return;
					var inverse = $th.hasClass('desc');
					$('.sorting', $resultTable).removeClass('asc desc');
					if (inverse)
						$th.addClass('asc');
					else 
						$th.addClass('desc');
					
					// Apply sort function based on data-type
					if (dataType == 'index') {
						$resultTable.find('tbody tr.entry').sortElements(function(a, b){
							return ($(a).data('original_index') > $(b).data('original_index')) ?
									inverse ? -1 : 1
									: inverse ? 1 : -1;
						});
					} else {
						$resultTable.find('td').filter(function(){
							return $(this).index() === thIndex;
						}).sortElements(function(a, b){
							if( $.text([a]).toUpperCase() == $.text([b]).toUpperCase() )
								return 0;
							return $.text([a]).toUpperCase() > $.text([b]).toUpperCase() ?
									inverse ? -1 : 1
									: inverse ? 1 : -1;
						}, function(){
							// parentNode is the element we want to move
							return this.parentNode; 
						});
					}
					inverse = !inverse;
				});
				
			});
		},
		
		_initBatchOperations : function() {
			var self = this;
			$(".select_all", self.element).click(function(){
				$(".selectable", self.element).resultObject('select');
			});
			
			$(".deselect_all", self.element).click(function(){
				$(".selectable", self.element).resultObject('unselect');
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
			this.element.on('click', ".selectable", function(e){
				$(this).resultObject('toggleSelect');
				e.stopPropagation();
			});
			this.element.on('click', ".selectable a", function(e){
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
	