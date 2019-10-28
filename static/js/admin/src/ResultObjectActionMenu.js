define('ResultObjectActionMenu', [ 'jquery', 'jquery-ui', 'StringUtilities',  'AddFileForm', 'EditLabelForm', 'EditFilenameForm', 'DeleteForm', 'contextMenu'],
		function($, ui, StringUtilities, AddFileForm, EditLabelForm, EditFilenameForm, DeleteForm) {
	
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
		if (resultObject.isContainer) {
			isContainerFlag = true;
			items["openContainer"] = {name : "Open"};
		}
		items["viewInDCR"] = {name : "View in DCR"};
		if (metadata.type == 'File') {
			var originalFile = resultObject.getDatastream("original_file");
			items["viewFile"] = {name : "View File"
				+ " ("+ StringUtilities.readableFileSize(originalFile['fileSize']) + ")"};
		}
		
		// Modification options
		items["sepedit"] = "";
		if ($.inArray('editResourceType', metadata.permissions) != -1) {
			if (metadata.type == 'File') {
				if ($.inArray('Is Primary Object', metadata.contentStatus) != -1) {
					items['clearPrimaryObject'] = { name : 'Clear Primary Object' };
				} else {
					items['setAsPrimaryObject'] = { name : 'Set as Primary Object' };
				}
			} else if (metadata.type == 'Work') {
				if ($.inArray('Has Primary Object', metadata.contentStatus) != -1) {
					items['clearPrimaryObject'] = { name : 'Clear Primary Object' };
				}
			}
		}
		
		/* Publish action being replaced
		if ($.inArray('publish', metadata.permissions) != -1)
			items["publish"] = {name : $.inArray('Unpublished', metadata.status) == -1 ? 'Unpublish' : 'Publish'};
		*/
		
		if ($.inArray('editDescription', metadata.permissions) != -1) {
			if (isContainerFlag) {
				items["editLabel"] = {name : 'Edit Label'};
			} else {
				items["editFilename"] = {name : 'Edit Filename'};
			}
		}
		
		/* Evaluating if retaining feature
		if ($.inArray('changePatronAccess', metadata.permissions) != -1
				&& $.inArray('info:fedora/cdr-model:Collection', metadata.model) != -1) {
			items["editCollectionSettings"] = {name : 'Edit Collection Settings'};
		}
		*/
		
		if ($.inArray('editDescription', metadata.permissions) != -1) {
			items["editDescription"] = {name : 'Edit Description'};
		}

		// Add files to work objects
		if (metadata.type === 'Work' && $.inArray('ingest', metadata.permissions) != -1) {
			items["addFile"] = {name : 'Add File'};
		}

		// Export actions
		items["sepexport"] = "";
		if (metadata.type !== 'File' ) {
			items["exportCSV"] = {name : 'Export as CSV'};
		}
		if ($.inArray('editDescription', metadata.permissions) != -1) {
			items["exportXML"] = {name : 'Export MODS'};
		}
		items["copyid"] = {name : 'Copy PID to Clipboard'};
		
		// Admin actions
		var adminItems = [];
		if ($.inArray('destroy', metadata.permissions) != -1 || $.inArray('reindex', metadata.permissions) != -1) {
			items["sepdestroy"] = "";
			if ($.inArray('reindex', metadata.permissions) != -1) {
				items["runEnhancements"] = {name : 'Run enhancements'};
				items["reindex"] = {name : 'Reindex'};
			}
			if ($.inArray('destroy', metadata.permissions) != -1) {
				items["destroy"] = {name : 'Destroy', disabled :  !metadata.isDeleted};
			}
		}
		
		// Trash actions
		if ($.inArray('markForDeletionUnit', metadata.permissions) != -1 || ($.inArray('markForDeletion', metadata.permissions) != -1 && metadata.type !== 'Unit')) {
			items["septrash"] = "";
			items["restoreResult"] = {name : 'Restore', disabled : !metadata.isDeleted};
			items["deleteResult"] = {name : 'Delete', disabled : metadata.isDeleted};
		}

		// Set/Update permission actions
		items["seppermission"] = "";
		items["patronPermissions"] = {name : 'Patron permissions'};
		items["staffPermissions"] = {name : 'Staff permissions'};

		// Get data object for vue permissions editor
		var perms_editor_data = perms_editor.$children[0].$children[0].$data;

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
						var originalFile = resultObject.getDatastream("original_file");
						if (originalFile) {
							self.actionHandler.addEvent({
								action : 'ChangeLocation',
								url : "content/" + metadata.id,
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
					case "review" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "review/" + metadata.id
						});
						break;
					case "publish" :
						self.actionHandler.addEvent({
							action : $.inArray("Unpublished", metadata.status) == -1? 
									'Unpublish' : 'Publish',
							target : resultObject
						});
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
					case "clearPrimaryObject" :
						self.actionHandler.addEvent({
							action : 'ClearPrimaryObjectResult',
							target : resultObject,
							confirm : false
						});
						break;
					case "setAsPrimaryObject" :
						self.actionHandler.addEvent({
							action : 'SetAsPrimaryObjectResult',
							target : resultObject,
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
						var deleteForm = new DeleteForm({
							alertHandler : self.options.alertHandler,
							actionHandler : self.actionHandler
						});
						deleteForm.open([resultObject]);
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
					case "patronPermissions" :
						perms_editor_data.permissionType = 'Patron';
						perms_editor_data.metadata = metadata;
						perms_editor_data.showModal = true;
						perms_editor_data.alertHandler = self.options.alertHandler;
						break;
					case "staffPermissions":
						perms_editor_data.permissionType = 'Staff';
						perms_editor_data.metadata = metadata;
						perms_editor_data.showModal = true;
						perms_editor_data.alertHandler = self.options.alertHandler;
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