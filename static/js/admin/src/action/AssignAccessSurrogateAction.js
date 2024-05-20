define('AssignAccessSurrogateAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {

    function AssignAccessSurrogateAction(context) {
        this._create(context);
    };

    AssignAccessSurrogateAction.prototype.constructor = AssignAccessSurrogateAction;
    AssignAccessSurrogateAction.prototype = Object.create( AjaxCallbackAction.prototype );

    AssignAccessSurrogateAction.prototype._create = function(context) {
        this.context = context;

        var options = {
            workMethod: "PUT",
            workPath: "/services/api/edit/assignAccessSurrogate/{idPath}",
            workLabel: "Setting as assigned access surrogate...",
            followupLabel: "Setting as assigned access surrogate...",
            followupPath: "/services/api/status/item/{idPath}/solrRecord/version"
        }

        if ('confirm' in this.context && !this.context.confirm) {
            options.confirm = false;
        } else {
            options.confirm = {
                promptText : "Use this as the access surrogate for this file?",
                confirmAnchor : this.context.confirmAnchor
            };
        }

        AjaxCallbackAction.prototype._create.call(this, options);
    };

    AssignAccessSurrogateAction.prototype.completeState = function() {
        this.context.actionHandler.addEvent({
            action : 'RefreshResult',
            target : this.context.target
        });
        this.alertHandler.alertHandler("success", `Assignment of object "${this.context.target.metadata.title}" as the assigned access surrogate has completed.`);
        this.context.target.enable();
    };

    AssignAccessSurrogateAction.prototype.followup = function(data) {
        if (data) {
            return this.context.target.updateVersion(data);
        }
        return false;
    };

    return AssignAccessSurrogateAction;
});
