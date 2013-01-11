define([ 'jquery', 'jquery-ui', 'BatchCallbackButton' ], function($) {
	$.widget("cdr.publishBatchButton", $.cdr.batchCallbackButton, {
		options : {
			resultObjectList : undefined,
			workPath: "services/rest/edit/publish",
			childCallbackButtonSelector : ":cdr-publishObjectButton"
		},

		getTargetIds : function() {
			var targetIds = [];
			for (var id in this.options.resultObjectList.resultObjects) {
				var resultObject = this.options.resultObjectList.resultObjects[id];
				if (resultObject.resultObject("isSelected") && !resultObject.resultObject("getMetadata").isPublished()) {
					targetIds.push(resultObject.resultObject("getPid").getPid());
				}
			}
			return targetIds;
		},

		completeResult : function(id) {
			for (var index in this.targetIds) {
				this.options.resultObjectList.resultObjects[this.targetIds[index]].resultObject("publish");
			}
			this.targetIds = null;
		}
	});
});