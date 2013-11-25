define('RemoveBatchFromTrashButton', [ 'jquery', 'BatchCallbackButton', 'MoveObjectToTrashButton'], function($, BatchCallbackButton, MoveObjectToTrashButton) {
	function RemoveBatchFromTrashButton(options, element) {
		this._create(options, element);
	};
	
	RemoveBatchFromTrashButton.prototype.constructor = RemoveBatchFromTrashButton;
	RemoveBatchFromTrashButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
		childWorkLinkName : "restore",
		confirm: true,
		animateSpeed: 'fast'
	};
	
	RemoveBatchFromTrashButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		
		merged.workPath = "/services/api/edit/removeFromTrash";
		merged.workLabel = "Restoring from trash...";
		merged.followupLabel = "Restoring from trash....";
		merged.confirmMessage = "Restore these objects from the Trash?";
		merged.confirmAnchor = element;
		
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};

	RemoveBatchFromTrashButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (resultObject.isSelected() && resultObject.isEnabled() && $.inArray("Deleted", resultObject.getMetadata().status) != -1) {
				targetIds.push(resultObject.getPid());
			}
		}
		return targetIds;
	};
	
	RemoveBatchFromTrashButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
			var trashButton = new MoveObjectToTrashButton({
				pid : resultObject.pid,
				parentObject : resultObject,
				metadata : resultObject.metadata,
				confirm : false,
				moveToTrash: false
			});
			trashButton.activate();
		}
		this.enable();
	};
	
	return RemoveBatchFromTrashButton;
});