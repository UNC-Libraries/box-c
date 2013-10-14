define('ReindexObjectButton', [ 'jquery', 'AjaxCallbackButton'], function($, AjaxCallbackButton) {
	function ReindexObjectButton(options) {
		this._create(options);
	}
	
	ReindexObjectButton.prototype.constructor = ReindexObjectButton;
	ReindexObjectButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			workLabel: "Updating...",
			workPath: "services/rest/edit/solr/reindex/{idPath}?inplace=true",
			workMethod: $.post,
			followupLabel: "Updating...",
			followupPath: "services/rest/item/{idPath}/solrRecord/version",
			confirm: true,
			confirmMessage: "Reindex this object and all of its children?",
			animateSpeed: 'fast',
			complete: ReindexObjectButton.prototype.complete
		};
		
	ReindexObjectButton.prototype._create = function(options) {
		var merged = $.extend({}, defaultOptions, options);
		merged.complete = ReindexObjectButton.prototype.complete;
		AjaxCallbackButton.prototype._create.call(this, merged);
		
		if (this.options.parentObject)
			this.options.confirmAnchor = this.options.parentObject.element;
	};
	
	ReindexObjectButton.prototype.complete = function() {
		if (this.options.metadata)
			this.alertHandler.alertHandler("success", "Reindexing of " + this.options.metadata.title + " is underway, view status monitor");
		else this.alertHandler.alertHandler("success", "Reindexing is underway, view status monitor");
	};

	ReindexObjectButton.prototype.completeState = function() {
		if (this.options.parentObject != null)
			this.options.parentObject.refresh(true);
	};
	
	return ReindexObjectButton;
});