define('DeleteBatchButton', [ 'jquery', 'BatchActionButton'], function($, BatchActionButton) {
	function DeleteBatchButton(options, element) {
		this._create(options, element);
	};
	
	DeleteBatchButton.prototype.constructor = DeleteBatchButton;
	DeleteBatchButton.prototype = Object.create( BatchActionButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
		confirm: true
	};
	
	DeleteBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		
		merged.workLabel = "Deleting...";
		merged.followupLabel = "Deleting....";
		merged.confirm = {
			confirmAnchor : element
		};
		
		BatchActionButton.prototype._create.call(this, merged, element);
	};
	
	DeleteBatchButton.prototype.activate = function() {
		if (this.options.disabled)
			return;
		
		this.targetIds = this.getTargetIds();
		
		if (this.targetIds.length == 1)	
			this.options.confirm.promptText = "Mark the selected object as deleted?";
		else
			this.options.confirm.promptText = "Mark " + this.targetIds.length + " selected objects as deleted?";
		
		BatchActionButton.prototype.activate.call(this);
	}
	
	DeleteBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled() 
					&& $.inArray("Active", resultObject.getMetadata().status) != -1;
	};
	
	DeleteBatchButton.prototype.doWork = function() {
		this.disable();
	
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