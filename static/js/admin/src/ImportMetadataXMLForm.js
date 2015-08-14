define('ImportMetadataXMLForm', [ 'jquery', 'jquery-ui', 'underscore', 'tpl!../templates/admin/importMetadataXMLForm', 
		'ModalLoadingOverlay', 'AbstractFileUploadForm'], 
		function($, ui, _, importTemplate, ModalLoadingOverlay, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Import MODS',
			createFormTemplate : importTemplate
	};
	
	function ImportMetadataXMLForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	ImportMetadataXMLForm.prototype.constructor = ImportMetadataXMLForm;
	ImportMetadataXMLForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	ImportMetadataXMLForm.prototype.preprocessForm = function() {
		var label = $("input[name='name']", this.$form).val();
		if (!label && this.ingestFile) {
			$("input[name='name']", this.$form).val(this.ingestFile.name);
		}
	};
	
	// Validate the form and retrieve any errors
	ImportMetadataXMLForm.prototype.validationErrors = function() {
		var errors = [];
		var dataFile = $("input[type='file']", this.$form).val();
		if (!dataFile)
			errors.push("You must select a file to import");
		return errors;
	};
	
	ImportMetadataXMLForm.prototype.getSuccessMessage = function(data) {
		return this.ingestFile.name + " has been successfully uploaded for updating.  You will receive an email when it completes.";
	};
	
	ImportMetadataXMLForm.prototype.getErrorMessage = function(data) {
		var message = "Failed to import file " + this.ingestFile.name + ".";
		if (data && data.errorStack && !this.closed) {
			message += "  See errors below.";
			this.setError(data.errorStack);
		}
		return message;
	};
	
	return ImportMetadataXMLForm;
});