define('RemoveBatchFromTrashButton', [ 'jquery', 'BatchCallbackButton'], function($, BatchCallbackButton) {
	function RemoveBatchFromTrashButton(options, element) {
		this._create(options, element);
	};
	
	RemoveBatchFromTrashButton.prototype.constructor = RemoveBatchFromTrashButton;
	RemoveBatchFromTrashButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
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

	
	RemoveBatchFromTrashButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled() 
					&& $.inArray("Deleted", resultObject.getMetadata().status) != -1;
	};
	
	RemoveBatchFromTrashButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			this.actionHandler.addEvent({
				action : 'RestoreResult',
				target : this.options.resultObjectList.resultObjects[this.targetIds[index]],
				confirm : false
			});
		}
		this.enable();
	};
	
	return RemoveBatchFromTrashButton;
});