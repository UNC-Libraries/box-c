define('SetAsDefaultWebObjectBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!../templates/admin/editTypeForm"], function($, AbstractBatchAction, editTypeTemplate) {

	function SetAsDefaultWebObjectBatchAction(context) {
		this._create(context);
		if (!this.resultList) {
			this.resultList = this.context.resultTable.resultObjectList;
		}
	};
	
	SetAsDefaultWebObjectBatchAction.prototype.constructor = SetAsDefaultWebObjectBatchAction;
	SetAsDefaultWebObjectBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	SetAsDefaultWebObjectBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("addRemoveContents", target.metadata.permissions) != -1
				&& (target.getMetadata().isPart);
	};
	
	SetAsDefaultWebObjectBatchAction.prototype.getTargets = function(targets) {
		if (this.context.targets) {
			return this.context.targets;
		} 
		return AbstractBatchAction.prototype.getTargets.call(this);
	};
	
	SetAsDefaultWebObjectBatchAction.prototype.execute = function() {
		this.targets = this.getTargets();
		
		if (!('confirm' in this.context) || this.context.confirm) {
			this.context.confirm = {
				confirmAnchor : this.context.anchor
			};
			if (this.targets.length == 1)
				this.context.confirm.promptText = "Set the selected object as the primary access object for its parent?";
			else
				this.context.confirm.promptText = "Set the selected " + this.targets.length + " objects as the primary access objects for their parents?";
		}
	
		AbstractBatchAction.prototype.execute.call(this);
	};
	
	SetAsDefaultWebObjectBatchAction.prototype.getExistingDWOs = function(newDWOs) {
		var targets = [];
		var resultList = this.resultList.resultObjects;
		
		for (var newId in newDWOs) {
			var newDWO = newDWOs[newId];
			
			for (var id in resultList) {
				var target = resultList[id];
				
				if (newDWO.metadata.rollup == target.metadata.rollup 
						&& target.metadata.isPart
						&& $.inArray('Default Access Object', target.metadata.contentStatus) != -1) {
					targets.push(target);
				}
			}
		}
		return targets;
	};
	
	SetAsDefaultWebObjectBatchAction.prototype.doWork = function() {
		var self = this;
		this.targets = this.getTargets();
		
		var existingDWOs = self.getExistingDWOs(this.targets);
		var updateList = this.targets.concat(existingDWOs.filter(function (item) {
			return self.targets.indexOf(item) < 0;
		}));
		
		var pids = [];
		for (var index in this.targets) {
			pids.push(this.targets[index].getPid());
		}
		
		$.ajax({
			url : "/services/api/edit/editDefaultWebObject",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			dataType: "json",
			data : JSON.stringify({
				pids : pids,
				clear : this.context.clear == true
			})
		}).done(function(reponse) {
			for (var index in updateList) {
				// Trigger refreshing of results
				self.context.actionHandler.addEvent({
					action : 'RefreshResult',
					target : updateList[index],
					waitForUpdate : true
				});
			}
			
			self.context.view.$alertHandler.alertHandler("message", "Assignment of " + self.targets.length + " object(s) as primary access objects has started.");
		}).fail(function() {
			self.context.view.$alertHandler.alertHandler("error", "Failed to assign " + self.targets.length + " object(s) as primary access objects.");
		});
	};
	
	return SetAsDefaultWebObjectBatchAction;
});