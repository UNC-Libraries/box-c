define('EditTitleForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editTitleForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, editTitleForm, ModalLoadingOverlay, AbstractForm) {

        var defaultOptions = {
            title : 'Edit Title',
            createFormTemplate : editTitleForm,
            submitMethod: 'PUT'
        };

        function EditTitleForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        };

        EditTitleForm.prototype.constructor = EditTitleForm;
        EditTitleForm.prototype = Object.create( AbstractForm.prototype);

        EditTitleForm.prototype.preprocessForm = function(resultObject) {
            var newTitle = $("input[name='title']", this.$form).val();
            var pid = resultObject.metadata.id;

            this.action_url = "/services/api/edit/title/" + pid + "?title=" + encodeURIComponent(newTitle);
        };

        EditTitleForm.prototype.validationErrors = function() {
            var errors = [];
            var title = $("input[name='title']", this.$form).val();
            // Validate input
            if (!title)
                errors.push("You must specify a title.");
            return errors;
        };

        EditTitleForm.prototype.getSuccessMessage = function(data) {
            return "Title has been successfully edited.";
        };

        EditTitleForm.prototype.getErrorMessage = function(data) {
            return "An error occurred while editing the title";
        };

        EditTitleForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return EditTitleForm;
    });