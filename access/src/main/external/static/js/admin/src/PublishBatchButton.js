define('PublishBatchButton', [ 'jquery', 'BatchCallbackButton', 'PublishObjectButton'], function($, BatchCallbackButton, PublishObjectButton) {
	
	function PublishBatchButton(options, element) {
		this._create(options, element);
	};
	
	PublishBatchButton.prototype.constructor = PublishBatchButton;
	PublishBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
		workPath: "/services/api/edit/publish",
		childWorkLinkName : 'publish'
	};
	
	PublishBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workFunction = this.resultObjectWorkFunction;
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};

	PublishBatchButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (resultObject.isSelected() && $.inArray("Unpublished", resultObject.getMetadata().status) != -1
					&& resultObject.isEnabled()) {
				targetIds.push(resultObject.getPid());
			}
		}
		return targetIds;
	};
	
	PublishBatchButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
			var publishButton = new PublishObjectButton({
				pid : resultObject.pid,
				parentObject : resultObject,
				defaultPublish : false,
				metadata : resultObject.metadata
			});
			publishButton.activate();
		}
		this.enable();
	};
	
	return PublishBatchButton;
});