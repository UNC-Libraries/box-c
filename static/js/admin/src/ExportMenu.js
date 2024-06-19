define('ExportMenu', [ 'jquery', 'jquery-ui', 'underscore', 'qtip', 'cycle'],
    function($, ui, _) {

        function ExportMenu(options) {
            this.options = $.extend({}, options);
            this.init();
            this.refresh();
        }

        ExportMenu.prototype.getMenuItems = function() {
            let items = {};
            let targets = this.getTargets();

            if (this.getMemberOrderTargetIds() !== '') {
                items["exportMemberOrder"] = {name : "Member Order"};
            }
            if ($.inArray('editDescription', targets[0].metadata.permissions) !== -1) {
                items["exportXML"] = {name : "Bulk Metadata"};
                items["exportCSV"] = {name : "Bulk CSV"};
            }

            return items;
        };

        ExportMenu.prototype.getMemberOrderTargetIds = function() {
            let ids = this.getTargets().filter((d) => {
                return $.inArray("viewHidden", d.metadata.permissions) !== -1 && d.metadata.type === "Work"
            }).map(d => d.metadata.id);
            return ids.join(',');
        }

        ExportMenu.prototype.getTargetIdsAsString = function() {
            let targets = this.getTargets();
            return targets.map(d => d.metadata.id).join(',');
        }

        ExportMenu.prototype.canEditDescription = function() {
            return $.inArray('editDescription', this.getTargets()[0].metadata.permissions) !== -1;
        }

        ExportMenu.prototype.getTargets = function() {
            return JSON.retrocycle(JSON.parse(sessionStorage.getItem('exportTargets')));
        }

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
                                sessionStorage.removeItem('exportTargets');
                            }
                        },
                        items: items,
                        callback : function(key, options) {
                            switch (key) {
                                case "exportMemberOrder" :
                                    self.options.actionHandler.addEvent({
                                        action : 'ChangeLocation',
                                        url : "api/edit/memberOrder/export/csv?ids=" + self.getMemberOrderTargetIds(),
                                        application: "services"
                                    });
                                    sessionStorage.removeItem('exportTargets');
                                    break;
                                case "exportXML" :
                                    self.options.actionHandler.addEvent({
                                        action : 'ExportMetadataXMLBatch',
                                        targets : self.getTargets()
                                    });
                                    sessionStorage.removeItem('exportTargets');
                                    break;
                                case "exportCSV" :
                                    self.options.actionHandler.addEvent({
                                        action : 'ChangeLocation',
                                        url : `api/exportTree/csv?ids=${self.getTargetIdsAsString()}`,
                                        application: "services"
                                    });
                                    sessionStorage.removeItem('exportTargets');
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