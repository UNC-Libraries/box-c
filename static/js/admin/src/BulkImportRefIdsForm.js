define('BulkImportRefIdsForm', [ 'jquery', 'jquery-ui', 'underscore', 'tpl!templates/admin/bulkImportRefIdsForm',
        'ModalLoadingOverlay', 'AbstractFileUploadForm'],
    function($, ui, _, importTemplate, ModalLoadingOverlay, AbstractFileUploadForm) {

        var defaultOptions = {
            title : 'Bulk Import Ref Ids',
            createFormTemplate : importTemplate
        };

        function BulkImportRefIdsForm(options) {
            this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
        }

        BulkImportRefIdsForm.prototype.constructor = BulkImportRefIdsForm;
        BulkImportRefIdsForm.prototype = Object.create( AbstractFileUploadForm.prototype );

        BulkImportRefIdsForm.prototype.preprocessForm = function() {
            var label = $("input[name='name']", this.$form).val();
            if (!label && this.ingestFile) {
                $("input[name='name']", this.$form).val(this.ingestFile.name);
            }
        };

        // Validate the form and retrieve any errors
        BulkImportRefIdsForm.prototype.validationErrors = function() {
            var errors = [];
            var dataFile = $("input[type='file']", this.$form).val();
            if (!dataFile)
                errors.push("You must select a file to import");
            return errors;
        };

        BulkImportRefIdsForm.prototype.getSuccessMessage = function(data) {
            return this.ingestFile.name + " has been successfully uploaded for updating.  You will receive an email when it completes.";
        };

        BulkImportRefIdsForm.prototype.getErrorMessage = function(data) {
            var message = "Failed to import file " + this.ingestFile.name + ".";
            if (data && data.errorStack && !this.closed) {
                message += "  See errors below.";
                this.setError(data.errorStack);
            }
            return message;
        };

        return BulkImportRefIdsForm;
    });