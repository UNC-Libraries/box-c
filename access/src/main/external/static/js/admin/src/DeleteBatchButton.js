define('DeleteBatchButton', [ 'jquery', 'BatchCallbackButton', 'DeleteObjectButton'], function($, BatchCallbackButton, DeleteObjectButton) {
	function DeleteBatchButton(options, element) {
		this._create(options, element);
	};
	
	DeleteBatchButton.prototype.constructor = DeleteBatchButton;
	DeleteBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		confirm: true,
		confirmMessage: "Permanently delete selected object(s)?  This action cannot be undone."
	};
	
	DeleteBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		merged.confirmAnchor = element;
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};
	
	DeleteBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled();
	};
	
	DeleteBatchButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
			var deleteButton = new DeleteObjectButton({
				pid : resultObject.pid,
				parentObject : resultObject,
				metadata : resultObject.metadata,
				confirm : false
			});
			deleteButton.activate();
		}
		this.enable();
	};
	
	return DeleteBatchButton;
});