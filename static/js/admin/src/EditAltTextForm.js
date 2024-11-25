define('EditAltTextForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editAltTextForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, editAltTextForm, ModalLoadingOverlay, AbstractForm) {

        var defaultOptions = {
            title : 'Edit Alt Text',
            createFormTemplate : editAltTextForm,
            submitMethod: 'POST'
        };

        function EditAltTextForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        }

        EditAltTextForm.prototype.constructor = EditAltTextForm;
        EditAltTextForm.prototype = Object.create( AbstractForm.prototype );

        EditAltTextForm.prototype.open = function(resultObject) {
            AbstractForm.prototype.open.call(this, resultObject);
        };

        EditAltTextForm.prototype.preprocessForm = function(resultObject) {
            var newText = $("textarea[name='altText']", this.$form).val();
            var pid = resultObject.metadata.id;

            this.action_url = "/services/api/edit/altText/" + pid + "?altText=" + encodeURIComponent(newText);
        };

        EditAltTextForm.prototype.validationErrors = function() {
            return [];
        };

        EditAltTextForm.prototype.getSuccessMessage = function(data) {
            return "Alt text has been successfully updated.";
        };

        EditAltTextForm.prototype.getErrorMessage = function(data) {
            return "An error occurred while updating the alt text";
        };

        EditAltTextForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return EditAltTextForm;
    });