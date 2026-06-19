define('EditWcagComplianceForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor',
        'tpl!templates/admin/editWcagComplianceForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, editWcagComplianceForm, ModalLoadingOverlay, AbstractForm) {
        var defaultOptions = {
            title : 'Edit WCAG Compliance Level',
            createFormTemplate : editWcagComplianceForm,
            submitMethod: 'PUT'
        };

        function EditWcagComplianceForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        }

        EditWcagComplianceForm.prototype.constructor = EditWcagComplianceForm;
        EditWcagComplianceForm.prototype = Object.create(AbstractForm.prototype);

        EditWcagComplianceForm.prototype.preprocessForm = function(resultObject) {
            var pid = resultObject.metadata.id;
            this.action_url = "/services/api/edit/wcag/" + pid;
        };

        EditWcagComplianceForm.prototype.validationErrors = function() {
            var errors = [];
            var wcagComplianceLevel = $("#edit_wcag_compliance", this.$form).val();
            // Validate input
            if (wcagComplianceLevel === "")
                errors.push("You must specify a WCAG compliance level.");
            return errors;
        };

        EditWcagComplianceForm.prototype.getSuccessMessage = function(data) {
            return "WCAG compliance level has been successfully edited.";
        };

        EditWcagComplianceForm.prototype.getErrorMessage = function(data) {
            return "An error occurred while editing the WCAG compliance level";
        };

        EditWcagComplianceForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return EditWcagComplianceForm;
    });