define('DeleteStreamingPropertiesResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {

    function DeleteStreamingPropertiesResultAction(context) {
        this._create(context);
    };

    DeleteStreamingPropertiesResultAction.prototype.constructor = DeleteStreamingPropertiesResultAction;
    DeleteStreamingPropertiesResultAction.prototype = Object.create( AjaxCallbackAction.prototype );

    DeleteStreamingPropertiesResultAction.prototype._create = function(context) {
        this.context = context;

        var options = {
            workMethod: "PUT",
            workPath: `/services/api/edit/streamingProperties?id=${this.context.target.metadata.id}&action=delete`,
            workLabel: "Deleting streaming properties..",
            followupLabel: "Deleting streaming properties...",
            followupPath: `/services/api/status/item/${this.context.target.metadata.id}/solrRecord/version`
        }

        if ('confirm' in this.context && !this.context.confirm) {
            options.confirm = false;
        } else {
            options.confirm = {
                promptText : "Clear streaming properties?",
                confirmAnchor : this.context.confirmAnchor
            };
        }

        AjaxCallbackAction.prototype._create.call(this, options);
    };

    DeleteStreamingPropertiesResultAction.prototype.completeState = function() {
        this.context.actionHandler.addEvent({
            action : 'RefreshResult',
            target : this.context.target
        });
        this.alertHandler.alertHandler("success", "Cleared streaming properties.");
        this.context.target.enable();
    };

    DeleteStreamingPropertiesResultAction.prototype.followup = function(data) {
        if (data) {
            return this.context.target.updateVersion(data);
        }
        return false;
    };

    return DeleteStreamingPropertiesResultAction;
});