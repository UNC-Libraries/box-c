define('DeleteBatchAction', [ 'jquery', 'AbstractBatchAction'], function($, AbstractBatchAction) {
	
	function DeleteBatchAction(context) {
		this._create(context);
	};
	
	DeleteBatchAction.prototype.constructor = DeleteBatchAction;
	DeleteBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	DeleteBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("markForDeletion", target.metadata.permissions) != -1
					&& $.inArray("Marked For Deletion", target.getMetadata().status) == -1;
	};
	
	DeleteBatchAction.prototype.execute = function() {
		this.targets = this.getTargets();
		
		if (!('confirm' in this.context) || this.context.confirm) {
			this.context.confirm = {
				confirmAnchor : this.context.anchor
			};
			if (this.targets.length == 1)
				this.context.confirm.promptText = "Mark the selected object as deleted?";
			else
				this.context.confirm.promptText = "Mark " + this.targets.length + " selected objects as deleted?";
		}
	
		AbstractBatchAction.prototype.execute.call(this);
	};
	
	DeleteBatchAction.prototype.doWork = function() {
		var validTargets = this.targets;
		
		for (var index in validTargets) {
			this.actionHandler.addEvent({
				action : 'DeleteResult',
				target : validTargets[index],
				confirm : false
			});
		}
	};
	
	return DeleteBatchAction;
});