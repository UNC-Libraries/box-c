define('MoveBatchToTrashButton', [ 'jquery', 'BatchCallbackButton' ], function($, BatchCallbackButton) {
	function MoveBatchToTrashButton(options, element) {
		this._create(options, element);
	};
	
	MoveBatchToTrashButton.prototype.constructor = MoveBatchToTrashButton;
	MoveBatchToTrashButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
		childWorkLinkName : "trash",
		confirm: true,
		animateSpeed: 'fast'
	};
	
	MoveBatchToTrashButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};
	
	MoveBatchToTrashButton.prototype.removeFromTrashState = function() {
		this.options.workPath = "/services/rest/edit/removeFromTrash";
		this.options.workLabel = "Removing from trash...";
		this.options.followupLabel = "Removing from trash....";
		this.options.confirmMessage = "Move these object(s) to the Trash?";
	};

	MoveBatchToTrashButton.prototype.moveToTrashState = function() {
		this.options.workPath = "/services/rest/edit/moveToTrash";
		this.options.workLabel = "Moving to trash...";
		this.options.followupLabel = "Moving to trash....";
		this.options.confirmMessage = "Move these object(s) to the Trash?";
	};

	MoveBatchToTrashButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (resultObject.isSelected() && resultObject.isEnabled()) {
				targetIds.push(resultObject.getPid());
			}
		}
		return targetIds;
	};
	
	MoveBatchToTrashButton.prototype.followup = function(data) {
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
						resultObject[this.options.completeFunction]();
				} else {
					resultObject.setState("idle");
				}
			}
		}
		
		return !this.followupObjects;
	};
	
	return MoveBatchToTrashButton;
});