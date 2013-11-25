define('PublishObjectButton', [ 'jquery', 'jquery-ui', 'AjaxCallbackButton', 'ResultObject'], function($, ui, AjaxCallbackButton) {
	function PublishObjectButton(options) {
		this._create(options);
	};
	
	PublishObjectButton.prototype.constructor = PublishObjectButton;
	PublishObjectButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			defaultPublish: false,
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
			workMethod: $.post
		};
		
	PublishObjectButton.prototype._create = function(options) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workDone = this.publishWorkDone;
		merged.followup = this.publishFollowup;
		AjaxCallbackButton.prototype._create.call(this, merged);
		
		this.published = this.options.defaultPublish;
		if (this.published) {
			this.publishedState();
		} else {
			this.unpublishedState();
		}
	};

	PublishObjectButton.prototype.publishFollowup = function(data) {
		if (data) {
			return this.options.parentObject.updateVersion(data);
		}
		return false;
	};
	
	PublishObjectButton.prototype.completeState = function() {
		if (this.options.parentObject) {
			this.options.parentObject.refresh(true);
			this.options.parentObject.enable();
		} else {
			this.toggleState();
		}
		this.enable();
	};
	
	PublishObjectButton.prototype.toggleState = function() {
		if (this.published) {
			this.unpublishedState();
		} else {
			this.publishedState();
		}
	};

	PublishObjectButton.prototype.publishedState = function() {
		this.published = true;
		this.setWorkURL("/services/api/edit/unpublish/{idPath}");
		this.options.workLabel = "Unpublishing...";
		this.options.followupLabel = "Unpublishing....";
	};

	PublishObjectButton.prototype.unpublishedState = function() {
		this.published = false;
		this.setWorkURL("/services/api/edit/publish/{idPath}");
		this.options.workLabel = "Publishing...";
		this.options.followupLabel = "Publishing....";
	};

	PublishObjectButton.prototype.publishWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "Failed to change publication status for " + (this.options.metadata? this.options.metadata.title : this.pid);
			}
		} else {
			jsonData = data;
		}
		
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	
	return PublishObjectButton;
});