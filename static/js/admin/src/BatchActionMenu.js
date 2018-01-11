define('BatchActionMenu', [ 'jquery', 'jquery-ui', 'URLUtilities', 'contextMenu'],
		function($, ui, URLUtilities) {
	
	var defaultOptions = {
		selector : undefined,
		containerSelector : undefined,
		trigger : 'left',
		positionAtTrigger : true
	};
	
	function BatchActionMenu(options) {
		this.options = $.extend({}, defaultOptions, options);
		this.actionHandler = this.options.actionHandler;
		this.create();
	};
	
	BatchActionMenu.prototype.create = function() {
		var self = this;
		var menuOptions = {
			selector: this.options.selector,
			trigger: this.options.trigger,
			className: 'result_entry_context_menu',
			events : {
				show: function(event) {
					var resultObject = event.$trigger.parents(self.options.containerSelector).data('resultObject');
					event.$menu.attr('data-menutitle', resultObject.metadata.title);
				}
			},
			build: function($trigger, e) {
				var resultObject = $trigger.parents(self.options.containerSelector).data('resultObject');
				var metadata = resultObject.metadata;
				var baseUrl = URLUtilities.getAdminUrl();
				
				var items = {};
				if (resultObject.isContainer)
					items["openContainer"] = {name : "Open"};
				items["viewInCDR"] = {name : "View in CDR"};
				if (resultObject.metadata.type == 'Collection') {
					items["sepbrowse"] = "";
					items["viewTrash"] = {name : "View trash for this collection"};
					items["review"] = {name : "Review unpublished"};
				}
				items["sepedit"] = "";
				if ($.inArray('publish', metadata.permissions) != -1)
					items["publish"] = {name : $.inArray('Unpublished', metadata.status) == -1 ? 'Unpublish' : 'Publish'};
				if ($.inArray('editAccessControl', metadata.permissions) != -1) 
					items["editAccess"] = {name : 'Edit Access'};
				if ($.inArray('editDescription', metadata.permissions) != -1) {
					items["editDescription"] = {name : 'Edit Description'};
					if ($.inArray('info:fedora/cdr-model:Container', metadata.model) != -1) {
						items["exportCSV"] = {name : 'Export as CSV'};
					}
				}
				items["copyid"] = {name : 'Copy PID to Clipboard'};
				if ($.inArray('purgeForever', metadata.permissions) != -1) {
					items["sepadmin"] = "";
					items["reindex"] = {name : 'Reindex'};
				}
				if ($.inArray('purgeForever', metadata.permissions) != -1) {
					items["sepdestroy"] = "";
					items["destroy"] = {name : 'Destroy', disabled :  $.inArray('Deleted', metadata.status) == -1};
				}
				
				if ($.inArray('moveToTrash', metadata.permissions) != -1) {
					items["sepdel"] = "";
					items["deleteResult"] = {name : 'Delete', disabled : $.inArray('Active', metadata.status) == -1};
					items["restoreResult"] = {name : 'Restore', disabled : $.inArray('Deleted', metadata.status) == -1};
				}
				
				return {
					callback: function(key, options) {
						switch (key) {
							case "viewInCDR" :
								window.open(URLUtilities.getAccessUrl() + "record/" + metadata.id,'_blank');
								break;
							case "openContainer" :
								document.location.href = baseUrl + "list/" + metadata.id;
								break;
							case "viewTrash" :
								document.location.href = baseUrl + "trash/" + metadata.id;
								break;
							case "review" :
								document.location.href = baseUrl + "review/" + metadata.id;
								break;
							case "publish" :
								self.actionHandler.addEvent({
									action : $.inArray("Unpublished", resultObject.metadata.status) == -1? 
											'Unpublish' : 'Publish',
									target : resultObject
								});
								break;
							case "editAccess" :
								self.editAccess(resultObject);
								break;
							case "editDescription" :
								// Resolve url to be absolute for IE, which doesn't listen to base tags when dealing with javascript
								document.location.href = baseUrl + "describe/" + metadata.id;
								break;
							case "destroy" :
								self.actionHandler.addEvent({
									action : 'DestroyResult',
									target : resultObject
								});
								break;
							case "deleteResult": case "restoreResult":
								self.actionHandler.addEvent({
									action : ($.inArray('Deleted', metadata.status) == -1)? 
											'DeleteResult' : 'RestoreResult',
									target : resultObject,
									confirmAnchor : options.$trigger
								});
								break;
							case "reindex" :
								self.actionHandler.addEvent({
									action : 'ReindexResult',
									target : resultObject,
									confirmAnchor : options.$trigger
								});
								break;
							case "exportCSV" :
								document.location.href = baseUrl + "export/" + metadata.id;
								break;
							case "copyid" :
								window.prompt("Copy PID to clipboard", metadata.id);
								break;
						}
					},
					items: items
				};
			}
		};
		if (self.options.positionAtTrigger)
			menuOptions.position = function(options, x, y) {
				options.$menu.position({
					my : "right top",
					at : "right bottom",
					of : options.$trigger
				});
			};
		$.contextMenu(menuOptions);
	};
	
	BatchActionMenu.prototype.editAccess = function(resultObject) {
		var self = this;
		var dialog = $("<div class='containingDialog'><img src='/static/images/admin/loading_large.gif'/></div>");
		dialog.dialog({
			autoOpen: true,
			width: 500,
			height: 'auto',
			maxHeight: 800,
			minWidth: 500,
			modal: true,
			title: 'Access Control Settings',
			close: function() {
				self.actionHandler.addEvent({
					action : 'RefreshResult',
					target : resultObject,
					waitForUpdate : true,
					maxAttempts : 3
				});
				dialog.remove();
				resultObject.unhighlight();
			}
		});
		dialog.load("acl/" + resultObject.metadata.id, function(responseText, textStatus, xmlHttpRequest){
			dialog.dialog('option', 'position', 'center');
		});
	};
	
	BatchActionMenu.prototype.disable = function() {
		$(this.options.selector).contextMenu(false);
	};
	
	BatchActionMenu.prototype.enable = function() {
		$(this.options.selector).contextMenu(true);
	};
	
	return BatchActionMenu;
});