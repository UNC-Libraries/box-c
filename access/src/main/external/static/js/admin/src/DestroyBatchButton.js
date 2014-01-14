define('DestroyBatchButton', ['jquery', 'BatchActionButton'], function($, BatchActionButton) {
	function DestroyBatchButton(options, element) {
		this._create(options, element);
	};
	
	DestroyBatchButton.prototype.constructor = DestroyBatchButton;
	DestroyBatchButton.prototype = Object.create( BatchActionButton.prototype );
	
	var defaultOptions = {
		confirm: true,
		confirmMessage: "Permanently destroy selected object(s)?  This action cannot be undone."
	};
	
	DestroyBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		merged.confirmAnchor = element;
		BatchActionButton.prototype._create.call(this, merged, element);
	};
	
	DestroyBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled();
	};
	
	DestroyBatchButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			this.actionHandler.addEvent({
				action : 'DestroyResult',
				target : this.options.resultObjectList.resultObjects[this.targetIds[index]],
				confirm : false
			});
		}
		this.enable();
	};
	
	return DestroyBatchButton;
});