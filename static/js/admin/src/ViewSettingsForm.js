define('ViewSettingsForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/viewSettingsForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, viewSettingsForm, ModalLoadingOverlay, AbstractForm) {

        let defaultOptions = {
            title : 'Edit View Settings',
            createFormTemplate : viewSettingsForm,
            submitMethod: 'PUT'
        };

        function ViewSettingsForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        }

        ViewSettingsForm.prototype.constructor = ViewSettingsForm;
        ViewSettingsForm.prototype = Object.create(AbstractForm.prototype);

        ViewSettingsForm.prototype.preprocessForm = function() {
            let newViewSetting = $('#view_settings_change', this.$form).val();
            let pids = $('#view_settings_targets', this.$form).val();
            this.action_url = `/services/api/edit/view_settings?targets=${encodeURIComponent(pids)}&view_setting=${encodeURIComponent(newViewSetting)}`;
        };

        ViewSettingsForm.prototype.validationErrors = function(resultObject) {
            let errors = [];
            let viewSetting = $('#view_settings_change', this.$form).val();
            // Validate input
            if (!viewSetting)
                errors.push('You must specify a view setting.');
            return errors;
        };

        ViewSettingsForm.prototype.getSuccessMessage = function(data) {
            return 'View settings settings have been successfully edited.';
        };

        ViewSettingsForm.prototype.getErrorMessage = function(data) {
            return 'An error occurred while editing the view settings';
        };

        ViewSettingsForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return ViewSettingsForm;
    });