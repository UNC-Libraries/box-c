define('ReindexResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function ReindexResultAction(context) {
		this._create(context);
	}
	
	ReindexResultAction.prototype.constructor = ReindexResultAction;
	ReindexResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	ReindexResultAction.prototype._create = function(context) {
		this.context = context;
		
		this.options = {
				workLabel: "Updating...",
				workPath: "/services/api/edit/solr/reindex/{idPath}?inplace=true",
				workMethod: $.post,
				followupLabel: "Updating...",
				followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
				confirm: true,
				confirmMessage: "Reindex this object and all of its children?",
				animateSpeed: 'fast',
				complete: ReindexResultAction.prototype.complete
			};
		AjaxCallbackAction.prototype._create.call(this, this.options);
	};
	
	ReindexResultAction.prototype.complete = function() {
		if (this.context.target.metadata)
			this.alertHandler.alertHandler("success", "Reindexing of " + this.context.target.metadata.title + " is underway, view status monitor");
		else this.alertHandler.alertHandler("success", "Reindexing is underway, view status monitor");
	};

	ReindexResultAction.prototype.completeState = function() {
		this.context.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.context.target,
			waitForUpdate : true
		});
	};
	
	return ReindexResultAction;
});