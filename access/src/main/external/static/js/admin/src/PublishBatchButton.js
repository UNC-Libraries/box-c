define('PublishBatchButton', [ 'jquery', 'BatchCallbackButton'], function($, BatchCallbackButton) {
	
	function PublishBatchButton(options, element) {
		this._create(options, element);
	};
	
	PublishBatchButton.prototype.constructor = PublishBatchButton;
	PublishBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined
	};
	
	PublishBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workFunction = this.resultObjectWorkFunction;
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};
	
	PublishBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && $.inArray("Unpublished", resultObject.getMetadata().status) != -1
					&& resultObject.isEnabled();
	};
	
	PublishBatchButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			this.actionHandler.addEvent({
				action : 'Publish',
				target : this.options.resultObjectList.resultObjects[this.targetIds[index]]
			});
		}
		this.enable();
	};
	
	return PublishBatchButton;
});