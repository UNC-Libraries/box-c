define('AggregatePdfAction', [ 'jquery', 'AbstractBatchAction' ], function($, AbstractBatchAction) {

    function AggregatePdfAction(context) {
        this._create(context);
    };

    AggregatePdfAction.prototype.constructor = AggregatePdfAction;
    AggregatePdfAction.prototype = Object.create( AbstractBatchAction.prototype );

    AggregatePdfAction.prototype.isValidTarget = function(target) {
        return target.isSelected() && target.isEnabled()
            && $.inArray("reindex", target.metadata.permissions) !== -1
            && "Work" === target.getMetadata().type;
    };

        AggregatePdfAction.prototype.getTargets = function(targets) {
        if (this.context.targets) {
            return this.context.targets;
        }
        return AbstractBatchAction.prototype.getTargets.call(this);
    };

    AggregatePdfAction.prototype.execute = function() {
        var self = this;

        this.targets = this.getTargets();

        var title;
        var promptText;

        if (this.targets.length === 1) {
            title = "Create aggregate PDF";
            promptText = "Create aggregate PDFs for " + this.targets[0].metadata.title.substring(0, 30) + "?";
        } else {
            title = "Create aggregate PDFs";
            promptText = "Create aggregate PDFs for " + this.targets.length + " objects?";
        }

        this.dialog = $("<div class='containingDialog'>" + promptText + "</div>");

        this.dialog.dialog({
            autoOpen: true,
            width: 'auto',
            minWidth: '500',
            modal: true,
            title: title,
            buttons: {
                "Create PDFs": function() {
                    self.dialog.dialog("close");
                    self._submitAggregatePdfRequest();
                },
                "Cancel": function() {
                    self.dialog.dialog("close");
                    self.dialog.remove();
                }
            },
            close: function() {
                self.dialog.remove();
            }
        });
    };

    AggregatePdfAction.prototype._submitAggregatePdfRequest = function() {
        var self = this;

        var pids = [];
        for (var index in self.targets) {
            pids.push(self.targets[index].getPid());
        }

        $.ajax({
            url: "/services/api/edit/aggregatePdf",
            type: "POST",
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            data: JSON.stringify({
                pids: pids.join("\n")
            })
        }).done(function(response) {
            self.context.view.$alertHandler.alertHandler("message", "Aggregate PDF generation of "
                + self.targets.length + " object(s) has started.");
        }).fail(function() {
            self.context.view.$alertHandler.alertHandler("error", "Failed to generate aggregate PDF for "
                + self.targets.length + " object(s)");
        });
    };

    return AggregatePdfAction;
});