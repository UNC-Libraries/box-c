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

        perms_editor_store.commit('setPermissionType', 'Patron');
        perms_editor_store.commit('setAlertHandler', this.context.view.$alertHandler);
        perms_editor_store.commit('setActionHandler', this.context.actionHandler);
        if (targets.length == 1) {
            perms_editor_store.commit('setResultObject', targets[0]);
            perms_editor_store.commit('setResultObjects', null);
            perms_editor_store.commit('setMetadata', targets[0].metadata);
        } else {
            perms_editor_store.commit('setResultObject', null);
            perms_editor_store.commit('setResultObjects', targets);
            perms_editor_store.commit('setMetadata', {
                title: targets.length + " objects",
                id: null,
                type: null
            });
        }
        perms_editor_store.commit('setShowModal', true);

        AbstractBatchAction.prototype.execute.call(this);
    }

    UpdatePatronAccessBatchAction.prototype.doWork = function() {
    };

    return UpdatePatronAccessBatchAction;
});