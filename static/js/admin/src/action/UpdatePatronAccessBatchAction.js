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

        perms_editor_store.setPermissionType('Patron');
        perms_editor_store.setAlertHandler(this.context.view.$alertHandler);
        perms_editor_store.setActionHandler(this.context.actionHandler);
        if (targets.length == 1) {
            perms_editor_store.setResultObject(targets[0]);
            perms_editor_store.setResultObjects(null);
            perms_editor_store.setMetadata(targets[0].metadata);
        } else {
            perms_editor_store.setResultObject(null);
            perms_editor_store.setResultObjects(targets);
            perms_editor_store.setMetadata({
                title: targets.length + " objects",
                id: null,
                type: null
            });
        }
        perms_editor_store.setShowModal(true);

        AbstractBatchAction.prototype.execute.call(this);
    }

    UpdatePatronAccessBatchAction.prototype.doWork = function() {
    };

    return UpdatePatronAccessBatchAction;
});