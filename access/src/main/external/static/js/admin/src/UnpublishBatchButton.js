define('UnpublishBatchButton', [ 'jquery', 'BatchCallbackButton'], function($, BatchCallbackButton) {
	function UnpublishBatchButton(options, element) {
		this._create(options, element);
	};
	
	UnpublishBatchButton.prototype.constructor = UnpublishBatchButton;
	UnpublishBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
			resultObjectList : undefined
		};
	
	UnpublishBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};
	
	UnpublishBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && $.inArray("Unpublished", resultObject.getMetadata().status) == -1
					&& resultObject.isEnabled();
	};

	UnpublishBatchButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			this.actionHandler.addEvent({
				action : 'Unpublish',
				target : this.options.resultObjectList.resultObjects[this.targetIds[index]]
			});
		}
		this.enable();
	};

	return UnpublishBatchButton;
});