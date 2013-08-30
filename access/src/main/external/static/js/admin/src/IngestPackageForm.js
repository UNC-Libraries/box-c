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
		this.options = $.extend({}, defaultOptions, options);
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
	
	return IngestPackageForm;
});