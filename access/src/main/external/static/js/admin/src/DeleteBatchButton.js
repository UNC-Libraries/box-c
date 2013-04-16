define([ 'jquery', 'jquery-ui', 'BatchCallbackButton' ], function($) {
	$.widget("cdr.deleteBatchButton", $.cdr.batchCallbackButton, {
		options : {
			resultObjectList : undefined,
			workPath: "delete",
			childWorkLinkName : "delete",
			confirm: true,
			confirmMessage: "Delete selected object(s)?",
			animateSpeed: 'fast'
		},

		getTargetIds : function() {
			var targetIds = [];
			for (var id in this.options.resultObjectList.resultObjects) {
				var resultObject = this.options.resultObjectList.resultObjects[id];
				if (resultObject.resultObject("isSelected") && resultObject.resultObject("isEnabled")) {
					targetIds.push(resultObject.resultObject("getPid").getPid());
				}
			}
			return targetIds;
		},
		
		followup : function(data) {
			var removedIds;
			var emptyData = jQuery.isEmptyObject(data.length);
			if (emptyData){
				removedIds = this.followupObjects;
			} else {
				removedIds = [];
				for (var index in this.followupObjects) {
					var id = this.followupObjects[index];
					if (!(id in data)) {
						removedIds.push(id);
					}
				}
			}
			
			if (removedIds.length > 0) {
				if (emptyData)
					this.followupObjects = null;
				for (var index in removedIds) {
					var id = removedIds[index];
					// Don't bother trimming out followup objects if all ids are complete
					if (!emptyData) {
						var followupIndex = $.inArray(id, this.followupObjects);
						this.followupObjects.splice(followupIndex, 1);
					}
					
					var resultObject = this.options.resultObjectList.resultObjects[id];
					// Trigger the complete function on targeted child callback buttons
					if (this.options.completeFunction) {
						if ($.isFunction(this.options.completeFunction))
							this.options.completeFunction.call(resultObject);
						else
							resultObject.resultObject(this.options.completeFunction);
					} else {
						resultObject.resultObject("setState", "idle");
					}
				}
			}
			
			return !this.followupObjects;
		}
	});
});