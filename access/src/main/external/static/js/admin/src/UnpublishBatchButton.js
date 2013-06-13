define('UnpublishBatchButton', [ 'jquery', 'jquery-ui', 'BatchCallbackButton' ], function($) {
	$.widget("cdr.unpublishBatchButton", $.cdr.batchCallbackButton, {
		options : {
			resultObjectList : undefined,
			workPath: "services/rest/edit/unpublish",
			childWorkLinkName : 'publish'
		},

		getTargetIds : function() {
			var targetIds = [];
			for (var id in this.options.resultObjectList.resultObjects) {
				var resultObject = this.options.resultObjectList.resultObjects[id];
				if (resultObject.isSelected() && resultObject.isPublished()
						&& resultObject.isEnabled()) {
					targetIds.push(resultObject.getPid());
				}
			}
			return targetIds;
		}
	});
});