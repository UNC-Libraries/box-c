define('CreateContainerForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createContainerForm', 
		'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
		function($, ui, _, RemoteStateChangeMonitor, createContainerForm, ModalLoadingOverlay, AbstractForm) {
	
	var defaultOptions = {
			title : 'Create container',
			createFormTemplate : createContainerForm,
			showUploadProgress : false
	};
	
	function CreateContainerForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	}
	
	CreateContainerForm.prototype.constructor = CreateContainerForm;
	CreateContainerForm.prototype = Object.create( AbstractForm.prototype );
	
	CreateContainerForm.prototype.validationErrors = function() {
		var errors = [];

		// Validate input
		if (!this.containerName)
			errors.push("You must specify a name for the " + this.containerType);
		return errors;
	};

	CreateContainerForm.prototype.containerType = function (resultObject) {
		var inputType = $("input[name='container_type']", this.$form);
		var parentType = resultObject.type;

		if (parentType === "RootObject") {
			inputType.val("adminUnit");
		} else if (parentType === "Unit") {
			inputType.val("collection")
		} else if (parentType === "Collection") {
            inputType.val("folder");
		} else {
            inputType.val("folder");
		}
	};
	
	CreateContainerForm.prototype.preprocessForm = function(resultObject) {
		this.containerName = $("input[name='name']", this.$form).val();
		this.containerType = $("input[name='container_type']", this.$form).val();

		var pid;
		if ($.type(resultObject) === 'string') {
			pid = resultObject;
		} else {
			pid = resultObject.id;
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