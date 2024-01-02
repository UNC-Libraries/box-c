define('ClearPrimaryObjectResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {

	function ClearPrimaryObjectResultAction(context) {
		this._create(context);
	};
	
	ClearPrimaryObjectResultAction.prototype.constructor = ClearPrimaryObjectResultAction;
	ClearPrimaryObjectResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	ClearPrimaryObjectResultAction.prototype._create = function(context) {
		this.context = context;
		
		var options = {
			workMethod: "PUT",
			workPath: "/services/api/edit/clearPrimaryObject/{idPath}",
			workLabel: "Clearing primary object...",
			followupLabel: "Clearing primary object...",
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version"
		}
		
		if ('confirm' in this.context && !this.context.confirm) {
			options.confirm = false;
		} else {
			options.confirm = {
				promptText : "Clear the primary object?",
				confirmAnchor : this.context.confirmAnchor
			};
		}
		
		AjaxCallbackAction.prototype._create.call(this, options);
	};
	
	ClearPrimaryObjectResultAction.prototype.completeState = function() {
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target
		});
		this.alertHandler.alertHandler("success", "Cleared the primary object assignment.");
		this.context.target.enable();
	};

	ClearPrimaryObjectResultAction.prototype.followup = function(data) {
		if (data) {
			return this.context.target.updateVersion(data);
		}
		return false;
	};
	
	return ClearPrimaryObjectResultAction;
});