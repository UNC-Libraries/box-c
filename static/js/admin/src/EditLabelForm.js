define('EditLabelForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editLabelForm', 
		'ModalLoadingOverlay', 'AbstractFileUploadForm', 'AlertHandler'], 
		function($, ui, _, RemoteStateChangeMonitor, editLabelForm, ModalLoadingOverlay, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Edit Label',
			createFormTemplate : editLabelForm,
			showUploadProgress : false
	};
	
	function EditLabelForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	
	EditLabelForm.prototype.constructor = EditLabelForm;
	EditLabelForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	EditLabelForm.prototype.validationErrors = function() {
		var errors = [];
		var label = $("input[name='label']", this.$form).val();
		// Validate input
		if (!label)
			errors.push("You must specify a label.");
		return errors;
	};
	
		
	EditLabelForm.prototype.getSuccessMessage = function(data) {
		return "Label has been successfully edited.";
	};
	
	EditLabelForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while editing the label";
	};
	
	
	EditLabelForm.prototype.remove = function() {
		AbstractFileUploadForm.prototype.remove.apply(this);
		this.options.actionHandler.addEvent({
			action : 'RefreshResult',
			target : this.resultObject,
			waitForUpdate : true
		});
		
	};
	
	EditLabelForm.prototype.close = function() {
		AbstractFileUploadForm.prototype.remove.apply(this);
	};
	
	return EditLabelForm;
});