define('CreateContainerForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createContainerForm', 
		'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
		function($, ui, _, RemoteStateChangeMonitor, createContainerForm, ModalLoadingOverlay, AbstractForm) {
	
	var defaultOptions = {
			title : 'Create container',
			createFormTemplate : createContainerForm
	};
	
	function CreateContainerForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	}
	
	CreateContainerForm.prototype.constructor = CreateContainerForm;
	CreateContainerForm.prototype = Object.create( AbstractForm.prototype );
	
	CreateContainerForm.prototype.open = function(resultObject) {
		this.options.containerType = this.getContainerType(resultObject);
		this.options.title = 'Create ' + this.options.containerType;
		this.options.privateOnlyBox = this.options.containerType !== "AdminUnit";
		
		AbstractForm.prototype.open.apply(this, [resultObject]);
	};
	
	CreateContainerForm.prototype.validationErrors = function() {
		var errors = [];

		// Validate input
		if (!this.containerName)
			errors.push("You must specify a name for the " + this.options.containerType);
		return errors;
	};

	CreateContainerForm.prototype.getContainerType = function (resultObject) {
		var parentType = resultObject.type;

		if (parentType === "ContentRoot") {
			return "AdminUnit";
		} else if (parentType === "AdminUnit") {
			return "Collection";
		} else {
			// For Collection and Folder parents
			return "Folder";
		}
	};
	
	CreateContainerForm.prototype.preprocessForm = function(resultObject) {
		this.containerName = $("input[name='name']", this.$form).val();
		this.staffOnly = $("input[name='staff_only']", this.$form);

		var pid;
		if ($.type(resultObject) === 'string') {
			pid = resultObject;
		} else {
			pid = resultObject.id;
		}

		var typeName = this.options.containerType.charAt(0).toLowerCase() + this.options.containerType.substr(1);
		var apiUrl = "/services/api/edit/create/" + typeName + "/" + pid + "?label=" + this.containerName;

		if (this.staffOnly.length > 0) {
			apiUrl += "&staffOnly=" + this.staffOnly.is(':checked');
		}

		this.action_url = apiUrl;
	};
	
	CreateContainerForm.prototype.getSuccessMessage = function(data) {
		return this.options.containerType + " " + this.containerName + " has been successfully created.";
	};
	
	CreateContainerForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while creating " + this.options.containerType + " " + this.containerName;
	};
	
	return CreateContainerForm;
});