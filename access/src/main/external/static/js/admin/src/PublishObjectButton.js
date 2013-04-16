define([ 'jquery', 'jquery-ui', 'AjaxCallbackButton', 'ResultObject'], function($) {
	$.widget("cdr.publishObjectButton", $.cdr.ajaxCallbackButton, {
		options : {
			defaultPublish: false,
			followupPath: "services/rest/item/{idPath}/solrRecord/version"
		},
		
		_create: function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);
			
			this.options.workDone = this.publishWorkDone;
			this.options.followup = this.publishFollowup;
			
			this.element.data("callbackButtonClass", "publishObjectButton");
			
			this.published = this.options.defaultPublish;
			if (this.published) {
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
		
		completeState : function() {
			if (this.options.parentObject) {
				this.options.parentObject.refresh(true);
			} else {
				this.toggleState();
			}
			this.enable();
		},
		
		toggleState : function() {
			if (this.published) {
				this.unpublishedState();
			} else {
				this.publishedState();
			}
		},

		publishedState : function() {
			this.published = true;
			this.element.text("Unpublish");
			this.setWorkURL("services/rest/edit/unpublish/{idPath}");
			this.options.workLabel = "Unpublishing...";
			this.options.followupLabel = "Unpublishing....";
		},

		unpublishedState : function() {
			this.published = false;
			this.element.text("Publish");
			this.setWorkURL("services/rest/edit/publish/{idPath}");
			this.options.workLabel = "Publishing...";
			this.options.followupLabel = "Publishing....";
		},

		publishWorkDone : function(data) {
			var jsonData;
			if ($.type(data) === "string") {
				try {
					jsonData = $.parseJSON(data);
				} catch (e) {
					throw "Failed to change publication status for " + this.pid.pid;
				}
			} else {
				jsonData = data;
			}
			
			
			this.completeTimestamp = jsonData.timestamp;
			return true;
		}
	});
});