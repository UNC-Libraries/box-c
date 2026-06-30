define('AggregatePdfBatchAction', [ 'jquery', 'AbstractBatchAction'], function($, AbstractBatchAction) {

    function AggregatePdfBatchAction(context) {
        this._create(context);
    }

    AggregatePdfBatchAction.prototype = Object.create(AbstractBatchAction.prototype);
    AggregatePdfBatchAction.prototype.constructor = AggregatePdfBatchAction;

    AggregatePdfBatchAction.prototype.isValidTarget = function(target) {
        return target.isSelected()
            && target.isEnabled()
            && target.metadata
            && $.inArray("editResourceType", target.metadata.permissions) !== -1
            && target.metadata.type === "Work";
    };

    AggregatePdfBatchAction.prototype.execute = function() {
        var aggregateList = $("<ul class='confirm_selected_list'></ul>");

        this.targets = this.getTargets();

        // Only one item selected: use the normal single-object action.
        if (this.targets.length === 1) {
            this.actionHandler.addEvent({
                action: 'AggregatePdf',
                target: this.targets[0],
                confirm: true,
                confirmAnchor: this.context.anchor
            });
            return;
        }

        // Add valid targets to the confirmation text.
        for (var index in this.targets) {
            var resultObject = this.targets[index];
            aggregateList.append("<li>" + resultObject.metadata.id + "</li>");
        }

        var message = $("<div></div>");
        message.append("<h3>Generate aggregate PDFs for " + aggregateList.children().length
            + " selected work object" + (aggregateList.children().length !== 1 ? "s" : "") + "?</h3>"
        );

        message.append(
            "<p>This will start aggregate PDF generation for the selected work objects.</p>"
        );

        message.append(aggregateList);

        this.context.confirm = {
            promptText: message,
            confirmText: 'Confirm',
            confirmAnchor: this.context.anchor,
            dialogOptions: {
                width: 400,
                modal: true,
                position: 'center'
            }
        };

        AbstractBatchAction.prototype.execute.call(this);
    };

    AggregatePdfBatchAction.prototype.doWork = function() {
        for (var index in this.targets) {
            this.actionHandler.addEvent({
                action: 'AggregatePdf',
                target: this.targets[index],
                confirm: false
            });
        }
    };

    return AggregatePdfBatchAction;
});