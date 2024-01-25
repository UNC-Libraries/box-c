define('PagedDisplayForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/pagedDisplayForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, pagedDisplayForm, ModalLoadingOverlay, AbstractForm) {

        var defaultOptions = {
            title : 'Edit View Settings',
            createFormTemplate : pagedDisplayForm,
            submitMethod: 'PUT'
        };

        function PagedDisplayForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        }

        PagedDisplayForm.prototype.constructor = PagedDisplayForm;
        PagedDisplayForm.prototype = Object.create(AbstractForm.prototype);

        PagedDisplayForm.prototype.preprocessForm = function(resultObject) {
            var newViewSetting = $("#view_settings_change", this.$form).val();
            var pid = resultObject.metadata.id;
            this.action_url = "/services/api/edit/view_settings/" + pid + "?direction=" + encodeURIComponent(newViewSetting);
        };

        PagedDisplayForm.prototype.validationErrors = function() {
            var errors = [];
            var viewSetting = $("#view_settings_change", this.$form).val();
            // Validate input
            if (!viewSetting)
                errors.push("You must specify a view setting.");
            return errors;
        };

        PagedDisplayForm.prototype.getSuccessMessage = function(data) {
            return "View settings settings have been successfully edited.";
        };

        PagedDisplayForm.prototype.getErrorMessage = function(data) {
            return "An error occurred while editing the view settings";
        };

        PagedDisplayForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return PagedDisplayForm;
    });