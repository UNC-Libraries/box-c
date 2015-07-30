define('PublishAction', ['jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
		
	function PublishAction(context) {
		this._create(context);
	};

	PublishAction.prototype.constructor = PublishAction;
	PublishAction.prototype = Object.create(AjaxCallbackAction.prototype);
	
	PublishAction.prototype.actionName = "Publish";

	PublishAction.prototype._create = function(context) {
		this.context = context;
		
		this.options = {
			followupPath : "/services/api/status/item/{idPath}/solrRecord/version",
			workMethod : $.post
		};
		
		this._configure();
		
		AjaxCallbackAction.prototype._create.call(this, this.options);
	};
	
	PublishAction.prototype.followup = function(data) {
		if (data) {
			return this.context.target.updateVersion(data);
		}
		return false;
	};
	
	PublishAction.prototype.completeState = function() {
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target
		});
		
		this.context.target.enable();
	};

	PublishAction.prototype._configure = function() {
		this.options.workPath = "/services/api/edit/publish/{idPath}";
		this.options.workLabel = "Publishing...";
		this.options.followupLabel = "Publishing....";
	};
	
	return PublishAction;
});