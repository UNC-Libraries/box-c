/**
 * Implements functionality and UI for the generic Ingest Package form
 */
define('IngestPackageForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/ingestPackageForm', 
		'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'], 
		function($, ui, _, RemoteStateChangeMonitor, ingestPackageTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Ingest package',
			createFormTemplate : ingestPackageTemplate
	};
	
	function IngestPackageForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	IngestPackageForm.prototype.constructor = IngestPackageForm;
	IngestPackageForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	// Validate the form and retrieve any errors
	IngestPackageForm.prototype.validationErrors = function() {
		var errors = [];
		var packageFile = $("input[type='file']", this.$form).val();
		if (!packageFile)
			errors.push("You must select a package file to ingest");
		return errors;
	};
	
	IngestPackageForm.prototype.getSuccessMessage = function(data) {
		return "Package " + this.ingestFile.name + " has been successfully uploaded for ingest.  You will receive an email when it completes.";
	};
	
	IngestPackageForm.prototype.getErrorMessage = function(data) {
		var message = "Failed to submit package " + this.ingestFile.name + " for ingest.";
		if (data && data.errorStack && !this.closed) {
			message += "  See errors below.";
			this.setError(data.errorStack);
		}
		return message;
	};
	
	return IngestPackageForm;
});