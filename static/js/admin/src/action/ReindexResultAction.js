define('ReindexResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function ReindexResultAction(context) {
		this._create(context);
	}
	
	ReindexResultAction.prototype.constructor = ReindexResultAction;
	ReindexResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	ReindexResultAction.prototype.actionName = "Reindex";
	
	ReindexResultAction.prototype._create = function(context) {
		this.context = context;
		
		this.options = {
				workLabel: "Updating...",
				workPath: "/services/api/edit/solr/reindex/{idPath}?inplace=true",
				workMethod: $.post,
				followupLabel: "Updating...",
				followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
				confirm: {
					promptText : "Reindex this object and all of its children?",
					confirmAnchor : this.context.confirmAnchor
				},
				followup: false
			};
		AjaxCallbackAction.prototype._create.call(this, this.options);
	};

	ReindexResultAction.prototype.complete = function() {
		if (this.context.target.metadata)
			this.alertHandler.alertHandler("success", "Reindexing of " + this.context.target.metadata.title + " is underway");
		else this.alertHandler.alertHandler("success", "Reindexing is underway");
		
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target,
			waitForUpdate : true,
			maxAttempts : 5
		});
	};
	
	ReindexResultAction.prototype.workDone = function() {
		return true;
	}
	
	return ReindexResultAction;
});