define('ImportMenu', [ 'jquery', 'jquery-ui', 'underscore', 'ImportMemberOrderForm', 'ImportMetadataXMLForm', 'BulkImportRefIdsForm', 'qtip'],
    function($, ui, _, ImportMemberOrderForm, ImportMetadataXMLForm, BulkImportRefIdsForm) {

        function ImportMenu(options) {
            this.options = $.extend({}, options);
            this.container = this.options.container;
            this.init();
            this.refresh();
        }

        ImportMenu.prototype.getMenuItems = function() {
            let items = {};

            if (this.container !== undefined && $.inArray('bulkUpdateDescription', this.container.permissions) !== -1) {
                items["importMemberOrder"] = {name : "Member Order"};
                items["importMetadata"] = {name : "Bulk MODS"};
            }

            if (this.container !== undefined && (this.container.type === 'AdminUnit' || this.container.type === 'Collection' || this.container.type === 'Folder')
                && $.inArray('editAspaceProperties', this.container.permissions) !== -1) {
                items["importRefIds"] = {name : "Bulk Ref Ids"};
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
                                    new ImportMemberOrderForm({
                                        alertHandler : self.options.alertHandler
                                    }).open();
                                    break;
                                case "importMetadata" :
                                    new ImportMetadataXMLForm({
                                        alertHandler : self.options.alertHandler
                                    }).open();
                                    break;
                                case "importRefIds" :
                                    new BulkImportRefIdsForm({
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