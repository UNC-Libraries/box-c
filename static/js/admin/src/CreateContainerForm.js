define('CreateContainerForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createContainerForm', 
		'ModalLoadingOverlay', 'AbstractFileUploadForm', 'AlertHandler'], 
		function($, ui, _, RemoteStateChangeMonitor, createContainerForm, ModalLoadingOverlay, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Create container',
			createFormTemplate : createContainerForm,
			showUploadProgress : false
	};
	
	function CreateContainerForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	CreateContainerForm.prototype.constructor = CreateContainerForm;
	CreateContainerForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	CreateContainerForm.prototype.validationErrors = function() {
		var errors = [];
		var description = $("input[type='file']", this.$form).val();
		// Validate input
		if (!this.containerName)
			errors.push("You must specify a name for the " + this.containerType);
		return errors;
	};
	
	CreateContainerForm.prototype.preprocessForm = function() {
		this.containerName = $("input[name='name']", this.$form).val();
		this.containerType = $("select", this.$form).val();
	};
	
	CreateContainerForm.prototype.getSuccessMessage = function(data) {
		return this.containerType + " " + this.containerName + " has been successfully created.";
	};
	
	CreateContainerForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while creating " + this.containerType + " " + this.containerName;
	};
	
	return CreateContainerForm;
});