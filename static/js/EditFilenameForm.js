define('editFilenameForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editFilenameForm', 
		'ModalLoadingOverlay', 'AbstractFileUploadForm', 'AlertHandler'], 
		function($, ui, _, RemoteStateChangeMonitor, editFilenameForm, ModalLoadingOverlay, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Edit Filename',
			createFormTemplate : editFilenameForm,
			showUploadProgress : false
	};
	
	function editFilenameForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	
	editFilenameForm.prototype.constructor = editFilenameForm;
	editFilenameForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	editFilenameForm.prototype.validationErrors = function() {
		var errors = [];
		var filename = $("input[name='filename']", this.$form).val();
		// Validate input
		if (!filename)
			errors.push("You must specify a filename.");
		return errors;
	};
	
		
	editFilenameForm.prototype.getSuccessMessage = function(data) {
		return "Filename has been successfully edited.";
	};
	
	editFilenameForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while editing the filename";
	};
	
	
	editFilenameForm.prototype.remove = function() {
		AbstractFileUploadForm.prototype.remove.apply(this);
		this.options.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.resultObject,
			waitForUpdate : true
		});
		
	};
	
	editFilenameForm.prototype.close = function() {
		AbstractFileUploadForm.prototype.remove.apply(this);
	};
	
	return editFilenameForm;
});