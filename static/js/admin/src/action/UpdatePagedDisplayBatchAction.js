define('UpdatePagedDisplayBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!../templates/admin/pagedDisplayForm"], function($, AbstractBatchAction, pagedDisplayTemplate, ) {
    function UpdatePagedDisplayBatchAction(context) {
        this._create(context);
    }

    UpdatePagedDisplayBatchAction.prototype.constructor = UpdatePagedDisplayBatchAction;
    UpdatePagedDisplayBatchAction.prototype = Object.create(AbstractBatchAction.prototype);

    UpdatePagedDisplayBatchAction.prototype.isValidTarget = function (target) {
        return target.isSelected() && target.isEnabled()
            && $.inArray('changePatronAccess', target.metadata.permissions) !== -1
            && target.metadata.type === 'Work';
    };

    UpdatePagedDisplayBatchAction.prototype.execute = function() {
        let targets = this.getTargets();
        var UpdatePagedDisplayForm = pagedDisplayTemplate({
            options: { targets: this.formatTargets(targets) },
            metadata: { viewSettings: 'individual' }
        });
        this.dialog = $("<div class='containingDialog'>" + UpdatePagedDisplayForm + "</div>");
        this.dialog.dialog({
            autoOpen: true,
            width: 'auto',
            minWidth: '500',
            height: 'auto',
            modal: true,
            title: `Edit View Settings for ${targets.length} object(s)`
        });
        this.$form = this.dialog.first();

        AbstractBatchAction.prototype.execute.call(this);
    }

    UpdatePagedDisplayBatchAction.prototype.formatTargets = function(targets) {
        return targets.filter(function(d) {
            return d.metadata.type === 'Work';
        }).map(function(d) {
            return d.metadata.id;
        }).join(',');
    }

    UpdatePagedDisplayBatchAction.prototype.doWork = function() {
    };

    return UpdatePagedDisplayBatchAction;
});