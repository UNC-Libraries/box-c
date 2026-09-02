define('EditTranscriptForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!templates/admin/editTranscriptForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, editTranscriptForm, ModalLoadingOverlay, AbstractForm) {

        var defaultOptions = {
            title: 'Edit Transcript',
            createFormTemplate: editTranscriptForm,
            submitMethod: 'POST'
        };

        function EditTranscriptForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        };

        EditTranscriptForm.prototype.constructor = EditTranscriptForm;
        EditTranscriptForm.prototype = Object.create(AbstractForm.prototype);

        EditTranscriptForm.prototype.open = function(resultObject) {
            AbstractForm.prototype.open.call(this, resultObject);
        };

        EditTranscriptForm.prototype.preprocessForm = function(resultObject) {
            this.action_url = `/services/api/edit/transcript/${resultObject.metadata.id}`;
        };

        EditTranscriptForm.prototype.validationErrors = function() {
            var errors = [];
            var ref_id = $("input[name='Transcript']", this.$form).val();
            // Validate input
            if (!ref_id)
                errors.push("You must specify a transcript.");
            return errors;
        };

        EditTranscriptForm.prototype.getSuccessMessage = function(data) {
            return "Transcript has been successfully edited.";
        };

        EditTranscriptForm.prototype.getErrorMessage = function(data) {
            return "An error occurred while editing the transcript";
        };

        EditTranscriptForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return EditTranscriptForm;
    });