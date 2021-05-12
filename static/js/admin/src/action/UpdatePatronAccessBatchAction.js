define('UpdatePatronAccessBatchAction', [ 'jquery', 'AbstractBatchAction'], function($, AbstractBatchAction) {
    function UpdatePatronAccessBatchAction(context) {
        this._create(context);
    };

    UpdatePatronAccessBatchAction.prototype.constructor = UpdatePatronAccessBatchAction;
    UpdatePatronAccessBatchAction.prototype = Object.create( AbstractBatchAction.prototype );

    UpdatePatronAccessBatchAction.prototype.isValidTarget = function(target) {
        return target.isSelected() && target.isEnabled()
            && $.inArray('changePatronAccess', target.metadata.permissions) !== -1
            && target.metadata.type !== 'ContentRoot' && target.metadata.type !== 'AdminUnit';
    };

    UpdatePatronAccessBatchAction.prototype.execute = function() {
        let targets = this.getTargets();

        let perms_editor_data = perms_editor.$children[0].$children[0].$data;
        perms_editor_data.permissionType = 'Patron';
        perms_editor_data.alertHandler = this.context.view.$alertHandler;
        perms_editor_data.actionHandler = this.context.actionHandler;
        if (targets.length == 1) {
            perms_editor_data.resultObject = targets[0];
            perms_editor_data.metadata = targets[0].metadata;
        } else {
            perms_editor_data.resultObjects = targets;
            perms_editor_data.metadata = {
                title: targets.length + " objects",
                id: null,
                type: null
            };
        }
        perms_editor_data.showModal = true;

        AbstractBatchAction.prototype.execute.call(this);
    }

    UpdatePatronAccessBatchAction.prototype.doWork = function() {
    };

    return UpdatePatronAccessBatchAction;
});