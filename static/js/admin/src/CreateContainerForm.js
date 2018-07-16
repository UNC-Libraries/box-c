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
	}
	
	CreateContainerForm.prototype.constructor = CreateContainerForm;
	CreateContainerForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	CreateContainerForm.prototype.validationErrors = function() {
		var errors = [];

		// Validate input
		if (!this.containerName)
			errors.push("You must specify a name for the " + this.containerType);
		return errors;
	};

	CreateContainerForm.prototype.showOptions = function (resultObject) {
		var regx = /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/;

		$("select option", this.$form).each(function() {
			var self = $(this);
			var optionType = self.attr('value');

			if (optionType === 'folder' && resultObject === 'adminUnit') {
				self.addClass('hidden');
			} else if ((optionType === 'collection' || optionType === 'adminUnit') && resultObject === 'collections') {
				self.addClass('hidden');
			} else if ((optionType === 'collection' || optionType === 'adminUnit') && regx.test(resultObject)) {
				self.addClass('hidden');
			} else {
				self.removeClass('hidden');
			}
		});
	};
	
	CreateContainerForm.prototype.preprocessForm = function(resultObject) {
		this.containerName = $("input[name='name']", this.$form).val();
		this.containerType = $("select", this.$form).val();
		var pid;
		if ($.type(resultObject) === 'string') {
			pid = resultObject;
		} else {
			pid = resultObject.metadata.id;
		}

		this.action_url = "/services/api/edit/create/" + this.containerType + "/" + pid + "?label=" + this.containerName;
	};
	
	CreateContainerForm.prototype.getSuccessMessage = function(data) {
		return this.containerType + " " + this.containerName + " has been successfully created.";
	};
	
	CreateContainerForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while creating " + this.containerType + " " + this.containerName;
	};
	
	return CreateContainerForm;
});