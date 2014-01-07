define('ResultObjectActionMenu', [ 'jquery', 'jquery-ui', 'contextMenu'],
		function($, ui) {
	
	var defaultOptions = {
		selector : undefined,
		containerSelector : undefined,
		trigger : 'left',
		positionAtTrigger : true
	};
	
	function ResultObjectActionMenu(options) {
		this.options = $.extend({}, defaultOptions, options);
		this.actionHandler = this.options.actionHandler;
		this.create();
	};
	
	ResultObjectActionMenu.prototype.create = function() {
		var self = this;
		var menuOptions = {
			selector: this.options.selector,
			trigger: this.options.trigger,
			events : {
				show: function() {
					this.parents(self.options.containerSelector).find(".action_gear").attr("src", "/static/images/admin/gear_dark.png");
				},
				hide: function() {
					this.parents(self.options.containerSelector).find(".action_gear").attr("src", "/static/images/admin/gear.png");
				}
			},
			build: function($trigger, e) {
				var resultObject = $trigger.parents(self.options.containerSelector).data('resultObject');
				var metadata = resultObject.metadata;
				var baseUrl = document.location.href;
				var serverUrl = baseUrl.substring(0, baseUrl.indexOf("/admin/")) + "/";
				baseUrl = baseUrl.substring(0, baseUrl.indexOf("/admin/") + 7);
				
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
				if ($.inArray('editDescription', metadata.permissions) != -1)
					items["editDescription"] = {name : 'Edit Description'};
				if ($.inArray('purgeForever', metadata.permissions) != -1) {
					items["sepadmin"] = "";
					items["reindex"] = {name : 'Reindex'};
				}
				items["sepdel"] = "";
				if ($.inArray('purgeForever', metadata.permissions) != -1 && $.inArray('Deleted', metadata.status) != -1) {
					items["purgeForever"] = {name : 'Delete Forever'};
				}
				if ($.inArray('moveToTrash', metadata.permissions) != -1) {
					if ($.inArray('Deleted', metadata.status) == -1)
						items["moveToTrash"] = {name : 'Delete'};
					else
						items["moveToTrash"] = {name : 'Restore'};
				}
				
				return {
					callback: function(key, options) {
						switch (key) {
							case "viewInCDR" :
								window.open(serverUrl + "record/" + metadata.id,'_blank');
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
							case "purgeForever" :
								self.actionHandler.addEvent({
									action : 'DeleteForever',
									target : resultObject,
									confirmAnchor : options.$trigger
								});
								break;
							case "moveToTrash":
								self.actionHandler.addEvent({
									action : ($.inArray('Deleted', metadata.status) == -1)? 
											'TrashResult' : 'RestoreResult',
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
	
	ResultObjectActionMenu.prototype.editAccess = function(resultObject) {
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
	
	return ResultObjectActionMenu;
});