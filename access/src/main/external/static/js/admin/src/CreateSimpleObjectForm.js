/**
 * Implements functionality and UI for the generic Ingest Package form
 */
define('CreateSimpleObjectForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createSimpleObjectForm', 
		'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'], 
		function($, ui, _, RemoteStateChangeMonitor, simpleObjectTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Add Simple Object',
			createFormTemplate : simpleObjectTemplate
	};
	
	function CreateSimpleObjectForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	};
	
	CreateSimpleObjectForm.prototype.constructor = CreateSimpleObjectForm;
	CreateSimpleObjectForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	CreateSimpleObjectForm.prototype.preprocessForm = function() {
		var label = $("input[name='name']", this.$form).val();
		if (!label && this.ingestFile) {
			$("input[name='name']", this.$form).val(this.ingestFile.name);
		}
	};
	
	// Validate the form and retrieve any errors
	CreateSimpleObjectForm.prototype.validationErrors = function() {
		var errors = [];
		var dataFile = $("input[type='file']", this.$form).val();
		if (!dataFile)
			errors.push("You must select a file to ingest");
		return errors;
	};
	
	return CreateSimpleObjectForm;
});