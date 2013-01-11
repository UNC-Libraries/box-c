define([ 'jquery', 'jquery-ui', 'AjaxCallbackButton', 'ResultObjectList' ], function($, ui, ResultObjectList) {
	$.widget("cdr.batchCallbackButton", $.cdr.ajaxCallbackButton, {
		options : {
			resultObjectList : undefined,
			followupPath: "services/rest/item/solrRecord/version",
			childCallbackButtonSelector : undefined
		},

		_create : function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);

			this.options.workDone = this.workDone;
			this.options.followup = this.followup;
			this.options.complete = this.completeResult;
			this.options.completeTarget = this;
		},

		doWork : function() {
			this.disable();
			this.targetIds = this.getTargetIds();
			
			for (var index in this.targetIds) {
				var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
				resultObject.resultObject("disable");
				if (this.options.childCallbackButtonSelector) {
					var childButton = resultObject.find(this.options.childCallbackButtonSelector);
					childButton[childButton.data("callbackButtonClass")].call(childButton, "workState");
				}
			}
			
			var self = this;
			this.performWork($.post, {
				'ids' : self.targetIds.join('\n')
			});
		},

		workDone : function(data) {
			if ($.isArray(data)) {
				this.followupObjects = [];
				for (var index in data) {
					var id = data[index].pid.pid;
					this.followupObjects.push(id);
					
					if (this.options.childCallbackButtonSelector) {
						var resultObject = this.options.resultObjectList.resultObjects[id];
						var childButton = resultObject.find(this.options.childCallbackButtonSelector);
						childButton[childButton.data("callbackButtonClass")].call(childButton, "followupState");
					}
				}
				return true;
			} else
				alert("Error while attempting to perform action: " + data);
			return false;
		},

		followupPing : function() {
			this.performFollowupPing($.post, {
				'ids' : this.followupObjects.join('\n')
			});
		},

		followup : function(data) {
			for (var id in data) {
				if (this.options.resultObjectList.resultObjects[id].resultObject("updateVersion", data[id])) {
					var index = $.inArray(id, this.followupObjects);
					if (index != -1) {
						this.followupObjects.splice(index, 1);
						
						// Trigger the complete function on targeted child callback buttons
						if (this.options.childCallbackButtonSelector) {
							var resultObject = this.options.resultObjectList.resultObjects[id];
							var childButton = resultObject.find(this.options.childCallbackButtonSelector);
							childButton[childButton.data("callbackButtonClass")].call(childButton, "options").complete.call(childButton);
						}
					}
				}
			}
			return this.followupObjects.length == 0;
		},
		
		completeResult : function(id) {
			this.targetIds = null;
			this.enable();
		},

		getTargetIds : function() {
			var targetIds = [];

			$.each(this.options.resultObjects, function() {
				var resultObject = this;
				if (this.isSelected()) {
					targetIds.push(resultObject.resultObject("getPid").getPid());
				}
			});

			return targetIds;
		}
	});
});
