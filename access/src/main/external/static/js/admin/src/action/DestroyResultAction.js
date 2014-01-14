define('DestroyResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function DestroyResultAction(context) {
		this._create(context);
	};
	
	DestroyResultAction.prototype.constructor = DestroyResultAction;
	DestroyResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	
		
	DestroyResultAction.prototype._create = function(context) {
		this.context = context;
		
		var options = {
			workMethod: $.post,
			workLabel: "Destroying...",
			workPath: "/services/api/edit/destroy/{idPath}",
			followupLabel: "Cleaning up...",
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
			confirm: true,
			confirmMessage: "Permanently destroy this object?  This action cannot be undone.",
			animateSpeed: 'fast',
			workDone: DestroyResultAction.prototype.destroyWorkDone,
			followup: DestroyResultAction.prototype.destroyFollowup
		};
		
		AjaxCallbackAction.prototype._create.call(this, options);
	};

	DestroyResultAction.prototype.destroyFollowup = function(data) {
		if (data == null) {
			return true;
		}
		return false;
	};

	DestroyResultAction.prototype.completeState = function() {
		if (this.context.target != null) {
			if (this.context.target.metadata)
				this.alertHandler.alertHandler("success", "Permanently destroyed item " 
						+ this.context.target.metadata.title + " (" + this.context.target.metadata.id + ")");
			else this.alertHandler.alertHandler("success", "Permanently destroyed item " + data);
			this.context.target.deleteElement();			
		}
	};

	DestroyResultAction.prototype.destroyWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "An error occurred while attempting to destroy object " + this.context.target.pid;
			}
		} else jsonData = data;
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	return DestroyResultAction;
});