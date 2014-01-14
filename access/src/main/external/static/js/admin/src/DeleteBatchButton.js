define('DeleteBatchButton', [ 'jquery', 'BatchActionButton'], function($, BatchActionButton) {
	function DeleteBatchButton(options, element) {
		this._create(options, element);
	};
	
	DeleteBatchButton.prototype.constructor = DeleteBatchButton;
	DeleteBatchButton.prototype = Object.create( BatchActionButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
		confirm: true,
		animateSpeed: 'fast'
	};
	
	DeleteBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		
		merged.workLabel = "Deleting...";
		merged.followupLabel = "Deleting....";
		merged.confirmMessage = "Mark these objects as deleted?";
		merged.confirmAnchor = element;
		
		BatchActionButton.prototype._create.call(this, merged, element);
	};
	
	DeleteBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled() 
					&& $.inArray("Deleted", resultObject.getMetadata().status) == -1;
	};
	
	DeleteBatchButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			this.actionHandler.addEvent({
				action : 'DeleteResult',
				target : this.options.resultObjectList.resultObjects[this.targetIds[index]],
				confirm : false
			});
		}
		this.enable();
	};
	
	return DeleteBatchButton;
});