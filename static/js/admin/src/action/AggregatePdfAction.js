define('AggregatePdfAction', [ 'jquery', 'AbstractBatchAction', "tpl!templates/admin/aggregatePdfForm"],
    function($, AbstractBatchAction, aggregatePdfTemplate) {

    function AggregatePdfAction(context) {
        this._create(context);
    };

    AggregatePdfAction.prototype.constructor = AggregatePdfAction;
    AggregatePdfAction.prototype = Object.create( AbstractBatchAction.prototype );

    AggregatePdfAction.prototype.isValidTarget = function(target) {
        return target.isSelected() && target.isEnabled() && $.inArray("editResourceType", target.metadata.permissions) != -1
            && "Folder" == target.getMetadata().type;
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
        if (this.targets.length == 1) {
            title = "Create aggregrate PDFs for " + this.targets[0].metadata.title.substring(0, 30);
        } else {
            title = "Create aggregate PDFs for " + this.targets.length + " objects";
        }

        var aggregatePdfForm = aggregatePdfTemplate();
        this.dialog = $("<div class='containingDialog'>" + aggregatePdfForm + "</div>");
        this.dialog.dialog({
            autoOpen: true,
            width: 'auto',
            minWidth: '500',
            modal: true,
            title: title
        });
        this.$form = this.dialog.first();

        this.$form.submit(function(e){
            var pids = [];
            for (var index in self.targets) {
                pids += this.targets[index].getPid() + "\n";
            }

            $.ajax({
                url : "/services/api/edit/aggregatePdf",
                type : "POST",
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                data : JSON.stringify({
                    pids : pids,
                })
            }).done(function(response) {
                self.context.view.$alertHandler.alertHandler("message", "Aggregate PDF generation of "
                    + self.targets.length + " object(s) has started.");
                self.dialog.remove();
            }).fail(function() {
                self.context.view.$alertHandler.alertHandler("error", "Failed to generate aggregate PDF for "
                    + self.targets.length + " object(s)");
            });

            e.preventDefault();
        });
    }

    return AggregatePdfAction;
});
