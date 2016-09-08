define('EditFilenameForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editFilenameForm', 
		'ModalLoadingOverlay', 'AbstractFileUploadForm', 'AlertHandler'], 
		function($, ui, _, RemoteStateChangeMonitor, editFilenameForm, ModalLoadingOverlay, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Edit Filename',
			createFormTemplate : editFilenameForm,
			showUploadProgress : false
	};
	
	function EditFilenameForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	
	EditFilenameForm.prototype.constructor = editFilenameForm;
	EditFilenameForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	EditFilenameForm.prototype.validationErrors = function() {
		var errors = [];
		var filename = $("input[name='label']", this.$form).val();
		// Validate input
		if (!filename)
			errors.push("You must specify a filename.");
		return errors;
	};
	
		
	EditFilenameForm.prototype.getSuccessMessage = function(data) {
		return "Filename has been successfully edited.";
	};
	
	EditFilenameForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while editing the filename";
	};
	
	
	EditFilenameForm.prototype.remove = function() {
		AbstractFileUploadForm.prototype.remove.apply(this);
		this.options.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.resultObject,
			waitForUpdate : true
		});
		
	};
	
	EditFilenameForm.prototype.close = function() {
		AbstractFileUploadForm.prototype.remove.apply(this);
	};
	
	return EditFilenameForm;
});