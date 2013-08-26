define('UnpublishBatchButton', [ 'jquery', 'BatchCallbackButton' ], function($, BatchCallbackButton) {
	function UnpublishBatchButton(options, element) {
		this._create(options, element);
	};
	
	UnpublishBatchButton.prototype.constructor = UnpublishBatchButton;
	UnpublishBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
			resultObjectList : undefined,
			workPath: "services/rest/edit/unpublish",
			childWorkLinkName : 'publish'
		};
	
	UnpublishBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};

	UnpublishBatchButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (resultObject.isSelected() && $.inArray("Unpublished", resultObject.getMetadata().status) == -1
					&& resultObject.isEnabled()) {
				targetIds.push(resultObject.getPid());
			}
		}
		return targetIds;
	};
	return UnpublishBatchButton;
});