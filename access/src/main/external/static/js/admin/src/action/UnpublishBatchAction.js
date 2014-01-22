define('UnpublishBatchAction', [ 'jquery', 'AbstractBatchAction'], function($, AbstractBatchAction) {
	function UnpublishBatchAction(options, element) {
		this._create(options, element);
	};
	
	UnpublishBatchAction.prototype.constructor = UnpublishBatchAction;
	UnpublishBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	UnpublishBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && $.inArray("publish", target.metadata.permissions) != -1
			&& $.inArray("Unpublished", target.getMetadata().status) == -1 && target.isEnabled();
	};
	
	UnpublishBatchAction.prototype.doWork = function() {
		var validTargets = this.getTargets();
		
		for (var index in validTargets) {
			this.actionHandler.addEvent({
				action : 'Unpublish',
				target : validTargets[index]
			});
		}
	};
	
	return UnpublishBatchAction;
});