define('RefreshResultAction', ['jquery', 'RemoteStateChangeMonitor'], function($, RemoteStateChangeMonitor) {
	function RefreshResultAction(context) {
		// target - ResultObject or array of ResultObjects to refresh
		// immediately - if false, then refreshing will wait until the _version of the entry has changed server side before updating
		// clearOverlay - if true, result entry overlay will be removed when the task is complete
		this.context = context;
	};
	
	RefreshResultAction.prototype.execute = function() {
		var target = this.context.target, self = this;
		if (target instanceof Array) {
			for (var i in target) {
				this.refreshObject(target[i]);
			}
		} else
			this.refreshObject(target[i]);
	};
	
	RefreshResultAction.prototype.refreshObject = function(resultObject) {
		var self = this;
		
		resultObject.updateOverlay('open');
		resultObject.setStatusText('Refreshing...');
		
		if (this.context.immediately) {
			this.refreshData(true);
			return;
		}
		
		var followupMonitor = new RemoteStateChangeMonitor({
			'checkStatus' : function(data) {
				return (data != resultObject.metadata._version_);
			},
			'checkStatusTarget' : this,
			'statusChanged' : function(data) {
				self.refreshData(resultObject);
			},
			'statusChangedTarget' : this, 
			'checkStatusAjax' : {
				url : "/services/api/status/item/" + resultObject.pid + "/solrRecord/version",
				dataType : 'json'
			}
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
				self.context.resultTable._selectionUpdated();
			},
			error : function(a, b, c) {
				if (self.context.clearOverlay === undefined || self.context.clearOverlay)
					resultObject.updateOverlay("close");
				console.log(c);
			}
		});
	};
	
	RefreshResultAction.prototype.getDataUrl = function(resultObject) {
		return resultObject.options.resultObjectList.options.refreshEntryUrl + resultObject.pid;
	};
	
	return RefreshResultAction;
});