define('AssignAsThumbnailAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {

    function AssignAsThumbnailAction(context) {
        this._create(context);
    };

    AssignAsThumbnailAction.prototype.constructor = AssignAsThumbnailAction;
    AssignAsThumbnailAction.prototype = Object.create( AjaxCallbackAction.prototype );

    AssignAsThumbnailAction.prototype._create = function(context) {
        this.context = context;

        var options = {
            workMethod: "PUT",
            workPath: "/services/api/edit/assignThumbnail/{idPath}",
            workLabel: "Setting as assigned thumbnail...",
            followupLabel: "Setting as assigned thumbnail...",
            followupPath: "/services/api/status/item/{idPath}/solrRecord/version"
        }

        if ('confirm' in this.context && !this.context.confirm) {
            options.confirm = false;
        } else {
            options.confirm = {
                promptText : "Use this as the assigned thumbnail for its parent?",
                confirmAnchor : this.context.confirmAnchor
            };
        }

        AjaxCallbackAction.prototype._create.call(this, options);
    };

    AssignAsThumbnailAction.prototype.completeState = function() {
        this.context.actionHandler.addEvent({
            action : 'RefreshResult',
            target : this.context.target
        });
        this.context.actionHandler.addEvent({
            action : 'RefreshResult',
            target : this.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId)
        });
        this.alertHandler.alertHandler("success", "Assignment of object \"" + this.context.target.metadata.title + "\" as the assigned thumbnail has completed.");
        this.context.target.enable();
    };

    AssignAsThumbnailAction.prototype.workDone = function(data) {
        this.completeTimestamp = data.timestamp;
        this.oldThumbnailId = data.oldThumbnailId;
        this.newThumbnailId = data.newThumbnailId;
        return true;
    };

    return AssignAsThumbnailAction;
});
