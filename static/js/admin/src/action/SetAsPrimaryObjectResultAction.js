define('SetAsPrimaryObjectResultAction', [ 'jquery', 'dompurify', 'AjaxCallbackAction'], function($, DomPurify, AjaxCallbackAction) {

	function SetAsPrimaryObjectResultAction(context) {
		this._create(context);
	};
	
	SetAsPrimaryObjectResultAction.prototype.constructor = SetAsPrimaryObjectResultAction;
	SetAsPrimaryObjectResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	SetAsPrimaryObjectResultAction.prototype._create = function(context) {
		this.context = context;
		
		var options = {
			workMethod: "PUT",
			workPath: "/services/api/edit/setAsPrimaryObject/{idPath}",
			workLabel: "Setting as primary object...",
			followupLabel: "Setting as primary object...",
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version"
		}
		
		if ('confirm' in this.context && !this.context.confirm) {
			options.confirm = false;
		} else {
			options.confirm = {
				promptText : "Use this as the primary object for its parent?",
				confirmAnchor : this.context.confirmAnchor
			};
		}
		
		AjaxCallbackAction.prototype._create.call(this, options);
	};
	
	SetAsPrimaryObjectResultAction.prototype.completeState = function() {
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target
		});
		this.alertHandler.alertHandler("success", "Assignment of object \"" + DomPurify.sanitize(this.context.target.metadata.title, { ALLOWED_TAGS: ['#text'] }) + "\" as primary object has completed.");
		this.context.target.enable();
	};

	SetAsPrimaryObjectResultAction.prototype.followup = function(data) {
		if (data) {
			return this.context.target.updateVersion(data);
		}
		return false;
	};
	
	return SetAsPrimaryObjectResultAction;
});