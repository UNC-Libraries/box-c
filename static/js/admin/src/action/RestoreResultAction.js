define('RestoreResultAction', [ 'jquery', 'AjaxCallbackAction', 'DeleteResultAction'], function($, AjaxCallbackAction,DeleteResultAction) {
	function RestoreResultAction(options) {
		this._create(options);
	};
	
	RestoreResultAction.prototype.constructor = RestoreResultAction;
	RestoreResultAction.prototype = Object.create(DeleteResultAction.prototype );
	
	RestoreResultAction.prototype.actionName = "Restore";
	
	RestoreResultAction.prototype._configure = function() {
		this.options.workPath = "/services/api/edit/restore/{idPath}";
		this.options.workLabel = "Restoring object...";
		this.options.followupLabel = "Restoring object....";
		
		if (this.context.confirm) {
			this.options.confirm = {
				promptText : "Restore this object from the trash?",
				confirmAnchor : this.context.confirmAnchor
			};
		} else {
			this.options.confirm = false;
		}
	};
	
	RestoreResultAction.prototype.completeState = function() {
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target
		});
		this.alertHandler.alertHandler("success", "Restored item " + this.context.target.metadata.title 
				+ " (" + this.context.target.metadata.id + ") from the Trash");
		this.context.target.enable();
	};

	return RestoreResultAction;
});