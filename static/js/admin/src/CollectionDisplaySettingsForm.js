define('CollectionDisplaySettingsForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!templates/admin/collectionDisplaySettingsForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, collectionDisplaySettingsForm, ModalLoadingOverlay, AbstractForm) {

        let defaultOptions = {
            title : 'Edit Collection Display Settings',
            createFormTemplate : collectionDisplaySettingsForm,
            submitMethod: 'PUT'
        };

        function CollectionDisplaySettingsForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        }

        CollectionDisplaySettingsForm.prototype.constructor = CollectionDisplaySettingsForm;
        CollectionDisplaySettingsForm.prototype = Object.create(AbstractForm.prototype);

        CollectionDisplaySettingsForm.prototype.preprocessForm = function() {
            let collectionId = $("#collection_id", this.form).val();
            let sortType = $('#sort_type', this.$form).val();
            let displayWorksOnly = $('#display_works_only', this.$form).prop('checked');
            let displayType = $('input[name="display_type"]:checked', this.$form).val();
            this.action_url = `/services/api/edit/collectionDisplay?id=${collectionId}&sortType=${encodeURIComponent(sortType)}&worksOnly=${encodeURIComponent(displayWorksOnly)}&displayType=${encodeURIComponent(displayType)}`;
        };

        CollectionDisplaySettingsForm.prototype.validationErrors = function(resultObject) {}

        CollectionDisplaySettingsForm.prototype.getSuccessMessage = function(data) {
            return 'Collection display settings have been successfully edited.';
        };

        CollectionDisplaySettingsForm.prototype.getErrorMessage = function(data) {
            return 'An error occurred while editing the collection display settings';
        };

        CollectionDisplaySettingsForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return CollectionDisplaySettingsForm;
    });