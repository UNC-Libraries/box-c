(function($) {
	$.widget("cdr.publishButton", $.cdr.ajaxCallbackButton, {
		options : {
			defaultPublish: false,
			workDone: this.publishWorkDone,
			followup: this.publishFollowup
		},
		
		_create: function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguements);
			
			if (this.options.defaultPublish) {
				this.publishState();
			} else {
				this.unpublishState();
			}
		},

		publishFollowup : function(data) {
			if (data && data > this.completeTimestamp) {
				return true;
			}
			return false;
		},

		publishState : function() {
			this.element.text("Unpublish");
			this.setWorkURL("services/rest/edit/unpublish/{idPath}");
			this.options.complete = this.options.parentObject.publish;
			this.options.workLabel = "Unpublishing...";
			this.options.followupLabel = "Unpublishing....";
		},

		unpublishState : function() {
			this.element.text("Publish");
			this.setWorkURL("services/rest/edit/publish/{idPath}");
			this.options.complete = this.options.parentObject.unpublish;
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