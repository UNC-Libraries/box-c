define('ExportMenu', [ 'jquery', 'jquery-ui', 'underscore', 'ImportMetadataXMLForm', 'qtip'],
    function($, ui, _, ImportMetadataXMLForm) {

        function ExportMenu(options) {
            this.options = $.extend({}, options);
            this.container = this.options.container;
            this.init();
            this.refresh();
        }

        ExportMenu.prototype.getMenuItems = function() {
            let items = {};
            const metadata = this.container.metadata;
            if (metadata.type === 'Work' && $.inArray('viewHidden', metadata.permissions) !== -1) {
                items["exportMemberOrder"] = {name : "Member Order"};
            }
            if ($.inArray('editDescription', metadata.permissions) !== -1) {
                items["exportXML"] = {name : "Bulk Metadata"};
            }

            return items;
        };

        ExportMenu.prototype.setContainer = function(container) {
            this.container = container;
            return this;
        };

        ExportMenu.prototype.refresh = function() {
            let items = this.getMenuItems();
            if ($.isEmptyObject(items)) {
                $(this.options.selector).hide();
            } else {
                $(this.options.selector).show();
            }
        }

        ExportMenu.prototype.getTargetIds = function(targets) {
            let ids = targets.map(d => d.metadata.id);
            return ids.join(',');
        }

        ExportMenu.prototype.init = function() {
            let self = this;

            this.menu = $.contextMenu({
                selector: this.options.selector,
                trigger: 'left',
                build: function($triggerEvent, e) {
                    let items = self.getMenuItems();

                    return {
                        className: 'add_to_container_menu',
                        events : {
                            show: function() {
                                this.addClass("active");
                            },
                            hide: function() {
                                this.removeClass("active");
                            }
                        },
                        items: items,
                        callback : function(key, options) {
                            switch (key) {
                                case "exportMemberOrder" :
                                    self.options.actionHandler.addEvent({
                                        action : 'ChangeLocation',
                                        url : "api/edit/memberOrder/export/csv?ids=" + self.getTargetIds(self.options.targets),
                                        application: "services"
                                    });
                                    break;
                                case "exportXML" :
                                    self.options.actionHandler.addEvent({
                                        action : 'ExportMetadataXMLBatch',
                                        targets : self.options.targets
                                    });
                                    break;
                            }
                        },
                        position : function(options, x, y) {
                            options.$menu.position({
                                my : "right top",
                                at : "right bottom",
                                of : options.$trigger
                            });
                        }
                    }
                }
            });
        };

        return ExportMenu;
    });