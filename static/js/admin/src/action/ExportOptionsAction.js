define('ExportOptionsAction', [ 'jquery', 'AbstractBatchAction', 'ExportMenu', 'cycle'], function($, AbstractBatchAction, ExportMenu) {

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
        let targets = JSON.stringify(JSON.decycle(self.getTargets()));
        sessionStorage.setItem('exportTargets', targets);

        new ExportMenu({
            selector: ".ExportOptions_selected",
            alertHandler: self.context.view.$alertHandler,
            actionHandler: self.context.actionHandler
        });
    }

    return ExportOptionsAction;
});