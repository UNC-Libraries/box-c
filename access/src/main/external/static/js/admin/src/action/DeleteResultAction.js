define('DeleteResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function DeleteResultAction(options) {
		this._create(options);
	};
	
	DeleteResultAction.prototype.constructor = DeleteResultAction;
	DeleteResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	DeleteResultAction.prototype.actionName = "Delete";
		
	DeleteResultAction.prototype._create = function(context) {
		this.context = context;
		
		this.options = {
			workMethod: $.post,
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version"
		};
		
		this._configure();
		
		AjaxCallbackAction.prototype._create.call(this, this.options);
	};
	
	DeleteResultAction.prototype._configure = function() {
		this.options.workPath = "/services/api/edit/delete/{idPath}";
		this.options.workLabel = "Deleting object...";
		this.options.followupLabel = "Deleting object....";
		
		if ('confirm' in this.context && !this.context.confirm) {
			this.options.confirm = false;
		} else {
			this.options.confirm = {
				promptText : "Mark this object for deletion?",
				confirmAnchor : this.context.confirmAnchor
			};
		}
	};
	
	DeleteResultAction.prototype.followup = function(data) {
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