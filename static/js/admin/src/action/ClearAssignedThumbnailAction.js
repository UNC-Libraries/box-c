define('ClearAssignedThumbnailAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {

    function ClearAssignedThumbnailAction(context) {
        this._create(context);
    };

    ClearAssignedThumbnailAction.prototype.constructor = ClearAssignedThumbnailAction;
    ClearAssignedThumbnailAction.prototype = Object.create( AjaxCallbackAction.prototype );

    ClearAssignedThumbnailAction.prototype._create = function(context) {
        this.context = context;

        var options = {
            workMethod: "DELETE",
            workPath: "/services/api/edit/deleteThumbnail/{idPath}",
            workLabel: "Clearing assigned thumbnail...",
            followupLabel: "Clearing assigned thumbnail...",
            followupPath: "/services/api/status/item/{idPath}/solrRecord/version"
        }

        if ('confirm' in this.context && !this.context.confirm) {
            options.confirm = false;
        } else {
            options.confirm = {
                promptText : "Clear the assigned thumbnail?",
                confirmAnchor : this.context.confirmAnchor
            };
        }

        AjaxCallbackAction.prototype._create.call(this, options);
    };

    ClearAssignedThumbnailAction.prototype.completeState = function() {
        this.context.actionHandler.addEvent({
            action : 'RefreshResult',
            target : this.context.target
        });
        this.alertHandler.alertHandler("success", "Cleared the assigned thumbnail.");
        this.context.target.enable();
    };

    ClearAssignedThumbnailAction.prototype.workDone = function(data) {
        this.completeTimestamp = data.timestamp;
        this.oldThumbnailId = data.oldThumbnailId;
        if (this.context.target.metadata.type == "Work") {
            var oldThumbnail = this.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId);
            if (oldThumbnail != null) {
                this.context.actionHandler.addEvent({
                    action : 'RefreshResult',
                    target : oldThumbnail,
                    waitForUpdate : true
                });
            }
        }
        return true;
    };

    ClearAssignedThumbnailAction.prototype.followup = function(data) {
        if (data) {
            return this.context.target.updateVersion(data);
        }
        return false;
    };

    return ClearAssignedThumbnailAction;
});