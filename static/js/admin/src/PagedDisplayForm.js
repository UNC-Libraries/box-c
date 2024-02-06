define('PagedDisplayForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/pagedDisplayForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, pagedDisplayForm, ModalLoadingOverlay, AbstractForm) {

        let defaultOptions = {
            title : 'Edit View Settings',
            createFormTemplate : pagedDisplayForm,
            submitMethod: 'PUT'
        };

        function PagedDisplayForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        }

        PagedDisplayForm.prototype.constructor = PagedDisplayForm;
        PagedDisplayForm.prototype = Object.create(AbstractForm.prototype);

        PagedDisplayForm.prototype.preprocessForm = function() {
            let newViewSetting = $('#view_settings_change', this.$form).val();
            let pids = $('#paged_display_targets', this.$form).val();
            this.action_url = `/services/api/edit/view_settings?targets=${encodeURIComponent(pids)}&direction=${encodeURIComponent(newViewSetting)}`;
        };

        PagedDisplayForm.prototype.validationErrors = function(resultObject) {
            let errors = [];
            let viewSetting = $('#view_settings_change', this.$form).val();
            // Validate input
            if (!viewSetting)
                errors.push('You must specify a view setting.');
            return errors;
        };

        PagedDisplayForm.prototype.getSuccessMessage = function(data) {
            return 'View settings settings have been successfully edited.';
        };

        PagedDisplayForm.prototype.getErrorMessage = function(data) {
            return 'An error occurred while editing the view settings';
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