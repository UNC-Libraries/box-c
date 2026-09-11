define('EditAspaceRefIdForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!templates/admin/editAspaceRefIdForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, editAspaceRefIdForm, ModalLoadingOverlay, AbstractForm) {

        var defaultOptions = {
            title: 'Edit Aspace Ref Id',
            createFormTemplate: editAspaceRefIdForm,
            submitMethod: 'POST'
        };

        function EditAspaceRefIdForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        };

        EditAspaceRefIdForm.prototype.constructor = EditAspaceRefIdForm;
        EditAspaceRefIdForm.prototype = Object.create(AbstractForm.prototype);

        EditAspaceRefIdForm.prototype.preprocessForm = function(resultObject) {
            this.action_url = `/services/api/edit/aspace/updateRefId/${resultObject.metadata.id}`;
        };

        EditAspaceRefIdForm.prototype.validationErrors = function() {
            var errors = [];
            var ref_id = $("input[name='aspaceRefId']", this.$form).val();
            // Validate input
            if (!ref_id)
                errors.push("You must specify an Aspace Ref Id.");
            return errors;
        };

        EditAspaceRefIdForm.prototype.getSuccessMessage = function(data) {
            return "Aspace Ref Id has been successfully edited.";
        };

        EditAspaceRefIdForm.prototype.getErrorMessage = function(data) {
            return "An error occurred while editing the Aspace Ref Id";
        };

        EditAspaceRefIdForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return EditAspaceRefIdForm;
    });