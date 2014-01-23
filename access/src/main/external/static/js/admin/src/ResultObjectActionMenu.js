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
		
		if (this.options.multipleSelectionEnabled) {
			this.batchActions = {};
			var actionClasses = [];
			for (var index in this.options.batchActions) {
				var group = this.options.batchActions[index];
				for (var aIndex in group.actions) {
					actionClasses.push(group.actions[aIndex].action + "Action");
				}
			}
			
			require(actionClasses, function(){
				var argIndex = 0, loadedClasses = arguments;
				
				for (var index in self.options.batchActions) {
					var group = self.options.batchActions[index];
			
					for (var aIndex in group.actions) {
						var actionDefinition = group.actions[aIndex];
						var actionClass = loadedClasses[argIndex++];
					
						var actionName = actionDefinition.action;
						var def = {
							label : actionDefinition.label,
							actionClass : actionClass,
							action : new actionClass({ target : self.options.resultList }),
							group : group.groupName? group.groupName : index
						};
						self.batchActions[actionName] = def;
					}
				}
			});
		}
		
		var menuOptions = {
			selector: this.options.selector,
			trigger: this.options.trigger,
			className: 'result_entry_context_menu',
			zIndex: 101,
			events : {
				show: function(event) {
					if (!self.hasMultipleSelected() || self.showingSingleMenu) {
						this.parents(self.options.containerSelector).find(".action_gear").attr("src", "/static/images/admin/gear_dark.png");
						var resultObject = event.$trigger.parents(self.options.containerSelector).data('resultObject');
						resultObject.highlight();
						event.$menu.attr('data-menutitle', resultObject.metadata.title);
					} else {
						event.$menu.attr('data-menutitle', "Selected " + self.selectedCount + " objects...");
					}
				},
				hide: function(event) {
					if (self.showingSingleMenu) {
						var resultObject = event.$trigger.parents(self.options.containerSelector).data('resultObject');
						this.parents(self.options.containerSelector).find(".action_gear").attr("src", "/static/images/admin/gear.png");
						resultObject.unhighlight();
					}
						
				}
			},
			build: function($trigger, e) {
				if (self.hasMultipleSelected())
					return self._buildSelectionMenu.call(self, $trigger, e);
				return self._buildSingleMenu.call(self, $trigger, e);
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
	
	ResultObjectActionMenu.prototype._buildSingleMenu = function($trigger, e) {
		var self = this;
		var resultObject = $trigger.parents(self.options.containerSelector).data('resultObject');
		var metadata = resultObject.metadata;
		var baseUrl = document.location.href;
		var serverUrl = baseUrl.substring(0, baseUrl.indexOf("/admin/")) + "/";
		baseUrl = baseUrl.substring(0, baseUrl.indexOf("/admin/") + 7);
		
		// Record which menu has been activated
		this.showingSingleMenu = true;
		
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
		if ($.inArray('purgeForever', metadata.permissions) != -1) {
			items["sepdestroy"] = "";
			items["destroy"] = {name : 'Destroy', disabled :  $.inArray('Active', metadata.status) != -1};
		}
		
		if ($.inArray('moveToTrash', metadata.permissions) != -1) {
			items["sepdel"] = "";
			items["restoreResult"] = {name : 'Restore', disabled : $.inArray('Deleted', metadata.status) == -1};
			items["deleteResult"] = {name : 'Delete', disabled : $.inArray('Active', metadata.status) == -1};
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
				}
			},
			items : items
		};
	};
	
	ResultObjectActionMenu.prototype._buildSelectionMenu = function($trigger, e) {
		var self = this;
		var resultObject = $trigger.parents(self.options.containerSelector).data('resultObject');
		// If the user activates 
		if (!resultObject.isSelected()) {
			return this._buildSingleMenu($trigger, e);
		}
		
		// Record which menu has been activated so that show/hide will know
		this.showingSingleMenu = false;
		
		var items = {};
		
		var previousGroup = null;
		$.each(this.batchActions, function(actionName, definition){
			// Add separators between groups
			if (definition.group != previousGroup) {
				if (previousGroup != null)
					items["sep" + definition.group] = "";
				previousGroup = definition.group;
			}
			
			var validCount = definition.action.countTargets();
			items[actionName] = {
				name : definition.label + (validCount? ' ' + validCount : '') 
					+ " object" + (validCount == 1? '' : 's'),
				disabled : validCount == 0
			};
		});
		
		return {
			callback: function(key, options) {
				self.actionHandler.addEvent({
					action : key,
					target : self.options.resultList,
					anchor : options.$trigger
				});
			}, items : items
		};
	};
	
	ResultObjectActionMenu.prototype.hasMultipleSelected = function() {
		return this.options.multipleSelectionEnabled && this.selectedCount > 1;
	};
	
	ResultObjectActionMenu.prototype.setSelectedCount = function(selectedCount) {
		this.selectedCount = selectedCount;
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
	
	ResultObjectActionMenu.prototype.disable = function() {
		$(this.options.selector).contextMenu(false);
	};
	
	ResultObjectActionMenu.prototype.enable = function() {
		$(this.options.selector).contextMenu(true);
	};
	
	return ResultObjectActionMenu;
});