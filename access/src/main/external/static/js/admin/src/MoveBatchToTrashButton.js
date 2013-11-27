define('MoveBatchToTrashButton', [ 'jquery', 'BatchCallbackButton', 'MoveObjectToTrashButton'], function($, BatchCallbackButton, MoveObjectToTrashButton) {
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
		
		merged.workPath = "/services/api/edit/moveToTrash";
		merged.workLabel = "Deleting...";
		merged.followupLabel = "Deleting....";
		merged.confirmMessage = "Mark these objects as deleted?";
		merged.confirmAnchor = element;
		
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};
	
	MoveBatchToTrashButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled() 
					&& $.inArray("Deleted", resultObject.getMetadata().status) == -1;
	};
	
	MoveBatchToTrashButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
			var trashButton = new MoveObjectToTrashButton({
				pid : resultObject.pid,
				parentObject : resultObject,
				metadata : resultObject.metadata,
				confirm : false,
				moveToTrash: true
			});
			trashButton.activate();
		}
		this.enable();
	};
	
	return MoveBatchToTrashButton;
});