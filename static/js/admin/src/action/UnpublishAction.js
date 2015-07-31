define('UnpublishAction', ['jquery', 'AjaxCallbackAction', 'PublishAction'], function($, AjaxCallbackAction, PublishAction) {
		
	function UnpublishAction(context) {
		this._create(context);
	};

	UnpublishAction.prototype.constructor = UnpublishAction;
	UnpublishAction.prototype = Object.create(PublishAction.prototype);
	
	UnpublishAction.prototype.actionName = "Unpublish";
	
	UnpublishAction.prototype._configure = function() {
		this.options.workPath = "/services/api/edit/unpublish/{idPath}";
		this.options.workLabel = "Unpublishing...";
		this.options.followupLabel = "Unpublishing....";
	};
	
	return UnpublishAction;
});