define('PublishBatchAction', [ 'jquery', 'AbstractBatchAction'], function($, AbstractBatchAction) {
	function PublishBatchAction(context) {
		this._create(context);
	};
	
	PublishBatchAction.prototype.constructor = PublishBatchAction;
	PublishBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	PublishBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && $.inArray("publish", target.metadata.permissions) != -1
			&& $.inArray("Unpublished", target.getMetadata().status) != -1 && target.isEnabled();
	};
	
	PublishBatchAction.prototype.doWork = function() {
		var validTargets = this.getTargets();
		
		for (var index in validTargets) {
			this.actionHandler.addEvent({
				action : 'Publish',
				target : validTargets[index]
			});
		}
	};
	
	return PublishBatchAction;
});