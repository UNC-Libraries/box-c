define('ImportMenu', [ 'jquery', 'jquery-ui', 'underscore', 'ImportMetadataXMLForm', 'qtip'],
    function($, ui, _, ImportMetadataXMLForm) {

        function ImportMenu(options) {
            this.options = $.extend({}, options);
            this.container = this.options.container;
            this.init();
            this.refresh();
        }

        ImportMenu.prototype.getMenuItems = function() {
            let items = {};

            if ($.inArray('bulkUpdateDescription', this.container.permissions) !== -1) {
                items["importMemberOrder"] = {name : "Member Order"};
            }
            if ($.inArray('bulkUpdateDescription', this.container.permissions) !== -1) {
                items["importMetadata"] = {name : "Bulk MODS"};
            }

            return items;
        };

        ImportMenu.prototype.setContainer = function(container) {
            this.container = container;
            return this;
        };

        ImportMenu.prototype.refresh = function() {
            let items = this.getMenuItems();
            if ($.isEmptyObject(items)) {
                $(this.options.selector).hide();
            } else {
                $(this.options.selector).show();
            }
        }

        ImportMenu.prototype.init = function() {
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
                                case "importMemberOrder" :
                                    new ImportMetadataXMLForm({
                                        alertHandler : self.options.alertHandler
                                    }).open();
                                    break;
                                case "importMetadata" :
                                    new ImportMetadataXMLForm({
                                        alertHandler : self.options.alertHandler
                                    }).open();
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

        return ImportMenu;
    });