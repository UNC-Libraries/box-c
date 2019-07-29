define('EditLabelForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editLabelForm', 
		'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'], 
		function($, ui, _, RemoteStateChangeMonitor, editLabelForm, ModalLoadingOverlay, AbstractForm) {
	
	var defaultOptions = {
			title : 'Edit Label',
			createFormTemplate : editLabelForm,
			submitMethod: 'PUT'
	};
	
	function EditLabelForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	};
	
	EditLabelForm.prototype.constructor = EditLabelForm;
	EditLabelForm.prototype = Object.create( AbstractForm.prototype );
	
	EditLabelForm.prototype.preprocessForm = function(resultObject) {
		var newLabel = $("input[name='label']", this.$form).val();
		var pid = resultObject.metadata.id;

		this.action_url = "/services/api/edit/label/" + pid + "?label=" + newLabel;
	};
	
	EditLabelForm.prototype.validationErrors = function() {
		var errors = [];
		var label = $("input[name='label']", this.$form).val();
		// Validate input
		if (!label)
			errors.push("You must specify a label.");
		return errors;
	};
	
		
	EditLabelForm.prototype.getSuccessMessage = function(data) {
		return "Label has been successfully edited.";
	};
	
	EditLabelForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while editing the label";
	};
	
	EditLabelForm.prototype.remove = function() {
		AbstractForm.prototype.remove.apply(this);
		if (this.submitSuccessful) {
			this.options.actionHandler.addEvent({
				action : 'RefreshResult',
				target : this.resultObject,
				waitForUpdate : true
			});
		}
	};
	
	return EditLabelForm;
});