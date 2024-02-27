define('UpdateViewSettingsBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!../templates/admin/viewSettingsForm"], function($, AbstractBatchAction, viewSettingsTemplate, ) {
    function UpdateViewSettingsBatchAction(context) {
        this._create(context);
    }

    UpdateViewSettingsBatchAction.prototype.constructor = UpdateViewSettingsBatchAction;
    UpdateViewSettingsBatchAction.prototype = Object.create(AbstractBatchAction.prototype);

    UpdateViewSettingsBatchAction.prototype.isValidTarget = function (target) {
        return target.isSelected() && target.isEnabled()
            && $.inArray('editViewSettings', target.metadata.permissions) !== -1
            && target.metadata.type === 'Work';
    };

    UpdateViewSettingsBatchAction.prototype.execute = function() {
        let self = this;
        let targets = this.getTargets();
        let UpdateViewSettingsForm = viewSettingsTemplate({
            options: { targets: this.formatTargets(targets) },
            metadata: { viewSettings: 'individual' }
        });

        this.dialog = $("<div class='containingDialog'>" + UpdateViewSettingsForm + "</div>");
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
            let pids = $('#view_settings_targets', self.$form).val();

            $.ajax({
                url: `/services/api/edit/viewSettings?targets=${encodeURIComponent(pids)}&view_setting=${encodeURIComponent(newViewSetting)}`,
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

    UpdateViewSettingsBatchAction.prototype.formatTargets = function(targets) {
        return targets.filter(d => this.isValidTarget(d))
            .map(d => d.metadata.id)
            .join(',');
    }

    UpdateViewSettingsBatchAction.prototype.doWork = function() {
        this.actionHandler.addEvent({
            action : 'ViewSettings',
            confirm : false
        });
    };

    return UpdateViewSettingsBatchAction;
});