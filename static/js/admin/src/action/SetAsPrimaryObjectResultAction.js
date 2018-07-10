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
	
	SetAsPrimaryObjectResultAction.prototype.getTargets = function(targets) {
		if (this.context.targets) {
			return this.context.targets;
		}
		return AbstractBatchAction.prototype.getTargets.call(this);
	};
	
	SetAsPrimaryObjectResultAction.prototype.execute = function() {
		var self = this;
		this.target = this.context.target;
			
		this.context.confirm.dialogOptions = {
			title : "Set Primary Access Object",
			dialogClass : "confirm_dialog",
			width: "400px"
		};
			
		this.context.confirm.promptText = "Set the selected object as the primary access object for its parent?";
	}
	
		AjaxCallbackAction.prototype.execute.call(this);
	};
	
//	SetAsPrimaryObjectResultAction.prototype.getExistingDWOs = function(newDWOs) {
//		var targets = [];
//		var resultList = this.resultList.resultObjects;
//		
//		for (var newId in newDWOs) {
//			var newDWO = newDWOs[newId];
//			
//			for (var id in resultList) {
//				var target = resultList[id];
//				
//				if (newDWO.metadata.rollup == target.metadata.rollup 
//						&& target.metadata.isPart
//						&& $.inArray('Default Access Object', target.metadata.contentStatus) != -1) {
//					targets.push(target);
//				}
//			}
//		}
//		return targets;
//	};
	
	SetAsPrimaryObjectResultAction.prototype.doWork = function() {
		var self = this;
		this.target = this.context.target;
		
		var existingDWOs = self.getExistingDWOs(this.targets);
		var updateList = this.targets.concat(existingDWOs.filter(function (item) {
			return self.targets.indexOf(item) < 0;
		}));
		
		var pid = this.target.getPid();
		
		$.ajax({
			url : "/services/api/edit/setAsPrimaryObject",
			type : "POST",
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
				target : this.target,
				waitForUpdate : true
			});
			
			self.context.view.$alertHandler.alertHandler("message", "Assignment of object as primary access object has started.");
		}).fail(function() {
			self.context.view.$alertHandler.alertHandler("error", "Failed to assign object as primary access object.");
		});
	};
	
	return SetAsPrimaryObjectResultAction;
});