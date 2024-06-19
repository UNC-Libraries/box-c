define('EditStreamingPropertiesForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/editStreamingPropertiesForm',
        'ModalLoadingOverlay', 'AbstractForm', 'AlertHandler'],
    function($, ui, _, RemoteStateChangeMonitor, editStreamingPropertiesForm, ModalLoadingOverlay, AbstractForm) {
        let defaultOptions = {
            title : 'Edit Streaming Properties',
            createFormTemplate : editStreamingPropertiesForm,
            submitMethod: 'PUT'
        };

        function EditStreamingPropertiesForm(options) {
            this.options = $.extend({}, defaultOptions, options);
        }

        EditStreamingPropertiesForm.prototype.constructor = EditStreamingPropertiesForm;
        EditStreamingPropertiesForm.prototype = Object.create( AbstractForm.prototype );

        EditStreamingPropertiesForm.prototype.preprocessForm = function() {
            let resultObjId = $('#streaming_file_id', this.$form).val();
            let streamingUrl = $('#streaming_url', this.$form).val();
            let isDeletion = $('#delete_streaming_settings', this.$form).is(':checked');
            let action = (isDeletion) ? 'delete' : 'add';
            this.action_url = `/services/api/edit/streamingProperties?id=${resultObjId}&url=${encodeURIComponent(streamingUrl)}&action=${action}`;
        }

        EditStreamingPropertiesForm.prototype.validationErrors = function(resultObject) {
            let errors = [];
            let streamingUrl = $('#streaming_url', this.$form).val();
            let isDeletion = $('#delete_streaming_settings', this.$form).is(':checked');

            // Validate input
            if (!isDeletion && !streamingUrl) {
                errors.push('You must specify a streaming URL.');
            }

            return errors;
        };

        EditStreamingPropertiesForm.prototype.getSuccessMessage = function(data) {
            return 'Streaming URL has been successfully edited.';
        };

        EditStreamingPropertiesForm.prototype.getErrorMessage = function(data) {
            return 'An error occurred while editing the streaming URL';
        };

        EditStreamingPropertiesForm.prototype.remove = function() {
            AbstractForm.prototype.remove.apply(this);
            if (this.submitSuccessful) {
                this.options.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : this.resultObject,
                    waitForUpdate : true
                });
            }
        };

        return EditStreamingPropertiesForm;
    });