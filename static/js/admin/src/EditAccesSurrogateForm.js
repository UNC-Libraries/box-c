define('EditAccessSurrogateForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editAccessSurrogateForm',
        'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'],
    function($, ui, _, RemoteStateChangeMonitor, addWorkTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {
        let defaultOptions = {
            title : 'Edit Access Surrogate',
            createFormTemplate : addWorkTemplate
        };

        function EditAccessSurrogateForm(options) {
            this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
            this.submitSuccessful = false;
        }

        EditAccessSurrogateForm.prototype.constructor = EditAccessSurrogateForm;
        EditAccessSurrogateForm.prototype = Object.create( AbstractFileUploadForm.prototype );

        // Validate the form and retrieve any errors
        EditAccessSurrogateForm.prototype.validationErrors = function() {
            let errors = [];
            let dataFile = $("input[type='file']", this.$form).val();
            if (!dataFile)
                errors.push("You must select a file to ingest");
            return errors;
        };

        EditAccessSurrogateForm.prototype.getSuccessMessage = function(data) {
            this.submitSuccessful = true;
            return this.ingestFile.name + " has been successfully uploaded for access surrogate creation.";
        };

        EditAccessSurrogateForm.prototype.getErrorMessage = function(data) {
            let message = "Failed to process file " + this.ingestFile.name + ".";
            if (data && data.errorStack && !this.closed) {
                message += "  See errors below.";
                this.setError(data.errorStack);
            }
            return message;
        };

        EditAccessSurrogateForm.prototype.remove = function() {
            AbstractFileUploadForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return EditAccessSurrogateForm;
    });