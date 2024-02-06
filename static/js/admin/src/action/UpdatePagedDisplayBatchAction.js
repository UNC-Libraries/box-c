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
        let self = this;
        let targets = this.getTargets();
        let UpdatePagedDisplayForm = pagedDisplayTemplate({
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
        this.$form.submit(function(e) {
            let newViewSetting = $('#view_settings_change', self.$form).val();
            let pids = $('#paged_display_targets', self.$form).val();

            $.ajax({
                url: `/services/api/edit/view_settings?targets=${encodeURIComponent(pids)}&direction=${encodeURIComponent(newViewSetting)}`,
                type: 'PUT',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json'
            }).done(function (response) {
                self.context.view.$alertHandler.alertHandler('message', response.message);
                self.dialog.remove();
            }).fail(function () {
                self.context.view.$alertHandler.alertHandler('error', `Failed to update view settings ${targets.length}objects`);
            });

            e.preventDefault();
        });
    }

    UpdatePagedDisplayBatchAction.prototype.formatTargets = function(targets) {
        return targets.filter(d => d.metadata.type === 'Work')
            .map(d => d.metadata.id)
            .join(',');
    }

    UpdatePagedDisplayBatchAction.prototype.doWork = function() {
        this.actionHandler.addEvent({
            action : 'PagedDisplay',
            confirm : false
        });
    };

    return UpdatePagedDisplayBatchAction;
});