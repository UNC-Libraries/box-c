define('BatchCallbackButton', [ 'jquery', 'AjaxCallbackButton', 'ResultObjectList' ], function($, AjaxCallbackButton, ResultObjectList) {
	function BatchCallbackButton(options, element) {
		this._create(options, element);
	};
	
	BatchCallbackButton.prototype.constructor = BatchCallbackButton;
	BatchCallbackButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			resultObjectList : undefined,
			followupPath: "/services/api/status/item/solrRecord/version",
			workFunction : undefined,
			followupFunction : undefined,
			completeFunction : undefined,
			animateSpeed: 'fast'
		};

	BatchCallbackButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workDone = this.workDone;
		merged.followup = this.followup;
		merged.completeTarget = this;
		AjaxCallbackButton.prototype._create.call(this, merged, element);
		this.followupMonitor.options.checkStatusAjax.type = 'POST';
		
		this.actionHandler = this.options.actionHandler;
	};

	BatchCallbackButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
		
		for (var index in this.targetIds) {
			var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
			resultObject.disable();
			if (this.options.workFunction)
				if ($.isFunction(this.options.workFunction))
					this.options.workFunction.call(resultObject);
				else
					resultObject[this.options.workFunction]();
		}
		
		var self = this;
		if (this.targetIds.length > 0) {
			this.performWork($.post, {
				'ids' : self.targetIds.join('\n')
			});
		} else {
			this.enable();
		}
	};

	BatchCallbackButton.prototype.workDone = function(data) {
		if ($.isArray(data)) {
			this.followupObjects = [];
			for (var index in data) {
				var id = data[index].pid;
				this.followupObjects.push(id);
				if (this.options.followupFunction) {
					var resultObject = this.options.resultObjectList.resultObjects[id];
					if ($.isFunction(this.options.followupFunction))
						this.options.followupFunction.call(resultObject);
					else
						resultObject[this.options.followupFunction]();
				}
			}
			this.followupMonitor.pingData = {
					'ids' : this.followupObjects.join('\n')
			}; 
			return true;
		} else
			alert("Error while attempting to perform action: " + data);
		return false;
	};

	BatchCallbackButton.prototype.followup = function(data) {
		for (var id in data) {
			if (this.options.resultObjectList.resultObjects[id].updateVersion(data[id])) {
				var index = $.inArray(id, this.followupObjects);
				if (index != -1) {
					this.followupObjects.splice(index, 1);
					
					var resultObject = this.options.resultObjectList.resultObjects[id];
					resultObject.setState("idle");
					
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
	};
	
	BatchCallbackButton.prototype.completeState = function(id) {
		this.targetIds = null;
		this.enable();
	};
		
	BatchCallbackButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (this.isValidTarget(resultObject))
				targetIds.push(resultObject.getPid());
		}
		return targetIds;
	};

	BatchCallbackButton.prototype.hasTargets = function() {
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (this.isValidTarget(resultObject))
				return true;
		}
		return false;
	};
	
	BatchCallbackButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected();
	};
	
	return BatchCallbackButton;
});
