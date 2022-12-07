define('ExportOptionsAction', [ 'jquery', 'AbstractBatchAction', 'ExportMenu'], function($, AbstractBatchAction, ExportMenu) {

    function ExportOptionsAction(context) {
        this._create(context);
    }

    ExportOptionsAction.prototype.constructor = ExportOptionsAction;
    ExportOptionsAction.prototype = Object.create(AbstractBatchAction.prototype);

    ExportOptionsAction.prototype.getTargets = function() {
        return AbstractBatchAction.prototype.getTargets.call(this);
    };

    ExportOptionsAction.prototype.execute = function() {
        let self = this;
        let targets = self.getTargets();

        if (this.exportMenu) {
            this.exportMenu.setContainer(targets[0]).refresh();
        } else {
            this.exportMenu = new ExportMenu({
                container: targets[0],
                selector: ".ExportOptions_selected",
                alertHandler: self.context.view.$alertHandler,
                actionHandler: self.context.actionHandler,
                targets: targets
            });
        }
    }

    return ExportOptionsAction;
});