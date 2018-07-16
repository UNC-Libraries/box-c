define('SetAsPrimaryObjectResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {

	function SetAsPrimaryObjectResultAction(context) {
		this._create(context);
		if (!this.resultList) {
			this.resultList = this.context.resultTable.resultObjectList;
		}
	};
	
	SetAsPrimaryObjectResultAction.prototype.constructor = SetAsPrimaryObjectResultAction;
	SetAsPrimaryObjectResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	SetAsPrimaryObjectResultAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("editResourceType", target.metadata.permissions) != -1
				&& (target.getMetadata().isPart);
	};
	
	SetAsPrimaryObjectResultAction.prototype.getTarget = function(target) {
		return this.context.target;
	};
	
	SetAsPrimaryObjectResultAction.prototype.execute = function() {
		var self = this;
		this.target = this.getTarget();
			
		this.context.confirm.dialogOptions = {
			title : "Set Primary Object",
			dialogClass : "confirm_dialog",
			width: "400px"
		};
			
		this.context.confirm.promptText = "Set the selected object as the primary object for its parent?";
	
		AjaxCallbackAction.prototype.execute.call(this);
	};
	
	SetAsPrimaryObjectResultAction.prototype.doWork = function() {
		var self = this;
		var target = this.context.target;
		var pid = this.target.getPid();
		
		$.ajax({
			url : "/services/api/edit/setAsPrimaryObject/" + pid,
			type : "PUT",
		}).done(function(reponse) {
			// Trigger refreshing of results
			self.context.actionHandler.addEvent({
				action : 'RefreshResult',
				target : target,
				waitForUpdate : true
			});
			
			self.context.view.$alertHandler.alertHandler("success", "Assignment of object with pid " + pid + " as primary object has completed.");
		})
	};
	
	return SetAsPrimaryObjectResultAction;
});