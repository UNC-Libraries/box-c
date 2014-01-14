define('DeleteResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function DeleteResultAction(options) {
		this._create(options);
	};
	
	DeleteResultAction.prototype.constructor = DeleteResultAction;
	DeleteResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
		
	DeleteResultAction.prototype._create = function(context) {
		this.context = context;
		
		this.options = {
			workMethod: $.post,
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
			animateSpeed: 'fast',
			workDone: DeleteResultAction.prototype.moveWorkDone,
			followup: DeleteResultAction.prototype.moveFollowup
		};
		
		this._configure();
		
		AjaxCallbackAction.prototype._create.call(this, this.options);
	};
	
	DeleteResultAction.prototype._configure = function() {
		this.options.workPath = "/services/api/edit/delete/{idPath}";
		this.options.workLabel = "Deleting object...";
		this.options.followupLabel = "Deleting object....";
		if ('confirm' in this.context) {
			this.options.confirm = this.context.confirm;
		} else {
			this.options.confirm = true;
		}
		this.options.confirmMessage = "Mark this object for deletion?";
	};
	
	DeleteResultAction.prototype.moveWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "Failed to move object for " + (this.options.target.metadata? 
						this.options.target.metadata.title : this.target.pid);
			}
		} else {
			jsonData = data;
		}
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	
	DeleteResultAction.prototype.moveFollowup = function(data) {
		if (data) {
			return this.context.target.updateVersion(data);
		}
		return false;
	};
	
	DeleteResultAction.prototype.completeState = function() {
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target
		});
		this.alertHandler.alertHandler("success", "Marked item " + this.context.target.metadata.title 
				+ " (" + this.context.target.metadata.id + ") for deletion");
		this.context.target.enable();
	};

	return DeleteResultAction;
});