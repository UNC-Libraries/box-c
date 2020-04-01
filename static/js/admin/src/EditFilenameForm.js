define('EditFilenameForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editFilenameForm',
		'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'], 
		function($, ui, _, RemoteStateChangeMonitor, editFilenameForm, ModalLoadingOverlay, AbstractForm) {
	
	var defaultOptions = {
			title : 'Edit Filename',
			createFormTemplate : editFilenameForm,
			submitMethod: 'PUT'
	};
	
	function EditFilenameForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	}
	
	EditFilenameForm.prototype.constructor = EditFilenameForm;
	EditFilenameForm.prototype = Object.create( AbstractForm.prototype );

	EditFilenameForm.prototype.open = function(resultObject) {
		resultObject.metadata.currentFilename = this.getCurrentFilename(resultObject.metadata.datastream);
		AbstractForm.prototype.open.call(this, resultObject);
	};
	
	EditFilenameForm.prototype.preprocessForm = function(resultObject) {
		var newLabel = $("input[name='label']", this.$form).val();
		var pid = resultObject.metadata.id;

		this.action_url = "/services/api/edit/filename/" + pid + "?label=" + newLabel;
	};
	
	EditFilenameForm.prototype.validationErrors = function() {
		var errors = [];
		var label = $("input[name='label']", this.$form).val();
		// Validate input
		if (!label)
			errors.push("You must specify a filename.");
		return errors;
	};

	EditFilenameForm.prototype.getCurrentFilename = function(datastream) {
		var filename = '';

		for (var i=0; i<datastream.length; i++) {
			if (/^original_file/.test(datastream[i])) {
				filename = datastream[i].split("|")[2];
				break;
			}
		}

		return filename;
	};

	EditFilenameForm.prototype.getSuccessMessage = function(data) {
		return "Filename has been successfully edited.";
	};
	
	EditFilenameForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while editing the filename";
	};
	
	EditFilenameForm.prototype.remove = function() {
		AbstractForm.prototype.remove.apply(this);
		if (this.submitSuccessful) {
			this.options.actionHandler.addEvent({
				action : 'RefreshResult',
				target : this.resultObject,
				waitForUpdate : true
			});
		}
	};
	
	return EditFilenameForm;
});