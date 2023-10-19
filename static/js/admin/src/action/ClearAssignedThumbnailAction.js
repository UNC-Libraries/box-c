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

    ClearAssignedThumbnailAction.prototype.performWork = function(workMethod, workData) {
        this.workState();
        var op = this;

        $.ajax({
            type: workMethod,
            url: this.workURL,
            dataType: "json",
            data: workData
        }).done(function(data) {
            this.oldThumbnailId = data.oldThumbnailId;
            if (op.options.followup) {
                try {
                    var workSuccessful = op.workDone(data);

                    if (!workSuccessful)
                        throw "Operation was unsuccessful";
                } catch (e) {
                    op.alertHandler.alertHandler('error', e.message);
                    if (typeof console == "object")
                        console.error(e.message, e.error);
                    if (op.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId))
                        op.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId).setState("failed");
                    return;
                }
                if (op.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId))
                    op.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId).setState("followup");
                op.followupMonitor.performPing();
            } else {
                if (op.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId))
                    op.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId).setState("idle");
                op.complete(data);
            }

            // Trigger unchecking all checked objects when action completes
            $(".select_all").trigger('click', function(){
                var resultObjects = op.resultObjectList.resultObjects;
                for (var index in resultObjects) {
                    resultObjects[index]["unselect"]();
                }
                op.selectionUpdated();
            }).children("input").prop("checked", false);

        }).fail(function(data, error) {
            op.alertHandler.alertHandler('error', data.responseText + ", " + error);
            console.error(data.responseText, error);
        });
    };

    ClearAssignedThumbnailAction.prototype.completeState = function() {
        this.context.actionHandler.addEvent({
            action : 'RefreshResult',
            target : this.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId)
        });
        this.alertHandler.alertHandler("success", "Cleared the assigned thumbnail.");
        this.context.target.enable();
    };

    ClearAssignedThumbnailAction.prototype.workDone = function(data) {
        this.completeTimestamp = data.timestamp;
        this.oldThumbnailId = data.oldThumbnailId;
        return true;
    };

    ClearAssignedThumbnailAction.prototype.followup = function(data) {
        if (data) {
            return this.context.resultTable.resultObjectList.getResultObject(this.oldThumbnailId).updateVersion(data);
        }
        return false;
    };

    return ClearAssignedThumbnailAction;
});