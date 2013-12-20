define('RestoreResultAction', [ 'jquery', 'AjaxCallbackAction', 'TrashResultAction'], function($, AjaxCallbackAction, TrashResultAction) {
	function RestoreResultAction(options) {
		this._create(options);
	};
	
	RestoreResultAction.prototype.constructor = RestoreResultAction;
	RestoreResultAction.prototype = Object.create( TrashResultAction.prototype );
	
	RestoreResultAction.prototype._configure = function() {
		this.options.workPath = "/services/api/edit/removeFromTrash/{idPath}";
		this.options.workLabel = "Restoring object...";
		this.options.followupLabel = "Restoring object....";
		if ('confirm' in this.context) {
			this.options.confirm = this.context.confirm;
		} else {
			this.options.confirm = false;
		}
	};
	
	RestoreResultAction.prototype.completeState = function() {
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target
		});
		this.alertHandler.alertHandler("success", "Restored item " + this.options.parentObject.metadata.title 
				+ " (" + this.options.parentObject.metadata.id + ") from the Trash");
		this.context.target.enable();
	};

	return RestoreResultAction;
});