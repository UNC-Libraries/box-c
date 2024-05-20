define('ResultObjectActionMenu', [ 'jquery', 'jquery-ui', 'StringUtilities',  'AddFileForm', 'EditAccessSurrogateForm', 'EditThumbnailForm',
		'EditFilenameForm', 'EditTitleForm', 'DeleteForm', 'IngestFromSourceForm', 'ViewSettingsForm', 'contextMenu'],
		function($, ui, StringUtilities, AddFileForm, EditAccessSurrogateForm, EditThumbnailForm, EditFilenameForm, EditTitleForm, DeleteForm, IngestFromSourceForm, ViewSettingsForm) {

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
	}
	
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
		var isContentRoot = metadata.type === 'ContentRoot';
		var isAdminUnit = metadata.type === 'AdminUnit';
		var isFile = metadata.type === 'File';

		// Record which menu has been activated
		this.showingSingleMenu = true;
		
		var items = {};
		var isContainerFlag = false;
		var datastreams = metadata.datastream;

		if (resultObject.isContainer) {
			isContainerFlag = true;
			items["openContainer"] = {name : "Open"};
		}
		items["viewInDCR"] = {name : "View in DCR"};
		if (isFile) {
			var originalFile = resultObject.getDatastream("original_file");
			items["viewFile"] = {name : "View File"
				+ " ("+ StringUtilities.readableFileSize(originalFile['fileSize']) + ")"};
		}

		if ($.inArray('viewHidden', metadata.permissions) !== -1) {
			items["metadata"] = {name: "View metadata", items: {}}

			if (/md_descriptive/ig.test(datastreams)) {
				items['metadata']['items']["viewMods"] = {name: "View MODS"};
			}

			if (/md_descriptive_history/ig.test(datastreams)) {
				items['metadata']['items']["viewModsHistory"] = {name: "View MODS History"};
			}

			if (isFile) {
				items['metadata']['items']["viewFits"] = {name: "View FITS"}

				if (/techmd_fits_history/ig.test(datastreams)) {
					items['metadata']['items']["viewFitsHistory"] = {name: "View FITS History"}
				}
			}

			items['metadata']['items']["viewEventLog"] = {name : "View Event Log"};
		}
		
		// Modification options
		items["sepedit"] = "";
		if (!isContentRoot && $.inArray('editResourceType', metadata.permissions) != -1) {
			if (isFile) {
				if ($.inArray('Is Primary Object', metadata.contentStatus) != -1) {
					items['clearPrimaryObject'] = { name : 'Clear Primary Object' };
				} else {
					items['setAsPrimaryObject'] = { name : 'Set as Primary Object' };
				}
				if ($.inArray('Assigned As Thumbnail', metadata.contentStatus) != -1) {
					items['clearAssignedThumbnail'] = { name : 'Clear Assigned Thumbnail' };
				} else {
					items['assignAsThumbnail'] = { name : 'Assign as Thumbnail' };
				}
				items['assignAccessSurrogate'] = { name : 'Set Access Surrogate' };
				if (metadata.datastream.findIndex(d => d.startsWith('access_surrogate')) !== -1) {
					items['clearAccessSurrogate'] = { name : 'Clear Access Surrogate' };
				}
			} else if (metadata.type == 'Work') {
				if ($.inArray('Has Primary Object', metadata.contentStatus) != -1) {
					items['clearPrimaryObject'] = { name : 'Clear Primary Object' };
				}
				if ($.inArray('Has Assigned Thumbnail', metadata.contentStatus) != -1) {
					items['clearAssignedThumbnail'] = { name : 'Clear Assigned Thumbnail' };
				}
			}
		}
		
		if (!isContentRoot && $.inArray('editDescription', metadata.permissions) != -1) {
			if (metadata.type === 'File') {
				items["editFilename"] = {name : 'Edit Filename'};
			}

			items["editTitle"] = {name : 'Edit Title'};
		}
		
		/* Evaluating if retaining feature
		if ($.inArray('changePatronAccess', metadata.permissions) != -1
				&& $.inArray('info:fedora/cdr-model:Collection', metadata.model) != -1) {
			items["editCollectionSettings"] = {name : 'Edit Collection Settings'};
		}
		*/

		if (!isContentRoot && $.inArray('editDescription', metadata.permissions) != -1) {
			items["editDescription"] = {name : 'Edit Description'};
		}

		if ((metadata.type === 'Collection' || isAdminUnit) && $.inArray('editDescription', metadata.permissions) != -1) {
			items["editThumbnail"] = {name : 'Edit Display Thumbnail'};
		}

		// Add files to work objects
		if (!isContentRoot && metadata.type === 'Work' && $.inArray('ingest', metadata.permissions) != -1) {
			items["addFile"] = {name : 'Add File'};
			items["ingestSourceFilesOnly"] = {name : 'Add Files from Server'};
		}

		if (metadata.type === 'Work' && $.inArray('editViewSettings', metadata.permissions) !== -1) {
			items["viewSettings"] = {name : 'Update View Settings'};
		}

		// Export actions
		if (!isContentRoot && metadata.type !== 'File' && $.inArray('viewHidden', metadata.permissions) !== -1) {
			items["export"] = {name: "Export", items: {}}

			items['export']['items']["exportCSV"] = {name: "Export CSV"};

			if (metadata.type === 'Work') {
				items['export']['items']["exportMemberOrder"] = {name: "Export Member Order"};
			}
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
			if (!isContentRoot && $.inArray('destroy', metadata.permissions) != -1) {
				items["destroy"] = {name : 'Destroy', disabled :  !resultObject.isDeleted};
			}
		}
		
		// Trash actions
		if (!isContentRoot && ($.inArray('markForDeletionUnit', metadata.permissions) != -1 || ($.inArray('markForDeletion', metadata.permissions) != -1))) {
			items["septrash"] = "";
			items["restoreResult"] = {name : 'Restore', disabled : !resultObject.isDeleted};
			items["deleteResult"] = {name : 'Delete', disabled : resultObject.isDeleted};
		}

		// Set/Update permission actions
		var canChangePatronAccess = $.inArray('changePatronAccess', metadata.permissions) !== -1;
		var canAssignStaffRoles = $.inArray('assignStaffRoles', metadata.permissions) !== -1;

		if (!isContentRoot && (canAssignStaffRoles || (!isAdminUnit && canChangePatronAccess))) {
			items["seppermission"] = "";
		}

		if (!isContentRoot && !isAdminUnit && canChangePatronAccess) {
			items["patronPermissions"] = {name : 'Patron permissions'};
		}

		if (!isContentRoot && canAssignStaffRoles) {
			items["staffPermissions"] = {name : 'Staff permissions'};
		}

		return {
			callback: function(key, options) {
				switch (key) {
					case "viewInDCR" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url: (function() {
								var viewInDCRQualifier = isContentRoot ? "" : "record/"
								return viewInDCRQualifier + metadata.id
							})(),
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
					case "viewMods":
						self.actionHandler.addEvent({
							action: "ChangeLocation",
							url: "api/file/" + metadata.id + "/md_descriptive",
							newWindow: true,
							application: "services"
						});
						break;
					case "viewModsHistory":
						self.actionHandler.addEvent({
							action: "ChangeLocation",
							url: "api/file/" + metadata.id + "/md_descriptive_history",
							newWindow: true,
							application: "services"
						});
						break;
					case "viewFits":
						self.actionHandler.addEvent({
							action: "ChangeLocation",
							url: "api/file/" + metadata.id + "/techmd_fits",
							newWindow: true,
							application: "services"
						});
						break;
					case "viewFitsHistory":
						self.actionHandler.addEvent({
							action: "ChangeLocation",
							url: "api/file/" + metadata.id + "/techmd_fits_history",
							newWindow: true,
							application: "services"
						});
						break;
					case "viewEventLog" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "api/file/" + metadata.id + "/event_log",
							newWindow : true,
							application : "services"
						});
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
					case "addFile" :
						new AddFileForm({
							alertHandler : self.options.alertHandler
						}).open(resultObject);
						break;
					case "ingestSourceFilesOnly" :
						new IngestFromSourceForm({
							alertHandler : self.options.alertHandler,
							filesOnlyMode : true
						}).open(metadata.id);
						break;
					case "editFilename" :
						self.editFilename(resultObject);
						break;
					case "editTitle" :
						self.editTitle(resultObject);
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
					case "editThumbnail":
						self.editThumbnail(resultObject);
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
					case "clearAccessSurrogate" :
						self.actionHandler.addEvent({
							action : 'ClearAccessSurrogate',
							target : resultObject,
							confirm : false
						});
						break;
					case "assignAccessSurrogate" :
						self.editAccessSurrogate(resultObject);
						break;
					case "clearAssignedThumbnail" :
						self.actionHandler.addEvent({
							action : 'ClearAssignedThumbnail',
							target : resultObject,
							confirm : false
						});
						break;
					case "assignAsThumbnail" :
						self.actionHandler.addEvent({
							action : 'AssignAsThumbnail',
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
					case "deleteResult":
						var deleteForm = new DeleteForm({
							alertHandler : self.options.alertHandler,
							actionHandler : self.actionHandler
						});
						deleteForm.open([resultObject]);
						break;
					case "restoreResult":
						self.actionHandler.addEvent({
							action : 'RestoreResult',
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
							url : "api/exportTree/csv/" + metadata.id,
							application: "services"
						});
						break;
					case "exportMemberOrder" :
						self.actionHandler.addEvent({
							action : 'ChangeLocation',
							url : "api/edit/memberOrder/export/csv?ids=" + metadata.id,
							application: "services"
						});
						break;
					case "runEnhancements" :
						self.actionHandler.addEvent({
							action : 'RunEnhancementsBatch',
							targets : [resultObject]
						});
						break;
					case "copyid" :
						(async function() {
							try {
								await navigator.clipboard.writeText(metadata.id);
								self.options.alertHandler.alertHandler('success', `Id copied to clipboard: ${metadata.id}`);
							} catch(err) {
								self.options.alertHandler.alertHandler('error', 'Unable to copy id to clipboard');
							}
						})();
						break;
					case "viewSettings":
						self.viewSettings(resultObject);
						break;
					case "patronPermissions":
						perms_editor_store.setPermissionType('Patron');
						perms_editor_store.setMetadata(metadata);
						perms_editor_store.setShowModal(true);
						perms_editor_store.setAlertHandler(self.options.alertHandler);
						perms_editor_store.setActionHandler(self.actionHandler);
						perms_editor_store.setResultObject(resultObject);
						perms_editor_store.setResultObjects(null);
						break;
					case "staffPermissions":
						perms_editor_store.setPermissionType('Staff');
						perms_editor_store.setMetadata(metadata);
						perms_editor_store.setShowModal(true);
						perms_editor_store.setAlertHandler(self.options.alertHandler);
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

	ResultObjectActionMenu.prototype.editAccessSurrogate = function(resultObject) {
		var editAccessSurrogateForm = new EditAccessSurrogateForm({
			alertHandler : this.options.alertHandler,
			actionHandler : this.actionHandler,
			metadata: resultObject.metadata
		});
		editAccessSurrogateForm.open(resultObject);
	};

	ResultObjectActionMenu.prototype.editFilename = function(resultObject) {
		var editFilenameForm = new EditFilenameForm({
			alertHandler : this.options.alertHandler,
			actionHandler : this.actionHandler
		});
		editFilenameForm.open(resultObject);
	};

	ResultObjectActionMenu.prototype.editTitle = function(resultObject) {
		var editTitleForm = new EditTitleForm({
			alertHandler : this.options.alertHandler,
			actionHandler : this.actionHandler
		});
		editTitleForm.open(resultObject);

	};

	ResultObjectActionMenu.prototype.editThumbnail = function(resultObject) {
		var editThumbnailForm = new EditThumbnailForm({
			alertHandler : this.options.alertHandler,
			actionHandler : this.actionHandler
		});
		editThumbnailForm.open(resultObject);
	};

	ResultObjectActionMenu.prototype.viewSettings = function(resultObject) {
		var viewSettingsForm = new ViewSettingsForm({
			alertHandler : this.options.alertHandler,
			actionHandler : this.actionHandler,
			targets: resultObject.metadata.id
		});
		viewSettingsForm.open(resultObject);
	}
	
	ResultObjectActionMenu.prototype.disable = function() {
		$(this.options.selector).contextMenu(false);
	};
	
	ResultObjectActionMenu.prototype.enable = function() {
		$(this.options.selector).contextMenu(true);
	};
	
	return ResultObjectActionMenu;
});