define('DeleteObjectButton', [ 'jquery', 'AjaxCallbackButton'], function($, AjaxCallbackButton) {
	function DeleteObjectButton(options) {
		this._create(options);
	};
	
	DeleteObjectButton.prototype.constructor = DeleteObjectButton;
	DeleteObjectButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			workLabel: "Deleting forever...",
			workPath: "delete/{idPath}",
			followupLabel: "Cleaning up...",
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
			confirm: true,
			confirmMessage: "Permanently delete this object?  This action cannot be undone",
			animateSpeed: 'fast',
			workDone: DeleteObjectButton.prototype.deleteWorkDone,
			followup: DeleteObjectButton.prototype.deleteFollowup,
			complete: DeleteObjectButton.prototype.complete
		};
		
	DeleteObjectButton.prototype._create = function(options) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workDone = DeleteObjectButton.prototype.deleteWorkDone;
		merged.followup = DeleteObjectButton.prototype.deleteFollowup;
		merged.complete = DeleteObjectButton.prototype.complete;
		AjaxCallbackButton.prototype._create.call(this, merged);
		
		if (this.options.parentObject)
			this.options.confirmAnchor = this.options.parentObject.element;
	};

	DeleteObjectButton.prototype.deleteFollowup = function(data) {
		if (data == null) {
			return true;
		}
		return false;
	};
	
	DeleteObjectButton.prototype.complete = function() {
		if (this.options.metadata)
			this.alertHandler.alertHandler("success", "Permanently deleted item " + metadata.title + " (" + metadata.id + ")");
		else this.alertHandler.alertHandler("success", "Permanently deleted item " + data);
	};

	DeleteObjectButton.prototype.completeState = function() {
		if (this.options.parentObject != null)
			this.options.parentObject.deleteElement();
		this.destroy();
	};

	DeleteObjectButton.prototype.deleteWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "An error occurred while attempting to delete object " + this.pid;
			}
		} else jsonData = data;
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	return DeleteObjectButton;
});