define('ResultObjectActionMenu', [ 'jquery', 'jquery-ui', 'DeleteObjectButton', 'PublishObjectButton', 'contextMenu'],
		function($, ui, DeleteObjectButton, PublishObjectButton) {
	
	function ResultObjectActionMenu(options) {
		this.create(options);
	};
	
	ResultObjectActionMenu.prototype.create = function(options) {
		this.options = options;
		var self = this;
		$.contextMenu({
			selector: this.options.selector,
			trigger: 'left',
			events : {
				show: function() {
					this.parents(self.options.containerSelector).find(".action_gear").attr("src", "/static/images/admin/gear_dark.png");
				},
				hide: function() {
					this.parents(self.options.containerSelector).find(".action_gear").attr("src", "/static/images/admin/gear.png");
				}
			},
			position : function(options, x, y) {
				options.$menu.position({
					my : "right top",
					at : "right bottom",
					of : options.$trigger
				});
			},
			build: function($trigger, e) {
				var resultObject = $trigger.parents(self.options.containerSelector).data('resultObject');
				var metadata = resultObject.metadata;
				var items = {};
				if ($.inArray('publish', metadata.permissions) != -1)
					items["publish"] = {name : $.inArray('Unpublished', metadata.status) == -1 ? 'Unpublish' : 'Publish'};
				if ($.inArray('editAccessControl', metadata.permissions) != -1) 
					items["editAccess"] = {name : 'Edit Access'};
				if ($.inArray('editDescription', metadata.permissions) != -1)
					items["editDescription"] = {name : 'Edit Description'};
				if ($.inArray('purgeForever', metadata.permissions) != -1)
					items["purgeForever"] = {name : 'Delete'};
					
				return {
					callback: function(key, options) {
						switch (key) {
							case "publish" :
								var publishButton = new PublishObjectButton({
									pid : resultObject.pid,
									parentObject : resultObject,
									defaultPublish : $.inArray("Unpublished", resultObject.metadata.status) == -1,
									metadata : metadata
								});
								publishButton.activate();
								break;
							case "editAccess" :
								self.editAccess(resultObject);
								break;
							case "editDescription" :
								// Resolve url to be absolute for IE, which doesn't listen to base tags when dealing with javascript
								var url = document.location.href;
								url = url.substring(0, url.indexOf("/admin/") + 7);
								document.location.href = url + "describe/" + metadata.id;
								break;
							case "purgeForever" :
								var deleteButton = new DeleteObjectButton({
									pid : resultObject.pid,
									parentObject : resultObject,
									metadata : metadata,
									confirmAnchor : options.$trigger
								});
								deleteButton.activate();
								break;
						}
					},
					items: items
				};
			}
		});
	};
	
	ResultObjectActionMenu.prototype.editAccess = function(resultObject) {
		var dialog = $("<div class='containingDialog'><img src='/static/images/admin/loading-large.gif'/></div>");
		dialog.dialog({
			autoOpen: true,
			width: 500,
			height: 'auto',
			maxHeight: 800,
			minWidth: 500,
			modal: true,
			title: 'Access Control Settings',
			close: function() {
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