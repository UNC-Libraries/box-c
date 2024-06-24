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
            this.action_url = `/services/api/edit/streamingProperties?id=${resultObjId}&url=${encodeURIComponent(streamingUrl)}&action=add`;
        }

        EditStreamingPropertiesForm.prototype.validationErrors = function(resultObject) {
            /**
               If you change this value it also needs to be updated in
               operations-jms/src/main/java/edu/unc/lib/boxc/operations/jms/streaming/StreamingPropertiesRequest.java
            **/
            const STREAMREAPER_PREFIX_URL = "https://durastream.lib.unc.edu/player"
            let errors = [];
            let streamingUrl = $('#streaming_url', this.$form).val();

            // Validate input
            if (!streamingUrl.startsWith(STREAMREAPER_PREFIX_URL)) {
                errors.push(`You must specify a DuraStream based streaming URL, e.g. ${STREAMREAPER_PREFIX_URL}`);
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