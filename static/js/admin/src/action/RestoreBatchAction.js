define('RestoreBatchAction', [ 'jquery', 'AbstractBatchAction'], function($, AbstractBatchAction) {
	function RestoreBatchAction(context) {
		this._create(context);
	};
	
	RestoreBatchAction.prototype.constructor = RestoreBatchAction;
	RestoreBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	RestoreBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("markForDeletion", target.metadata.permissions) != -1
			&& $.inArray("Deleted", target.getMetadata().status) != -1;
	};
	
	RestoreBatchAction.prototype.execute = function() {
		this.targets = this.getTargets();
		
		if (!('confirm' in this.context) || this.context.confirm) {
			this.context.confirm = {
				confirmAnchor : this.context.anchor
			};
			if (this.targets.length == 1)	
				this.context.confirm.promptText = "Restore the selected object from the trash?";
			else
				this.context.confirm.promptText = "Restore " + this.targets.length + " selected objects from the trash?";
		}
		
		AbstractBatchAction.prototype.execute.call(this);
	}
	
	RestoreBatchAction.prototype.doWork = function() {
		var validTargets = this.targets;
		
		for (var index in validTargets) {
			this.actionHandler.addEvent({
				action : 'RestoreResult',
				target : validTargets[index],
				confirm : false
			});
		}
	};
	
	return RestoreBatchAction;
});