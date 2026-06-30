define('AggregatePdfAction', [ 'jquery', 'AjaxCallbackAction' ], function($, AjaxCallbackAction) {
    function AggregatePdfAction(context) {
        this._create(context);
    };

    AggregatePdfAction.prototype = Object.create(AjaxCallbackAction.prototype);
    AggregatePdfAction.prototype.constructor = AggregatePdfAction;

    AggregatePdfAction.prototype.actionName = "AggregatePdf";

    AggregatePdfAction.prototype._create = function(context) {
        this.context = context;

        var options = {
            workLabel: "Creating aggregate PDF...",
            workMethod: "post",
            workPath: "/services/api/edit/aggregatePdf",
            workData: {
                ids: this.context.target.metadata.id
            },
            confirm: 'confirm' in this.context && !this.context.confirm ? false : {
                promptText: "Generate aggregate PDF for this work?",
                confirmAnchor: this.context.confirmAnchor,
                dialogOptions: {
                    width: 400,
                    modal: true,
                    position: 'center'
                }
            },
            followup: false
        };

        AjaxCallbackAction.prototype._create.call(this, options);
    };

    AggregatePdfAction.prototype.complete = function(data) {
        if (this.context.target && this.context.target.metadata) {
            this.alertHandler.alertHandler(
                "success",
                "Generating aggregate PDF for " + this.context.target.metadata.id
            );
        } else {
            this.alertHandler.alertHandler("success", "Generating aggregate PDF");
        }
    };

    return AggregatePdfAction;
});