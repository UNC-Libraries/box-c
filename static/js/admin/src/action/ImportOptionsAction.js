define('ImportOptionsAction', [ 'jquery', 'AbstractBatchAction', 'ImportMenu'], function($, AbstractBatchAction, ImportMenu) {

    function ImportOptionsAction(context) {
        this._create(context);
    }

    ImportOptionsAction.prototype.constructor = ImportOptionsAction;
    ImportOptionsAction.prototype = Object.create(AbstractBatchAction.prototype);

    ImportOptionsAction.prototype.getTargets = function() {
        let self = this;
        if (self.context.targets) {
            return self.context.targets;
        }
        return AbstractBatchAction.prototype.getTargets.call(this);
    };

    ImportOptionsAction.prototype.execute = function() {
        let self = this;
        let targets = self.getTargets();

        if (this.importMenu) {
            this.importMenu.setContainer(targets[0]).refresh();
        } else {
            this.importMenu = new ImportMenu({
                container: targets[0],
                selector: ".ImportOptions_selected",
                alertHandler: self.context.view.$alertHandler,
                actionHandler: self.context.actionHandler,
                targets: targets
            });
        }
    }

    return ImportOptionsAction;
});