define('AbstractBatchAction', ['jquery', 'AjaxCallbackAction', 'ConfirmationDialog'], function($, AjaxCallbackAction, ConfirmationDialog) {
	
	function AbstractBatchAction(context) {
		this._create(options);
	};
	
	AbstractBatchAction.prototype._create = function(context) {
		this.context = context;
		this.actionHandler = context.actionHandler;
		this.resultList = this.context.target;
	};
	
	AbstractBatchAction.prototype.execute = function() {
		if (this.context.confirm) {
			var confirmOptions = $.extend({
				promptText : "Are you sure?",
				confirmFunction : this.doWork,
				confirmTarget : this,
				autoOpen : true,
				dialogOptions : {
					width : 'auto',
					modal : true
				}
			}, this.context.confirm);
				
			if (confirmOptions.confirmAnchor) {
				confirmOptions.dialogOptions['position'] = {};
				confirmOptions.dialogOptions['position']['of'] = confirmOptions.confirmAnchor; 
			}
		
			var confirmationDialog = new ConfirmationDialog(confirmOptions);
		} else {
			this.doWork();
		}
	};
	
	AbstractBatchAction.prototype.getTargetIds = function() {
		var targetIds = [];
		var targetList = this.resultList.resultObjects;
		for (var id in targetList) {
			var target = targetList[id];
			if (this.isValidTarget(target))
				targetIds.push(target.getPid());
		}
		return targetIds;
	};
	
	AbstractBatchAction.prototype.getTargets = function(targets) {
		var targets = [];
		var targetList = this.resultList.resultObjects;
		for (var id in targetList) {
			var target = targetList[id];
			if (this.isValidTarget(target))
				targets.push(target);
		}
		return targets;
	};

	AbstractBatchAction.prototype.hasTargets = function(targets) {
		var targetList = this.resultList.resultObjects;
		for (var id in targetList) {
			var target = targetList[id];
			if (this.isValidTarget(target))
				return true;
		}
		return false;
	};
	
	AbstractBatchAction.prototype.countTargets = function() {
		var count = 0;
		var targetList = this.resultList.resultObjects;
		for (var id in targetList) {
			var target = targetList[id];
			if (this.isValidTarget(target))
				count++;
		}
		return count;
	};
	
	AbstractBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected();
	};

	return AbstractBatchAction;
});