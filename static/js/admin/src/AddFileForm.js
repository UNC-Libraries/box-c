/**
 * Implements functionality and UI for adding a single file to an aggregate work
 */
define('AddFileForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/addFileForm',
	'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'],
	function($, ui, _, RemoteStateChangeMonitor, addWorkTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {

		var defaultOptions = {
			title : 'Add File',
			createFormTemplate : addWorkTemplate
		};

		function AddFileForm(options) {
			this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
		};

		AddFileForm.prototype.constructor = AddFileForm;
		AddFileForm.prototype = Object.create( AbstractFileUploadForm.prototype );

		AddFileForm.prototype.preprocessForm = function() {
			var label = $("input[name='name']", this.$form).val();
			if (!label && this.ingestFile) {
				$("input[name='name']", this.$form).val(this.ingestFile.name);
			}
		};

		// Validate the form and retrieve any errors
		AddFileForm.prototype.validationErrors = function() {
			var errors = [];
			var dataFile = $("input[type='file']", this.$form).val();
			if (!dataFile)
				errors.push("You must select a file to ingest");
			return errors;
		};

		AddFileForm.prototype.getSuccessMessage = function(data) {
			return this.ingestFile.name + " has been successfully uploaded for ingest.  You will receive an email when it completes.";
		};

		AddFileForm.prototype.getErrorMessage = function(data) {
			var message = "Failed to ingest file " + this.ingestFile.name + ".";
			if (data && data.errorStack && !this.closed) {
				message += "  See errors below.";
				this.setError(data.errorStack);
			}
			return message;
		};

		return AddFileForm;
	});