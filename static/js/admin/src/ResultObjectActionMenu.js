define('ResultObjectActionMenu', [ 'jquery', 'jquery-ui', 'StringUtilities',  'AddFileForm', 'EditLabelForm', 'EditFilenameForm', 'contextMenu'],
		function($, ui, StringUtilities, AddFileForm, EditLabelForm, EditFilenameForm) {
	
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
		items["viewInCDR"] = {name : "View in CDR"};
		var dataFile = resultObject.getDatastream("DATA_FILE");
		if (dataFile) {
			items["viewFile"] = {name : "View File"
				+ " ("+ StringUtilities.readableFileSize(dataFile['fileSize']) + ")"};
		}
		if (resultObject.metadata.type == 'Collection') {
			items["sepbrowse"] = "";
			items["viewTrash"] = {name : "View trash for this collection"};
			items["review"] = {name : "Review unpublished"};
		}
		
		// Modification options
		items["sepedit"] = "";
		if ($.inArray('addRemoveContents', metadata.permissions) != -1 && metadata.isPart) {
			var isDWO = $.inArray('Default Access Object', metadata.contentStatus) != -1;
			items[isDWO? 'clearDefaultWebObject' : 'setDefaultWebObject'] = {
				name : isDWO? 'Clear Primary Object' : 'Set as Primary Object'
			};
		}
		if ($.inArray('publish', metadata.permissions) != -1)
			items["publish"] = {name : $.inArray('Unpublished', metadata.status) == -1 ? 'Unpublish' : 'Publish'};
		if ($.inArray('editAccessControl', metadata.permissions) != -1) 
			items["editAccess"] = {name : 'Edit Access'};
		
		if ($.inArray('editDescription', metadata.permissions) != -1) {
			if (isContainerFlag) {
				items["editLabel"] = {name : 'Edit Label'};
			} else {
				items["editFilename"] = {name : 'Edit Filename'};
			}
		}
		
		if ($.inArray('editAccessControl', metadata.permissions) != -1
				&& $.inArray('info:fedora/cdr-model:Collection', metadata.model) != -1) {
			items["editCollectionSettings"] = {name : 'Edit Collection Settings'};
		}
		
		if ($.inArray('editDescription', metadata.permissions) != -1) {
			items["editDescription"] = {name : 'Edit Description'};
		}

		// Add files to collections and compound objects
		if (metadata.type === 'Aggregate') {
			items["addFile"] = {name : 'Add File'};
		}

		// Export actions
		items["sepexport"] = "";
		if ($.inArray('info:fedora/cdr-model:Container', metadata.model) != -1) {
			items["exportCSV"] = {name : 'Export as CSV'};
		}
		if ($.inArray('editDescription', metadata.permissions) != -1) {
			items["exportXML"] = {name : 'Export MODS'};
		}
		items["copyid"] = {name : 'Copy PID to Clipboard'};
		
		// Admin actions
		if ($.inArray('editAccessControl', metadata.permissions) != -1) {
			items["sepdestroy"] = "";
			items["runEnhancements"] = {name : 'Run enhancements'};
			if ($.inArray('purgeForever', metadata.permissions) != -1) {
				items["reindex"] = {name : 'Reindex'};
				items["destroy"] = {name : 'Destroy', disabled :  $.inArray('Active', metadata.status) != -1};
			}
		}
		
		// Trash actions
		if ($.inArray('moveToTrash', metadata.permissions) != -1) {
			items["septrash"] = "";
			items["restoreResult"] = {name : 'Restore', disabled : $.inArray('Deleted', metadata.status) == -1};
			items["deleteResult"] = {name : 'Delete', disabled : $.inArray('Active', metadata.status) == -1};
		}
		
		return {
			callback: function(key, options) {
				switch (key) {
					case "viewInCDR" :
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
					case "viewTrash" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "trash/" + metadata.id
						});
						break;
					case "review" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "review/" + metadata.id
						});
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
					case "addFile" :
						new AddFileForm({
							alertHandler : self.options.alertHandler
						}).open(resultObject);
						break;
					case "editLabel" :
						self.editLabel(resultObject);
						break;
					case "editFilename" :
						self.editFilename(resultObject);
						break;
					case "editType" :
						self.actionHandler.addEvent({
							action : 'EditTypeBatch',
							targets : [resultObject]
						});
						break;
					case "editDescription" :
						// Resolve url to be absolute for IE, which doesn't listen to base tags when dealing with javascript
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "describe/" + metadata.id
						});
						break;
					case "editCollectionSettings" :
						self.actionHandler.addEvent({
							action : 'EditCollectionSettings',
							target : resultObject
						});
						break;
					case "setDefaultWebObject" : case "clearDefaultWebObject" :
						self.actionHandler.addEvent({
							action : 'SetAsDefaultWebObjectBatch',
							targets : [resultObject],
							clear : key == "clearDefaultWebObject",
							confirm : false
						});
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
					case "runEnhancements" :
						self.actionHandler.addEvent({
							action : 'RunEnhancementsBatch',
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
	
	ResultObjectActionMenu.prototype.editLabel = function(resultObject) {
		var editLabelForm = new EditLabelForm({
			alertHandler : this.options.alertHandler,
			actionHandler : this.actionHandler
		});
		editLabelForm.open(resultObject);
		
	};

	ResultObjectActionMenu.prototype.editFilename = function(resultObject) {
		var editFilenameForm = new EditFilenameForm({
			alertHandler : this.options.alertHandler,
			actionHandler : this.actionHandler
		});
		editFilenameForm.open(resultObject);
		
	};
	
	ResultObjectActionMenu.prototype.disable = function() {
		$(this.options.selector).contextMenu(false);
	};
	
	ResultObjectActionMenu.prototype.enable = function() {
		$(this.options.selector).contextMenu(true);
	};
	
	return ResultObjectActionMenu;
});