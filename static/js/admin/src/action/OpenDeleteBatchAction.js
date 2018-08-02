/**
* Action which opens a dialogue for confirming and providing a message when marking a batch of objects for deletion.
*/
define('OpenDeleteBatchAction', [ 'jquery', 'AbstractBatchAction', 'DeleteForm', 'DeleteBatchAction'], function($, AbstractBatchAction, DeleteForm, DeleteBatchAction) {
	
	function OpenDeleteBatchAction(context) {
		this._create(context);
	};
	
	OpenDeleteBatchAction.prototype.constructor = OpenDeleteBatchAction;
	OpenDeleteBatchAction.prototype = Object.create( DeleteBatchAction.prototype );
	
	OpenDeleteBatchAction.prototype.execute = function() {
		AbstractBatchAction.prototype.execute.call(this);
	};
	
	OpenDeleteBatchAction.prototype.doWork = function() {
		this.targets = this.getTargets();
		
		var deleteForm = new DeleteForm({
			alertHandler : this.context.alertHandler,
			actionHandler : this.actionHandler
		});
		deleteForm.open(this.targets);
	};
	
	return OpenDeleteBatchAction;
});