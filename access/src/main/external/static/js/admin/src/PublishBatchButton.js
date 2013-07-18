define('PublishBatchButton', [ 'jquery', 'jquery-ui', 'BatchCallbackButton' ], function($) {
	$.widget("cdr.publishBatchButton", $.cdr.batchCallbackButton, {
		options : {
			resultObjectList : undefined,
			workPath: "services/rest/edit/publish",
			childWorkLinkName : 'publish'
		},

		getTargetIds : function() {
			var targetIds = [];
			for (var id in this.options.resultObjectList.resultObjects) {
				var resultObject = this.options.resultObjectList.resultObjects[id];
				if (resultObject.isSelected() && $.inArray("Unpublished", resultObject.getMetadata().status) != -1
						&& resultObject.isEnabled()) {
					targetIds.push(resultObject.getPid());
				}
			}
			return targetIds;
		}
	});
});