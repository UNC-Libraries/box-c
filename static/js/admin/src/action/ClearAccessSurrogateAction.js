define('ClearAccessSurrogateAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {

    function ClearAccessSurrogateAction(context) {
        this._create(context);
    }

    ClearAccessSurrogateAction.prototype.constructor = ClearAccessSurrogateAction;
    ClearAccessSurrogateAction.prototype = Object.create( AjaxCallbackAction.prototype );

    ClearAccessSurrogateAction.prototype._create = function(context) {
        this.context = context;

        var options = {
            workMethod: "DELETE",
            workPath: "/services/api/edit/accessSurrogate/{idPath}",
            workLabel: "Clearing assigned access surrogate...",
            followupLabel: "Clearing assigned access surrogate...",
            followupPath: "/services/api/status/item/{idPath}/solrRecord/version"
        }

        if ('confirm' in this.context && !this.context.confirm) {
            options.confirm = false;
        } else {
            options.confirm = {
                promptText : "Clear the assigned access surrogate?",
                confirmAnchor : this.context.confirmAnchor
            };
        }

        AjaxCallbackAction.prototype._create.call(this, options);
    };

    ClearAccessSurrogateAction.prototype.completeState = function() {
        this.context.actionHandler.addEvent({
            action : 'RefreshResult',
            target : this.context.target
        });
        this.alertHandler.alertHandler("success", "Cleared the assigned access surrogate.");
        this.context.target.enable();
    };

    ClearAccessSurrogateAction.prototype.followup = function(data) {
        if (data) {
            return this.context.target.updateVersion(data);
        }
        return false;
    };

    return ClearAccessSurrogateAction;
});