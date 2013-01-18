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
			this.options.completeTarget = this.options.parentObject;
			this.options.complete = this.options.parentObject.deleteObject;
			
			this.element.data("callbackButtonClass", "deleteObjectButton");
		},

		deleteFollowup: function(data) {
			if (data == null) {
				return true;
			}
			return false;
		},

		deleteComplete: function() {
			obj.deleteObject();
			this.destroy();
		},

		deleteWorkDone: function(data) {
			if (data == null) {
				alert("Unable to delete object " + this.pid.pid);
				return false;
			}
			this.completeTimestamp = data.timestamp;
			return true;
		}
	});
});