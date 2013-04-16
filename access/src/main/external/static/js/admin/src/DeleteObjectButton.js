define([ 'jquery', 'jquery-ui', 'AjaxCallbackButton'], function($) {
	$.widget("cdr.deleteObjectButton", $.cdr.ajaxCallbackButton, {
		options : {
			workLabel: "Deleting...",
			workPath: "delete/{idPath}",
			followupLabel: "Cleaning up...",
			followupPath: "services/rest/item/{idPath}/solrRecord/version",
			confirm: true,
			confirmMessage: "Delete this object?",
			animateSpeed: 'fast'
		},
		
		_create: function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);
			
			this.options.workDone = this.deleteWorkDone;
			this.options.followup = this.deleteFollowup;
			
			this.element.data("callbackButtonClass", "deleteObjectButton");
		},

		deleteFollowup: function(data) {
			if (data == null) {
				return true;
			}
			return false;
		},

		completeState: function() {
			if (this.options.parentObject != null)
				this.options.parentObject.deleteElement();
			this.destroy();
		},

		deleteWorkDone: function(data) {
			var jsonData;
			if ($.type(data) === "string") {
				try {
					jsonData = $.parseJSON(data);
				} catch (e) {
					throw "An error occurred while attempting to delete object " + this.pid.pid;
				}
			} else jsonData = data;
			
			this.completeTimestamp = jsonData.timestamp;
			return true;
		}
	});
});