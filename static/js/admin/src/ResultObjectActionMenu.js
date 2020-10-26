define('ResultObjectActionMenu', [ 'jquery', 'jquery-ui', 'StringUtilities', 'contextMenu'],
		function($, ui, StringUtilities) {
	
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
							joiner : actionDefinition.joiner,
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
		
		// Record which menu has been activated
		this.showingSingleMenu = true;
		
		var items = {};
		var isContainerFlag = false;
		if (resultObject.isContainer)
			isContainerFlag = true;
			items["openContainer"] = {name : "Open"};
		items["viewInDCR"] = {name : "View in DCR"};
		var dataFile = resultObject.getDatastream("DATA_FILE");
		if (dataFile) {
			items["viewFile"] = {name : "View File"
				+ " ("+ StringUtilities.readableFileSize(dataFile['fileSize']) + ")"};
		}

		// Export actions
		items["sepexport"] = "";
		if ($.inArray('info:fedora/cdr-model:Container', metadata.model) != -1) {
			items["exportCSV"] = {name : 'Export as CSV'};
		}
		if ($.inArray('editDescription', metadata.permissions) != -1) {
			items["exportXML"] = {name : 'Export MODS'};
		}

		if ($.inArray('editAccessControl', metadata.permissions) != -1) {
			items["sepedit"] = "";
			items["editAccess"] = {name : 'Edit Access'};
		}

		items["copyid"] = {name : 'Copy PID to Clipboard'};

		if ($.inArray('editAccessControl', metadata.permissions) != -1 &&
			$.inArray('purgeForever', metadata.permissions) != -1) {
				items["sepdestroy"] = "";
				items["reindex"] = {name : 'Reindex'};
		}
		
		return {
			callback: function(key, options) {
				switch (key) {
					case "viewInDCR" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "record/" + metadata.id,
							newWindow : true,
							application : "access"
						});
						break;
					case "viewFile" :
						var dataFile = resultObject.getDatastream("DATA_FILE");
						if (dataFile) {
							self.actionHandler.addEvent({
								action : 'ChangeLocation',
								url : "content/" + (dataFile['defaultWebObject']? dataFile['defaultWebObject'] : metadata.id),
								newWindow : true,
								application : "access"
							});
						}
						break;
					case "openContainer" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "list/" + metadata.id
						});
						break;
					case "reindex" :
						self.actionHandler.addEvent({
							action : 'ReindexResult',
							target : resultObject,
							confirmAnchor : options.$trigger
						});
						break;
					case "editAccess" :
						self.editAccess(resultObject);
						break;
					case "exportCSV" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "export/" + metadata.id
						});
						break;
					case "exportXML" :
						self.actionHandler.addEvent({
							action : 'ExportMetadataXMLBatch',
							targets : [resultObject]
						});
						break;
					case "copyid" :
						window.prompt("Copy PID to clipboard", metadata.id);
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
					+ (definition.joiner? definition.joiner : "")
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