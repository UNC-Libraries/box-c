define('RestoreBatchButton', [ 'jquery', 'BatchActionButton'], function($, BatchActionButton) {
	function RestoreBatchButton(options, element) {
		this._create(options, element);
	};
	
	RestoreBatchButton.prototype.constructor = RestoreBatchButton;
	RestoreBatchButton.prototype = Object.create( BatchActionButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined
	};
	
	RestoreBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		
		merged.workLabel = "Restoring from trash...";
		merged.followupLabel = "Restoring from trash....";
		merged.confirm = {
			confirmAnchor : element
		};
		
		BatchActionButton.prototype._create.call(this, merged, element);
	};
	
	RestoreBatchButton.prototype.activate = function() {
		if (this.options.disabled)
			return;
		
		this.targetIds = this.getTargetIds();
		
		if (this.targetIds.length == 1)	
			this.options.confirm.promptText = "Restore the selected object from the trash?";
		else
			this.options.confirm.promptText = "Restore " + this.targetIds.length + " selected objects from the trash?";
		
		BatchActionButton.prototype.activate.call(this);
	}

	
	RestoreBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled() 
					&& $.inArray("Deleted", resultObject.getMetadata().status) != -1;
	};
	
	RestoreBatchButton.prototype.doWork = function() {
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
	
	return RestoreBatchButton;
});