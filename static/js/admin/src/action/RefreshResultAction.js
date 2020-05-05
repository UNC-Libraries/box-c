define('RefreshResultAction', ['jquery', 'RemoteStateChangeMonitor'], function($, RemoteStateChangeMonitor) {
	function RefreshResultAction(context) {
		// target - ResultObject or array of ResultObjects to refresh
		// waitForUpdate - If true, updating will be delayed until the _version_ field stored by the server no longer matches the one in the page state.
		// afterUpdate - function to perform after the expected update completes
		// clearOverlay - if true, result entry overlay will be removed when the task is complete
		// statusText - message to display while refreshing. Default is "Refreshing..."
		this.context = context;
	};
	
	RefreshResultAction.prototype.execute = function() {
		var target = this.context.target, self = this;
		if (target instanceof Array) {
			for (var i in target) {
				this.refreshObject(target[i]);
			}
		} else
			this.refreshObject(target);
	};
	
	RefreshResultAction.prototype.refreshObject = function(resultObject) {
		resultObject.updateOverlay('open');
		if (this.context.statusText === undefined) {
			resultObject.setStatusText('Refreshing...');
		} else {
			resultObject.setStatusText(this.context.statusText);
		}
		
		if (this.context.waitForUpdate) {
			this.refreshAfterUpdate(resultObject);
		} else {
			this.refreshData(resultObject);
		}
	};
	
	RefreshResultAction.prototype.refreshAfterUpdate = function(resultObject) {
		var self = this;
		var followupMonitor = new RemoteStateChangeMonitor({
			'checkStatus' : function(data) {
				return (data != resultObject.metadata._version_);
			},
			'checkStatusTarget' : this,
			'statusChanged' : function(data) {
				if (self.context.afterUpdate !== undefined) {
					self.context.afterUpdate.call(self, resultObject);
				} else {
					self.refreshData(resultObject);
				}
			},
			'statusChangedTarget' : this, 
			'checkStatusAjax' : {
				url : "/services/api/status/item/" + resultObject.pid + "/solrRecord/version",
				dataType : 'json'
			},
			maxAttempts : this.context.maxAttempts? this.context.maxAttempts : 0
		});
	
		followupMonitor.performPing();
	};
	
	RefreshResultAction.prototype.refreshData = function(resultObject) {
		var self = this;
		
		$.ajax({
			url : self.getDataUrl(resultObject),
			dataType : 'json',
			success : function(data, textStatus, jqXHR) {
				resultObject.init(data);
				if (resultObject.overlay)
					resultObject.overlay.element = resultObject.element;
				if (self.context.clearOverlay === undefined || self.context.clearOverlay)
					resultObject.updateOverlay("close");
				self.context.resultTable.selectionUpdated();
			},
			error : function(a, b, c) {
				if (self.context.clearOverlay === undefined || self.context.clearOverlay)
					resultObject.updateOverlay("close");
				console.log(c);
			}
		});
	};
	
	RefreshResultAction.prototype.getDataUrl = function(resultObject) {
		return "entry/" + resultObject.pid;
	};
	
	return RefreshResultAction;
});