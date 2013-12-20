define('DeleteForeverAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function DeleteForeverAction(context) {
		this._create(context);
	};
	
	DeleteForeverAction.prototype.constructor = DeleteForeverAction;
	DeleteForeverAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	
		
	DeleteForeverAction.prototype._create = function(context) {
		this.context = context;
		
		var options = {
			workLabel: "Deleting forever...",
			workPath: "delete/{idPath}",
			followupLabel: "Cleaning up...",
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
			confirm: true,
			confirmMessage: "Permanently delete this object?  This action cannot be undone",
			animateSpeed: 'fast',
			workDone: DeleteForeverAction.prototype.deleteWorkDone,
			followup: DeleteForeverAction.prototype.deleteFollowup,
			complete: DeleteForeverAction.prototype.complete
		};
		
		AjaxCallbackAction.prototype._create.call(this, options);
	};

	DeleteForeverAction.prototype.deleteFollowup = function(data) {
		if (data == null) {
			return true;
		}
		return false;
	};
	
	DeleteForeverAction.prototype.complete = function() {
		if (this.context.target.metadata)
			this.alertHandler.alertHandler("success", "Permanently deleted item " 
					+ this.context.target.metadata.title + " (" + this.context.target.metadata.id + ")");
		else this.alertHandler.alertHandler("success", "Permanently deleted item " + data);
	};

	DeleteForeverAction.prototype.completeState = function() {
		if (this.context.target != null)
			this.context.target.deleteElement();
		this.destroy();
	};

	DeleteForeverAction.prototype.deleteWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "An error occurred while attempting to delete object " + this.context.target.pid;
			}
		} else jsonData = data;
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	return DeleteForeverAction;
});