define('MoveObjectToTrashButton', [ 'jquery', 'AjaxCallbackButton'], function($, AjaxCallbackButton) {
	function MoveObjectToTrashButton(options) {
		this._create(options);
	};
	
	MoveObjectToTrashButton.prototype.constructor = MoveObjectToTrashButton;
	MoveObjectToTrashButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			moveToTrash: true,
			showTrash: true,
			workMethod: $.post,
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
			animateSpeed: 'fast'
		};
		
	MoveObjectToTrashButton.prototype._create = function(options) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workDone = MoveObjectToTrashButton.prototype.moveWorkDone;
		merged.followup = MoveObjectToTrashButton.prototype.moveFollowup;
		this.options = merged;
		if (this.options.moveToTrash) {
			this.moveToTrashState();
		} else {
			this.removeFromTrashState();
		}
		if (this.options.parentObject)
			this.options.confirmAnchor = this.options.parentObject.element;
		
		AjaxCallbackButton.prototype._create.call(this, merged);
	};
	
	MoveObjectToTrashButton.prototype.removeFromTrashState = function() {
		this.options.workPath = "/services/api/edit/removeFromTrash/{idPath}";
		this.options.workLabel = "Restoring object...";
		this.options.followupLabel = "Restoring object....";
		if (!('confirm' in this.options))
			this.options.confirm = false;
	};

	MoveObjectToTrashButton.prototype.moveToTrashState = function() {
		this.options.workPath = "/services/api/edit/moveToTrash/{idPath}";
		this.options.workLabel = "Deleting object...";
		this.options.followupLabel = "Deleting object....";
		if (!('confirm' in this.options))
			this.options.confirm =  true;
		this.options.confirmMessage = "Mark this object for deletion?";
	};
	
	MoveObjectToTrashButton.prototype.moveWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "Failed to move object for " + (this.options.metadata? this.options.metadata.title : this.pid);
			}
		} else {
			jsonData = data;
		}
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	
	MoveObjectToTrashButton.prototype.moveFollowup = function(data) {
		if (data) {
			return this.options.parentObject.updateVersion(data);
		}
		return false;
	};
	
	MoveObjectToTrashButton.prototype.completeState = function() {
		if (this.options.parentObject) {
			this.options.parentObject.refresh(true);
			if (this.options.moveToTrash) {
				this.alertHandler.alertHandler("success", "Marked item " + this.options.parentObject.metadata.title 
						+ " (" + this.options.parentObject.metadata.id + ") for deletion");
				this.options.parentObject.enable();
			} else {
				this.alertHandler.alertHandler("success", "Restored item " + this.options.parentObject.metadata.title 
						+ " (" + this.options.parentObject.metadata.id + ") from the Trash");
				this.options.parentObject.enable();
			}
		}
		
		this.enable();
	};

	return MoveObjectToTrashButton;
});