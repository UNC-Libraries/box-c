define([ 'jquery', 'jquery-ui', 'AjaxCallbackButton', 'ResultObjectList' ], function($, ui, ResultObjectList) {
	$.widget("cdr.batchCallbackButton", $.cdr.ajaxCallbackButton, {
		options : {
			resultObjectList : undefined,
			followupPath: "services/rest/item/solrRecord/version",
			childWorkLinkName : undefined,
			workFunction : undefined,
			followupFunction : undefined,
			completeFunction : undefined 
		},

		_create : function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);

			this.options.workDone = this.workDone;
			this.options.followup = this.followup;
			this.options.completeTarget = this;
		},
		
		_init : function() {
			$.cdr.ajaxCallbackButton.prototype._init.apply(this, arguments);
			
			this.followupMonitor.options.checkStatusAjax.type = 'POST';
		},

		doWork : function() {
			this.disable();
			this.targetIds = this.getTargetIds();
			
			for (var index in this.targetIds) {
				var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
				resultObject.resultObject("disable");
				if (this.options.workFunction)
					if ($.isFunction(this.options.workFunction))
						this.options.workFunction.call(resultObject);
					else
						resultObject.resultObject(this.options.workFunction);
			}
			
			var self = this;
			if (this.targetIds.length > 0) {
				this.performWork($.post, {
					'ids' : self.targetIds.join('\n')
				});
			} else {
				this.enable();
			}
		},

		workDone : function(data) {
			if ($.isArray(data)) {
				this.followupObjects = [];
				for (var index in data) {
					var id = data[index].pid;
					this.followupObjects.push(id);
					if (this.options.workFunction) {
						var resultObject = this.options.resultObjectList.resultObjects[id];
						if ($.isFunction(this.options.followupFunction))
							this.options.followupFunction.call(resultObject);
						else
							resultObject.resultObject(this.options.followupFunction);
					}
				}
				this.followupMonitor.pingData = {
						'ids' : this.followupObjects.join('\n')
				}; 
				return true;
			} else
				alert("Error while attempting to perform action: " + data);
			return false;
		},

		followup : function(data) {
			for (var id in data) {
				if (this.options.resultObjectList.resultObjects[id].resultObject("updateVersion", data[id])) {
					var index = $.inArray(id, this.followupObjects);
					if (index != -1) {
						this.followupObjects.splice(index, 1);
						
						var resultObject = this.options.resultObjectList.resultObjects[id];
						resultObject.resultObject("setState", "idle");
						
						if (this.options.completeFunction) {
							if ($.isFunction(this.options.completeFunction))
								this.options.completeFunction.call(resultObject);
							else
								resultObject.resultObject(this.options.completeFunction);
						}
					}
				}
			}
			this.followupMonitor.pingData = {
					'ids' : this.followupObjects.join('\n')
			}; 
			return this.followupObjects.length == 0;
		},
		
		completeState : function(id) {
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
