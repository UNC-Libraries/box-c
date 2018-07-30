define('DeleteForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/deleteForm', 
		'AbstractForm', 'DeleteBatchAction', 'AlertHandler'],
		function($, ui, _, RemoteStateChangeMonitor, deleteForm,  AbstractForm, DeleteBatchAction) {
	
	var defaultOptions = {
			title : 'Mark for Deletion',
			createFormTemplate : deleteForm
	};
	
	/**
	* Requires option actionHandler containing an ActionEventHandler object
	*/
	function DeleteForm(options) {
		this.options = $.extend({}, defaultOptions, options);
		this.actionHandler = this.options.actionHandler;
	}
	
	DeleteForm.prototype.constructor = DeleteForm;
	DeleteForm.prototype = Object.create( AbstractForm.prototype );
	
	/**
	* resultObjects is expected to be an array of resultEntry objects
	*/
	DeleteForm.prototype.open = function(resultObjects) {
    if (resultObjects.length == 1) {
      this.options.delete_target_message = resultObjects[0].metadata.title;
    } else {
      this.options.delete_target_message = resultObjects.length + " objects";
    }
		
		AbstractForm.prototype.open.apply(this, [resultObjects]);
	};
	
	DeleteForm.prototype.validationErrors = function() {
		var errors = [];

		// Validate input
		if (!this.delete_message) {
			errors.push("You must specify a reason for this deletion");
		}
		return errors;
	};

	DeleteForm.prototype.submit = function(e) {
		e.preventDefault();
		this.delete_message = $("textarea[name='delete_reason']", this.$form).val();
	
		errors = this.validationErrors();
		if (errors && errors.length > 0) {
			this.options.alertHandler.alertHandler("error", errors);
			return false;
		}
		
		if (this.resultObject.length == 1) {
			this.actionHandler.addEvent({
				action : 'DeleteResult',
				target : this.resultObject[0],
				delete_message : this.delete_message,
				confirm: false
			});
		} else {
			this.actionHandler.addEvent({
				action : 'DeleteBatch',
				target : this.resultObject,
				delete_message : this.delete_message,
				confirm: false
			});
		}

		this.remove();

		return false;
	}
	
	return DeleteForm;
});