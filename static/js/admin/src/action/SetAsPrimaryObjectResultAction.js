define('SetAsPrimaryObjectResultAction', [ 'jquery', 'AjaxCallbackAction', "tpl!../templates/admin/editTypeForm"], function($, AjaxCallbackAction, editTypeTemplate) {

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
			url : "/services/api/edit/setAsPrimaryObject",
			type : "PUT",
			contentType: "application/json; charset=utf-8",
			dataType: "json",
			data : JSON.stringify({
				pid : pid,
				clear : this.context.clear == true
			})
		}).done(function(reponse) {
				// Trigger refreshing of results
				self.context.actionHandler.addEvent({
					action : 'RefreshResult',
					target : target,
					waitForUpdate : true
			});
			
			self.context.view.$alertHandler.alertHandler("message", "Assignment of object with pid " + pid + " as primary object has started.");
		}).fail(function() {
			self.context.view.$alertHandler.alertHandler("error", "Failed to assign object with pid " + pid + " as primary object.");
		});
	};
	
	return SetAsPrimaryObjectResultAction;
});