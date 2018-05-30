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
		return target.isSelected() && target.isEnabled() && $.inArray("editResourceType", target.metadata.permissions) != -1
				&& (target.getMetadata().isPart);
	};
	
	SetAsDefaultWebObjectBatchAction.prototype.getTargets = function(targets) {
		if (this.context.targets) {
			return this.context.targets;
		}
		return AbstractBatchAction.prototype.getTargets.call(this);
	};
	
	SetAsDefaultWebObjectBatchAction.prototype.execute = function() {
		var self = this;
		this.targets = this.getTargets();
		
		if (!('confirm' in this.context) || this.context.confirm) {
			var parentSet = {};
			var multipleParents = false;
			
			this.context.confirm = {
				confirmAnchor : this.context.anchor
			};
			
			// Validating whether multiple items being selected have the same parent
			for (var i in this.targets) {
				var parentId = this.targets[i].metadata.rollup;
				if (parentId in parentSet) {
					multipleParents = true;
					parentSet[parentId].push(this.targets[i]);
				} else {
					parentSet[parentId] = [this.targets[i]];
				}
			}
			
			// Adding warning if there are repeat parents and disabling the action
			if (multipleParents) {
				var warningText =  "<h2>Error</h2><p>Some of the selected objects belong to the same aggregate object, but an aggregate object may have only one primary access object.</p><p>These objects cannot be made primary access objects:</p> <ul>";
				$.each(parentSet, function(key, resultSet) {
					if (resultSet.length > 1) {
						var parentTitle = self.resultList.getResultObject(key).metadata.title;
						warningText += "<li>" + parentTitle + "<ul>";
						for (var i in resultSet) {
							warningText += "<li>" + resultSet[i].metadata.title + "</li>";
						}
						warningText += "</ul></li>";
					}
				});
				warningText += "</ul>";

				this.context.confirm.warningText = warningText;
				this.context.confirm.disableConfirm = true;
			}
			
			this.context.confirm.dialogOptions = {
				title : "Set Primary Access Object",
				dialogClass : "confirm_dialog",
				width: "400px"
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