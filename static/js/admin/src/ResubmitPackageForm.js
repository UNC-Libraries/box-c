/**
 * Implements functionality and UI for the Resubmit Package form
 */
define('ResubmitPackageForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/resubmitPackageForm', 
		'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'], 
		function($, ui, _, RemoteStateChangeMonitor, resubmitPackageTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Resubmit Ingest Package',
			createFormTemplate : resubmitPackageTemplate
	};
	
	function ResubmitPackageForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	ResubmitPackageForm.prototype.constructor = ResubmitPackageForm;
	ResubmitPackageForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	// Validate the form and retrieve any errors
	ResubmitPackageForm.prototype.validationErrors = function() {
		var errors = [];
		var packageFile = $("input[type='file']", this.$form).val();
		if (!packageFile) {
			errors.push("You must select an ingest package file to resubmit.");
		}
		return errors;
	};
	
	ResubmitPackageForm.prototype.getSuccessMessage = function(data) {
		return "The ingest package " + this.ingestFile.name + " has been successfully resubmitted. You will receive an email when it completes.";
	};
	
	ResubmitPackageForm.prototype.getErrorMessage = function(data) {
		if (data && data.error && !this.closed) {
			this.setError(data.error);
		}
		return "Failed to resubmit ingest package " + this.ingestFile.name + ".";
	};
	
	ResubmitPackageForm.prototype.setError = function(message) {
		$(".errors", this.$form).show();
		$(".error_message", this.$form).html(message);
		this.dialog.dialog("option", "position", "center");
	};
	
	return ResubmitPackageForm;
});