define([ 'jquery', 'jquery-ui', 'AjaxCallbackButton'], function($) {
	$.widget("cdr.publishObjectButton", $.cdr.ajaxCallbackButton, {
		options : {
			defaultPublish: false,
			followupPath: "services/rest/item/{idPath}/solrRecord/version"
		},
		
		_create: function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);
			
			this.options.workDone = this.publishWorkDone;
			this.options.followup = this.publishFollowup;
			this.options.completeTarget = this.options.parentObject;
			
			this.element.data("callbackButtonClass", "publishObjectButton");
			
			if (this.options.defaultPublish) {
				this.publishedState();
			} else {
				this.unpublishedState();
			}
		},

		publishFollowup : function(data) {
			if (data) {
				return this.options.parentObject.updateVersion(data);
			}
			return false;
		},

		publishedState : function() {
			this.element.text("Unpublish");
			this.setWorkURL("services/rest/edit/unpublish/{idPath}");
			this.options.complete = this.options.parentObject.unpublish;
			this.options.workLabel = "Unpublishing...";
			this.options.followupLabel = "Unpublishing....";
		},

		unpublishedState : function() {
			this.element.text("Publish");
			this.setWorkURL("services/rest/edit/publish/{idPath}");
			this.options.complete = this.options.parentObject.publish;
			this.options.workLabel = "Publishing...";
			this.options.followupLabel = "Publishing....";
		},

		publishWorkDone : function(data) {
			if (data == null) {
				alert("Failed to change publication status for " + this.pid.pid);
				return false;
			}
			this.completeTimestamp = data.timestamp;
			return true;
		}
	});
});