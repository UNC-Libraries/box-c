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

            if ($.inArray('bulkUpdateDescription', this.container.permissions) !== -1) {
                items["exportMemberOrder"] = {name : "Member Order"};
            }
            if ($.inArray('bulkUpdateDescription', this.container.permissions) !== -1) {
                items["exportXML"] = {name : "Bulk MODS"};
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
                                    new ImportMetadataXMLForm({
                                        alertHandler : self.context.view.$alertHandler
                                    }).open();
                                    break;
                                case "exportXML" :
                                    self.options.actionHandler.addEvent({
                                        action : 'ExportMetadataXMLBatch',
                                        targets : self.targets
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