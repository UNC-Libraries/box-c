define('EditThumbnailForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editThumbnailForm',
        'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'],
    function($, ui, _, RemoteStateChangeMonitor, addWorkTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {

        var defaultOptions = {
            title : 'Add File',
            createFormTemplate : addWorkTemplate
        };

        function EditThumbnailForm(options) {
            this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
        }

        EditThumbnailForm.prototype.constructor = EditThumbnailForm;
        EditThumbnailForm.prototype = Object.create( AbstractFileUploadForm.prototype );

        // Validate the form and retrieve any errors
        EditThumbnailForm.prototype.validationErrors = function() {
            var errors = [];
            var dataFile = $("input[type='file']", this.$form).val();
            if (!dataFile)
                errors.push("You must select a file to ingest");
            return errors;
        };

        EditThumbnailForm.prototype.getSuccessMessage = function(data) {
            return this.ingestFile.name + " has been successfully uploaded for collection thumbnail creation.";
        };

        EditThumbnailForm.prototype.getErrorMessage = function(data) {
            var message = "Failed to process file " + this.ingestFile.name + ".";
            if (data && data.errorStack && !this.closed) {
                message += "  See errors below.";
                this.setError(data.errorStack);
            }
            return message;
        };

        return EditThumbnailForm;
    });