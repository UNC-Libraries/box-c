define('StructureEntry', [ 'jquery', 'jquery-ui', 'underscore', 'tpl!../templates/structureEntry'], function($, ui, _, structureEntryTemplate) {
	var defaultOptions = {
			indentSuppressed : false,
			isRoot : false,
			isSelected : false,
			structureView : null,
			node : null
	};
	
	function StructureEntry(options) {
		this.options = $.extend({}, defaultOptions, options);
		this.create();
	};
	
	StructureEntry.prototype.create = function() {
		this.node = this.options.node;
		this.metadata = this.node.entry;
		this.entryId = "str_" + this.metadata.id.substring(this.metadata.id.indexOf(':') + 1)
		this.isAContainer = this.metadata.type == "Collection" || this.metadata.type == "Folder" 
				|| this.metadata.type == "Aggregate";
		
		if (this.node.children) {
			this.childEntries = [];
			var childNodes = this.node.children;
			for (var i in childNodes) {
				this.childEntries.push(new StructureEntry({
					node : childNodes[i],
					structureView : this.options.structureView
				}));
			}
		}
		
		this.contentLoaded = (this.childEntries && this.childEntries.length > 0) 
				|| (this.metadata.counts && this.metadata.counts.containers !== undefined && this.metadata.counts.containers == 0);
	};
	
	StructureEntry.prototype.render = function($parentElement) {
		var $content = $(this.getTemplate());
		if ($parentElement)
			$parentElement.append($content);
		this.initializeElement($content);
	};
	
	StructureEntry.prototype.initializeElement = function(rootElement) {
		this.element = $("#" + this.entryId, rootElement);
		if (this.element.length == 0)
			this.element = rootElement;
		this.element.data('structureEntry', this);
		this.$entry = this.element.children(".entry");
		if (this.options.structureView.options.indentSuppressed)
			this.element.addClass('suppressed');
		this.skipLastIndent = this.element.hasClass('view_all');
		
		this._renderIndent();
		
		for (var i in this.childEntries) {
			this.childEntries[i].initializeElement(rootElement);
		}
	};
		
	StructureEntry.prototype.getTemplate = function() {
		var toggleClass = '';
		if (this.childEntries && this.childEntries.length > 0) {
			if (this.options.isRoot || !this.options.showingItems)
				toggleClass = 'collapse';
			else toggleClass = 'expand';
		} else if ((this.metadata.counts && this.metadata.counts.containers) ||
				(this.options.structureView.options.retrieveFiles && this.metadata.counts && this.metadata.counts.child)) {
			toggleClass = 'expand';
		}
		
		var childCount = this.metadata.counts && this.metadata.counts.child? this.metadata.counts.child : null;
		
		var primaryAction = this.options.structureView.options.queryPath + "/" + this.metadata.id;
		if (!this.isAContainer)
			primaryAction = "record/" + this.metadata.id;
		else if (this.options.structureView.options.filterParams)
			primaryAction += "?" + this.options.structureView.options.filterParams;
		
		var downloadUrl = null;
		if ($.inArray('viewOriginal', this.metadata.permissions) != -1 && $.inArray('DATA_FILE', this.metadata.datastreams) != -1){
			downloadUrl = "files/" + this.metadata.id + "/DATA_FILE?dl=true"; 
		}
		
		var hideEntry = (this.options.structureView.options.hideRoot && this.options.isRoot) || 
				(this.options.structureView.excludeIds && $.inArray(this.metadata.id, this.options.structureView.excludeIds) != -1)
				|| !this.metadata.title;
		
		return structureEntryTemplate({
			entryId : this.entryId,
			metadata : this.metadata,
			childEntries : this.childEntries,
			isAContainer : this.isAContainer,
			hideEntry : hideEntry,
			toggleClass : toggleClass,
			childCount : childCount,
			primaryAction : primaryAction,
			secondaryActions : this.options.structureView.options.secondaryActions,
			downloadUrl : downloadUrl,
			isRoot : this.options.isRoot,
			isSelected : this.options.isSelected
		});
	};
	
	StructureEntry.prototype.toggleChildren = function() {
		var self = this;
		var $toggleButton = this.$entry.find('.cont_toggle');
		var $childrenContainer = this.element.children(".children");
		if ($toggleButton.hasClass('expand')) {
			if (!self.contentLoaded) {
				var loadingImage = $("<img src=\"/static/images/ajax_loader.gif\"/>");
				$toggleButton.after(loadingImage);
				var childrenUrl = "structure/" + this.metadata.id + "/json";
				var childrenParams = "";
				if (this.options.structureView.options.retrieveFiles)
					childrenParams += "files=true";
				if (this.options.structureView.options.filterParams) {
					if (childrenParams)
						childrenParams += "&";
					childrenParams += this.options.structureView.options.filterParams;
				}
				if (childrenParams)
					childrenUrl += "?" + childrenParams;
				$.ajax({
					url: childrenUrl,
					dataType : 'json',
					success: function(data){
						loadingImage.remove();
						if (data) {
							this.childEntries = [];
							if (data.root && data.root.children && data.root.children.length > 0) {
								for (var i in data.root.children) {
									var childEntry = new StructureEntry({
										node : data.root.children[i],
										structureView : self.options.structureView
									});
									this.childEntries.push(childEntry);
									$childrenContainer.append(childEntry.getTemplate());
								}
								
								for (var i in this.childEntries) {
									this.childEntries[i].initializeElement(self.element);
								}
								
								$childrenContainer.find(".indent").show();
								$childrenContainer.show(100, function() {
									self.element.addClass("expanded");
								});
							}
							
							if ($childrenContainer.children().length > 0)
								$toggleButton.removeClass('expand').addClass('collapse');
							else
								$toggleButton.removeClass('expand').addClass('leaf');
						}
						self.contentLoaded = true;
					},
					error: function(xhr, ajaxOptions, thrownError){
						loadingImage.remove();
					}
				});
			} else {
				if ($childrenContainer.children().length > 0) {
					$childrenContainer.find(".indent").show();
					$childrenContainer.show(100, function() {
						self.element.addClass("expanded");
					});
					$toggleButton.removeClass('expand').addClass('collapse');
				}
			}
		} else if ($toggleButton.hasClass('collapse')) {
			if ($childrenContainer.children().length > 0) {
				$childrenContainer.hide(100, function() {
					self.element.removeClass("expanded");
				});
			}
			$toggleButton.removeClass('collapse').addClass('expand');
		}
	};
	
	StructureEntry.prototype.refreshIndent = function() {
		this.element.children(".indent").remove();
		this._renderIndent();
	};
	
	StructureEntry.prototype._renderIndent = function () {
		var $entry = this.element.children('.entry'),
			$ancestors = this.element.parents(".entry_wrap:not(.suppressed)"),
			lastTier = $ancestors.length;
		if (lastTier == 0)
			return;
		$ancestors = $($ancestors.get().reverse());
		if (!this.skipLastIndent)
			$ancestors.push(this.element);
		
		$ancestors.each(function(i){
			if (i == 0)
				return;
			var hasSiblings = $(this).next(".entry_wrap:not(.view_all)").length > 0;
			if (i == lastTier) {
				if (hasSiblings) {
					$entry.before("<div class='indent with_sib'></div>");
				} else {
					$entry.before("<div class='indent last_sib'></div>");
				}
			} else {
				if (hasSiblings) {
					$entry.before("<div class='indent'></div>");
				} else {
					$entry.before("<div class='indent empty'></div>");
				}
			}
		});
	};
	
	StructureEntry.prototype.getParentURL = function() {
		return "structure/" + this.metadata.id + "/parent";
	};
	
	StructureEntry.prototype.insertTree = function(oldRoot) {
	// Find the old root in the new results and remove it, while retaining an insertion point for the old root.
		var $oldRootDuplicate = this.element.find('#' + oldRoot.entryId);
		var $placeholder = $oldRootDuplicate.prev('.entry_wrap');
		$oldRootDuplicate.remove();
		
		// Insert the old root into the new results
		oldRoot.element.detach();
		// Insertion point depends on if it is the first sibling
		var $refreshSet = oldRoot.element.find(".entry_wrap").add(oldRoot.element);
		
		if ($placeholder.length == 0)
			this.element.children(".children").prepend(oldRoot.element);
		else {
			$placeholder.after(oldRoot.element);
			$refreshSet.add($placeholder);
		}
		this.element.addClass("expanded").children(".children").show();
		$refreshSet.each(function(){
			$(this).data('structureEntry').refreshIndent();
		});
	};
	
	StructureEntry.prototype.select = function() {
		this.element.addClass("selected");
		this.options.isSelected = true;
	};
	
	StructureEntry.prototype.findEntryById = function(id) {
		if (this.metadata.id == id)
			return this;
		for (var index in this.childEntries) {
			var result = this.childEntries[index].findEntryById(id);
			if (result)
				return result;
		}
		return null;
	};
	
	return StructureEntry;
});define('StructureView', [ 'jquery', 'jquery-ui', 'StructureEntry'], function($, ui, StructureEntry) {
	$.widget("cdr.structureView", {
		options : {
			showResourceIcons : true,
			indentSuppressed : false,
			showParentLink : false,
			secondaryActions : false,
			hideRoot : false,
			rootNode : null,
			queryPath : 'structure',
			filterParams : '',
			excludeIds : null,
			retrieveFiles : false,
			selectedId : false
		},
		_create : function() {
			
			this.element.wrapInner("<div class='structure_content'/>");
			this.$content = this.element.children();
			this.element.addClass('structure');
			if (!this.options.showResourceIcons)
				this.element.addClass('no_resource_icons');
			
			if (this.options.showParentLink) {
				this._generateParentLink();
			}
			
			if (this.options.excludeIds) {
				this.excludeIds = this.options.excludeIds.split(" ");
			}
			
			// Generate the tree of entries starting from the root node
			this.rootEntry = new StructureEntry({
				node : this.options.rootNode,
				structureView : this,
				isRoot : true,
				isSelected : this.options.rootSelected
			});
			
			// Render the tree
			this.rootEntry.render();
			
			// If specified, select the selecte entry
			if (this.options.selectedId) {
				var selectedEntry = this.rootEntry.findEntryById(this.options.selectedId);
				if (selectedEntry)
					selectedEntry.select();
			}
			this.$content.append(this.rootEntry.element);
			
			this._initHandlers();
		},
		
		_initHandlers : function() {
			this.element.on("click", ".cont_toggle", function(){
				var structureEntry = $(this).parents(".entry_wrap").first().data('structureEntry');
				structureEntry.toggleChildren();
				return false;
			});
		},
		
		_generateParentLink : function() {
			var self = this;
			var $parentLink = $("<a class='parent_link'>parent</a>");
			if (this.options.rootNode.isTopLevel)
				$parentLink.addClass('disabled');
				
			$parentLink.click(function(){
				if ($parentLink.hasClass('disabled'))
					return false;
				var $oldRoot = self.$content.children(".entry_wrap");
				var parentURL = $oldRoot.data("structureEntry").getParentURL();
				$.ajax({
					url : parentURL,
					dataType : 'json',
					success : function(data) {
						var newRoot = new StructureEntry({
							node : data.root,
							structureView : self,
							isRoot : true
						});
						newRoot.render();
						// Initialize the new results
						//$newRoot.find(".entry_wrap").add($newRoot).structureEntry({
						//	indentSuppressed : self.options.indentSuppressed
						//});
						newRoot.insertTree($oldRoot.data('structureEntry'));
						//$newRoot.structureEntry('insertTree', $oldRoot);
						self.$content.append(newRoot.element);
						if (data.root.isTopLevel)
							$parentLink.addClass('disabled');
					}
				});
				return false;
			});
			
			this.$content.before($parentLink);
		}
	});
});/**
 * Implements functionality and UI for the generic Ingest Package form
 */
define('AbstractFileUploadForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 
		'ModalLoadingOverlay', 'ConfirmationDialog'], 
		function($, ui, _, RemoteStateChangeMonitor, ModalLoadingOverlay, ConfirmationDialog) {
	
	var defaultOptions = {
		iframeSelector : "#upload_file_frame",
		showUploadProgress : true
	};
	
	function AbstractFileUploadForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	};
	
	AbstractFileUploadForm.prototype.getDefaultOptions = function () {
		return defaultOptions;
	};
	
	AbstractFileUploadForm.prototype.open = function(pid) {
		var self = this;
		var formContents = this.options.createFormTemplate({pid : pid});
		this.closed = false;
		
		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.$form = this.dialog.first();
		this.dialog.dialog({
			autoOpen: true,
			width: 'auto',
			minWidth: '500',
			height: 'auto',
			modal: true,
			title: self.options.title,
			beforeClose: $.proxy(self.close, self)
		});
		
		$("input[type='file']", this.$form).change(function(){
			self.ingestFile = this.files[0];
			if (self.ingestFile) {
				var fileInfo = "";
				if (self.ingestFile.type)
					fileInfo += self.ingestFile.type + ", ";
				fileInfo += self.readableFileSize(self.ingestFile.size);
				$(".file_info", self.$form).html(fileInfo);
			} else
				$(".file_info", self.$form).html("");
		});
		
		this.overlay = new ModalLoadingOverlay(this.$form, {
			autoOpen : false,
			type : this.options.showUploadProgress? 'determinate' : 'icon',
			text : this.options.showUploadProgress? 'uploading...' : null,
			dialog : this.dialog
		});
		// Flag to track when the form has been submitted and needs to be locked
		this.submitted = false;
		// Flag to track when a file upload is in progress
		this.uploadInProgress = false;
		this.$form.submit(function(){
			if (self.submitted)
				return false;
			self.preprocessForm();
			errors = self.validationErrors();
			if (errors && errors.length > 0) {
				self.options.alertHandler.alertHandler("error", errors);
				return false;
			}
			
			self.submitted = true;
			self.overlay.open();
			if (self.supportsAjaxUpload()) {
				self.submitAjax();
			} else {
				// Older browser or IE that doesn't support ajax file uploads, use iframe submit approach
				self.submitIFrame();
				return true;
			}
			return false;
		});
	};
	
	AbstractFileUploadForm.prototype.close = function() {
		var self = this;
		if (this.closed) return;
		if (this.uploadInProgress && this.options.showUploadProgress) {
			this.closeConfirm = new ConfirmationDialog({
				promptText : 'Your submission is currently uploading, do you wish to close this window and abort the upload?',
				confirmText : 'Close and continue',
				cancelText : 'Stay on page',
				'solo' : false,
				'dialogOptions' : {
					autoOpen : true,
					modal : true,
					width : 400,
					height: 'auto',
					position : {
						at : "right top",
						my : "right bottom",
						of : self.dialog
					}
				},
				confirmFunction : function() {
					self.unloadFunction = function(e) {
						return "There is an ongoing upload which will be interrupted if you leave this page, do you wish to continue?";
					};
					$(window).bind('beforeunload', self.unloadFunction);
					self.remove();
					this.closeConfirm = null;
					return false;
				},
				additionalButtons : {
					'Close and abort' : function() {
						$(this).dialog("close");
						if (self.xhr)
							self.xhr.abort();
						self.remove();
						this.closeConfirm = null;
					}
				}
			});
			return false;
		} else {
			this.remove();
		}
	};
	
	AbstractFileUploadForm.prototype.remove = function() {
		if (this.closed) return;
		this.closed = true;
		this.dialog.remove();
		if (this.overlay)
			this.overlay.remove();
		if (this.closeConfirm)
			this.closeConfirm.remove();
	};
	
	AbstractFileUploadForm.prototype.readableFileSize = function(size) {
		var fileSize = 0;
		if (size > 1024 * 1024 * 1024)
			fileSize = (Math.round(size * 100 / (1024 * 1024 * 1024)) / 100).toString() + 'gb';
		if (size > 1024 * 1024)
			fileSize = (Math.round(size * 100 / (1024 * 1024)) / 100).toString() + 'mb';
		else
			fileSize = (Math.round(size * 100 / 1024) / 100).toString() + 'kb';
		return fileSize;
	};
	
	AbstractFileUploadForm.prototype.supportsAjaxUpload = function() {
		return new XMLHttpRequest().upload;
	};
	
	AbstractFileUploadForm.prototype.submitAjax = function() {
		var self = this, $form = this.$form.find("form"), formData = new FormData($form[0]);
		this.uploadInProgress = true;
		
		// Set up the request for XHR2 clients, register events
		this.xhr = new XMLHttpRequest();
		// Finished sending to queue without any network errors
		this.xhr.addEventListener("load", function(event) {
			self.uploadCompleted();
			var data = null;
			try {
				data = JSON.parse(this.responseText);
			} catch (e) {
				if (typeof console != "undefined") console.log("Failed to parse ingest response", e);
			}
			// Check for upload errors
			if (this.status >= 400) {
				self.options.alertHandler.alertHandler("error", self.getErrorMessage(data));
			} else {
				// Ingest queueing was successful, let the user know and close the form
				self.options.alertHandler.alertHandler("success", self.getSuccessMessage(data));
				self.remove();
			}
		}, false);
		
		// Failed due to network problems
		this.xhr.addEventListener("error", function(event) {
			self.options.alertHandler.alertHandler("error", self.getErrorMessage());
			self.uploadCompleted();
		}, false);
		
		if (this.options.showUploadProgress) {
			// Upload aborted by the user
			this.xhr.addEventListener("abort", function(event) {
				self.options.alertHandler.alertHandler("message", "Cancelled upload of " + self.ingestFile.name);
				self.uploadCompleted();
			}, false);
			
			// Update the progress bar
			this.xhr.upload.addEventListener("progress", function(event) {
				if (self.closed) return;
				if (event.total > 0) {
					var percent = event.loaded / event.total * 100;
					self.overlay.setProgress(percent);
				}
				if (event.loaded == event.total) {
					self.uploadInProgress = false;
					self.overlay.setText("upload complete, processing...");
				}
			}, false);
		}
		
		this.xhr.open("POST", this.$form.find("form")[0].action);
		this.xhr.send(formData);
	};
	
	AbstractFileUploadForm.prototype.uploadCompleted = function() {
		this.hideOverlay();
		this.submitted = false;
		this.uploadInProgress = false;
		if (this.unloadFunction) {
			$(window).unbind('beforeunload', this.unloadFunction);
			this.unloadFunction = null;
		}
	};
	
	// Traditional iframe file upload approach, monitor an iframe for changes to know when the submit completes
	AbstractFileUploadForm.prototype.submitIFrame = function() {
		var self = this;
		$(this.options.iframeSelector).load(function(){
			if (!this.contentDocument.body.innerHTML)
				return;
			self.uploadCompleted();
			var data = null;
			try {
				if (this.contentDocument.body.innerHTML)
					data = JSON.parse(this.contentDocument.body.innerHTML);
			} catch (e) {
				if (typeof console != "undefined") console.log("Failed to parse ingest response", e);
			}
			if (data.error) {
				self.options.alertHandler.alertHandler("error", self.getErrorMessage(data));
			} else {
				self.options.alertHandler.alertHandler("success", self.getSuccessMessage(data));
				self.remove();
			}
		});
	};
	
	AbstractFileUploadForm.prototype.setError = function(errorText) {
		$(".errors", this.$form).show();
		$(".error_stack", this.$form).html(errorText);
		this.dialog.dialog("option", "position", "center");
	};
	
	// Validate the form and retrieve any errors
	AbstractFileUploadForm.prototype.validationErrors = function() {
		var errors = [];
		var packageFile = $("input[type='file']", this.$form).val();
		if (!packageFile)
			errors.push("You must select a file to ingest");
		return errors;
	};
	
	AbstractFileUploadForm.prototype.preprocessForm = function() {
	};
	
	AbstractFileUploadForm.prototype.hideOverlay = function() {
		if (this.closed) return;
		if (this.closeConfirm)
			this.closeConfirm.close();
		this.overlay.close();
		if (this.options.showUploadProgress) {
			this.overlay.setProgress(0);
			this.overlay.setText("uploading...");
		}
	};
	
	return AbstractFileUploadForm;
});define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'CreateContainerForm', 'IngestPackageForm', 'CreateSimpleObjectForm', 'qtip'],
		function($, ui, _, CreateContainerForm, IngestPackageForm, CreateSimpleObjectForm) {
	
	function AddMenu(options) {
		this.options = $.extend({}, options);
		this.init();
	};
	
	AddMenu.prototype.getMenuItems = function() {
		var items = {};
		if ($.inArray('addRemoveContents', this.options.container.permissions) == -1)
			return items;
		items["addContainer"] = {name : "Add Container"};
		items["ingestPackage"] = {name : "Add Ingest Package"};
		items["simpleObject"] = {name : "Add Simple Object"};
		return items;
	};
	
	AddMenu.prototype.init = function() {
		var self = this;
		
		var items = self.getMenuItems();
		if (items.length == 0)
			return;
		var createContainerForm = new CreateContainerForm({
			alertHandler : this.options.alertHandler
		});
		var ingestPackageForm = new IngestPackageForm({
			alertHandler : this.options.alertHandler
		});
		var simpleObjectForm = new CreateSimpleObjectForm({
			alertHandler : this.options.alertHandler
		});
		
		$.contextMenu({
			selector: this.options.selector,
			trigger: 'left',
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
					case "addContainer" :
						createContainerForm.open(self.options.container.id);
						break;
					case "ingestPackage" :
						ingestPackageForm.open(self.options.container.id);
						break;
					case "simpleObject" :
						simpleObjectForm.open(self.options.container.id);
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
		});
	};
	
	return AddMenu;
});/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
/*
 * @author Ben Pennell
 */
define('AjaxCallbackButton', [ 'jquery', 'jquery-ui', 'RemoteStateChangeMonitor', 'ConfirmationDialog'], function(
		$, ui, RemoteStateChangeMonitor, ConfirmationDialog) {
	function AjaxCallbackButton(options) {
		this._create(options);
	};
	
	AjaxCallbackButton.prototype.defaultOptions = {
			pid : null,
			metadata : undefined,
			element : undefined,
			defaultLabel : undefined,
			workLabel : undefined,
			workPath : "",
			workDone : undefined,
			workDoneTarget : undefined,
			followup : undefined,
			followupTarget : undefined,
			followupPath : "",
			followupLabel : undefined,
			followupFrequency : 1000,
			completeTarget : undefined,
			parentElement : undefined,
			animateSpeed : 80,
			confirm : false,
			confirmMessage : "Are you sure?",
			confirmPositionElement : undefined,
			alertHandler : "#alertHandler"
		};

	AjaxCallbackButton.prototype._create = function(options, element) {
		this.options = $.extend({}, this.defaultOptions, options);
		
		if (element) {
			this.element = element;
			if (!this.options.defaultLabel)
				this.options.defaultLabel = this.element.text();
			if (!this.options.workLabel)
				this.options.workLabel = this.element.text();
			this.element.addClass("ajaxCallbackButton");
		}
		if (this.options.workDoneTarget == undefined)
			this.options.workDoneTarget = this;
		if (this.options.completeTarget == undefined)
			this.options.completeTarget = this;
		if (this.options.followupTarget == undefined)
			this.options.followupTarget = this;
		if (this.options.setText == undefined)
			this.options.setText = this.setText;
		if (this.options.followupError == undefined)
			this.options.followupError = this.followupError;
		if (this.options.followupErrorTarget == undefined)
			this.options.followupErrorTarget = this;
		this.alertHandler = $(this.options.alertHandler);
		
		if (this.options.pid) {
			this.pid = this.options.pid;
		}
		this.setWorkURL(this.options.workPath);
		
		this.followupId = null;
		this._init();
	};
	
	AjaxCallbackButton.prototype._init = function() {
		var op = this;
		
		if (this.options.followup) {
			this.setFollowupURL(this.options.followupPath);

			this.followupMonitor = new RemoteStateChangeMonitor({
				'checkStatus' : this.options.followup,
				'checkStatusTarget' : this.options.followupTarget,
				'checkError' : this.options.followupError,
				'checkErrorTarget' : this.options.followupErrorTarget,
				'statusChanged' : this.completeState,
				'statusChangedTarget' : this.options.completeTarget, 
				'checkStatusAjax' : {
					url : this.followupURL,
					dataType : 'json'
				}
			});
		}
		
		if (this.options.confirm) {
			var dialogOptions = {
					width : 'auto',
					modal : true
				};
			if (this.options.parentObject)
				dialogOptions['close'] = function() {
					op.options.parentObject.unhighlight();
				};
			if (this.options.confirmAnchor) {
				dialogOptions['position'] = {};
				dialogOptions['position']['of'] = this.options.confirmAnchor; 
			}
			
			this.confirmationDialog = new ConfirmationDialog({
				'promptText' : this.options.confirmMessage,
				'confirmFunction' : this.doWork,
				'confirmTarget' : this,
				'dialogOptions' : dialogOptions
			});
		}

		/*this.element.text(this.options.defaultLabel);
		this.element.click(function() {
			op.activate.call(op);
			return false;
		});*/
		
		if (this.options.disabled){
			this.disable();
		} else {
			this.enable();
		}
	};
	
	AjaxCallbackButton.prototype.activate = function() {
		if (this.options.disabled)
			return;
		if (this.options.confirm) {
			if (this.options.parentObject)
				this.options.parentObject.highlight();
			this.confirmationDialog.open();
		} else {
			this.doWork();
		}
	};

	AjaxCallbackButton.prototype.doWork = function(workMethod, workData) {
		if (this.options.disabled)
			return;
		this.performWork($.get, null);
	};

	AjaxCallbackButton.prototype.workState = function() {
		this.disable();
		if (this.options.parentObject) {
			this.options.parentObject.setState("working");
			this.options.parentObject.setStatusText(this.options.workLabel);
		} else if (this.element) {
			this.element.text(this.options.workLabel);
		}
	};

	AjaxCallbackButton.prototype.performWork = function(workMethod, workData) {
		this.workState();
		var op = this;
		workMethod(this.workURL, workData, function(data, textStatus, jqXHR) {
			if (op.options.followup) {
				if (op.options.workDone) {
					try {
						var workSuccessful = op.options.workDone.call(op.options.workDoneTarget, data);
						if (!workSuccessful)
							throw "Operation was unsuccessful";
					} catch (e) {
						op.alertHandler.alertHandler('error', e);
						if (op.options.parentObject)
							op.options.parentObject.setState("failed");
						return;
					}
				}
				if (op.options.parentObject)
					op.options.parentObject.setState("followup");
				op.followupMonitor.performPing();
			} else {
				if (op.options.parentObject)
					op.options.parentObject.setState("idle");
				if (op.options.complete)
					op.options.complete.call(op.options.completeTarget, data);
				op.enable();
			}
		}).fail(function(jqxhr, textStatus, error) {
			op.alertHandler.alertHandler('error', textStatus + ", " + error);
		});
	};
	
	AjaxCallbackButton.prototype.followupError = function(obj, errorText, error) {
		this.alertHandler.alertHandler('error', "An error occurred while checking the status of " + (this.options.metadata? this.options.metadata.title : "an object"));
		if (console && console.log)
			console.log((this.options.metadata? "Error while checking " + this.options.metadata.id + ": " : "") +errorText, error);
		if (this.options.parentObject)
			this.options.parentObject.setState("failed");
	};

	AjaxCallbackButton.prototype.disable = function() {
		this.options.disabled = true;
		if (this.element) {
			this.element.css("cursor", "default");
			this.element.addClass("disabled");
			this.element.attr('disabled', 'disabled');
		}
	};

	AjaxCallbackButton.prototype.enable = function() {
		this.options.disabled = false;
		if (this.element) {
			this.element.css("cursor", "pointer");
			this.element.removeClass("disabled");
			this.element.removeAttr('disabled');
		}
	};

	AjaxCallbackButton.prototype.setWorkURL = function(url) {
		this.workURL = url;
		this.workURL = this.resolveParameters(this.workURL);
	};

	AjaxCallbackButton.prototype.setFollowupURL = function(url) {
		this.followupURL = url;
		this.followupURL = this.resolveParameters(this.followupURL);
	};

	AjaxCallbackButton.prototype.resolveParameters = function(url) {
		if (!url || !this.pid)
			return url;
		return url.replace("{idPath}", this.pid);
	};

	AjaxCallbackButton.prototype.destroy = function() {
		if (this.element)
			this.element.unbind("click");
	};

	AjaxCallbackButton.prototype.followupState = function() {
		if (this.options.followupLabel != null) {
			if (this.options.parentObject)
				this.options.parentObject.setStatusText(this.options.followupLabel);
			else if (this.element) 
				this.element.text(this.options.followupLabel);

		}
	};

	AjaxCallbackButton.prototype.completeState = function(data) {
		if (this.options.parentObject) {
			this.options.parentObject.setState("idle");
		}
		this.enable();
		if (this.element)
			this.element.text(this.options.defaultLabel);
	};
	
	return AjaxCallbackButton;
});define('AlertHandler', ['jquery', 'jquery-ui', 'qtip'], function($) {
	$.widget("cdr.alertHandler", {
		_create: function() {
			// Utilise delegate so we don't have to rebind for every qTip!
			$(document).delegate('.qtip.jgrowl', 'mouseover mouseout', this.timer);
		},
		
		error: function(message) {
			this.showAlert(message, "error");
		},
		
		message: function(message) {
			this.showAlert(message, "info");
		},
		
		success: function(message) {
			this.showAlert(message, "success");
		},
		
		showAlert: function(messages, type) {
			if (!messages)
				return;
			if (messages instanceof Array) {
				for (var index in messages)
					this._renderAlert(messages[index], type);
			} else {
				this._renderAlert(messages, type);
			}
		},
		
		_renderAlert: function(message, type) {
			var target = $('.qtip.jgrowl:visible:last');
			var self = this;
			
			$(document.body).qtip({
					content: {
						text: message,
						title: {
							text: "",
							button: true
						}
					},
					position: {
						my: 'top right',
						at: (target.length ? 'bottom' : 'top') + ' right',
						target: target.length ? target : $(window),
						adjust: { 'y': 10, 'x' : target.length ? 0 : -5 },
						effect: function(api, newPos) {
							$(this).animate(newPos, {
									duration: 200,
									queue: false
							});
							api.cache.finalPos = newPos; 
						}
					},
					show: {
						event: false,
						// Don't show it on a regular event
						ready: true,
						// Show it when ready (rendered)
						effect: function() {
							$(this).stop(0, 1).fadeIn(400);
						},
						// Matches the hide effect
						delay: 0,
						persistent: false
					},
					hide: {
						event: false,
						// Don't hide it on a regular event
						effect: function(api) {
							// Do a regular fadeOut, but add some spice!
							$(this).stop(0, 1).fadeOut(400).queue(function() {
							// Destroy this tooltip after fading out
							api.destroy();
							// Update positions
							self.updateGrowls();
						});
					}
				},
				style: {
					classes: 'jgrowl qtip-admin qtip-rounded alert-' + type,
					tip: false
				},
				events: {
					render: function(event, api) {
						// Trigger the timer (below) on render
						self.timer.call(api.elements.tooltip, event);
					}
				}
			}).removeData('qtip');
		},
		
		updateGrowls : function() {
			// Loop over each jGrowl qTip
			var each = $('.qtip.jgrowl'),
				width = each.outerWidth(),
				height = each.outerHeight(),
				gap = each.eq(0).qtip('option', 'position.adjust.y'),
				pos;
	 
			each.each(function(i) {
				var api = $(this).data('qtip');
	 
				// Set target to window for first or calculate manually for subsequent growls
				api.options.position.target = !i ? $(window) : [
					pos.left + width, pos.top + (height * i) + Math.abs(gap * (i-1))
				];
				api.set('position.at', 'top right');
				
				// If this is the first element, store its finak animation position
				// so we can calculate the position of subsequent growls above
				if(!i) { pos = api.cache.finalPos; }
			});
		},
		
		// Setup our timer function
		timer : function(event) {
			var api = $(this).data('qtip'),
				lifespan = 5000;
			
			// If persistent is set to true, don't do anything.
			if (api.get('show.persistent') === true) { return; }
	 
			// Otherwise, start/clear the timer depending on event type
			clearTimeout(api.timer);
			if (event.type !== 'mouseover') {
				api.timer = setTimeout(api.hide, lifespan);
			}
		}
	});
});define('BatchCallbackButton', [ 'jquery', 'AjaxCallbackButton', 'ResultObjectList' ], function($, AjaxCallbackButton, ResultObjectList) {
	function BatchCallbackButton(options, element) {
		this._create(options, element);
	};
	
	BatchCallbackButton.prototype.constructor = BatchCallbackButton;
	BatchCallbackButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			resultObjectList : undefined,
			followupPath: "services/rest/item/solrRecord/version",
			childWorkLinkName : undefined,
			workFunction : undefined,
			followupFunction : undefined,
			completeFunction : undefined 
		};

	BatchCallbackButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workDone = this.workDone;
		merged.followup = this.followup;
		merged.completeTarget = this;
		AjaxCallbackButton.prototype._create.call(this, merged, element);
		this.followupMonitor.options.checkStatusAjax.type = 'POST';
	};

	BatchCallbackButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
		
		for (var index in this.targetIds) {
			var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
			resultObject.disable();
			if (this.options.workFunction)
				if ($.isFunction(this.options.workFunction))
					this.options.workFunction.call(resultObject);
				else
					resultObject[this.options.workFunction]();
		}
		
		var self = this;
		if (this.targetIds.length > 0) {
			this.performWork($.post, {
				'ids' : self.targetIds.join('\n')
			});
		} else {
			this.enable();
		}
	};

	BatchCallbackButton.prototype.workDone = function(data) {
		if ($.isArray(data)) {
			this.followupObjects = [];
			for (var index in data) {
				var id = data[index].pid;
				this.followupObjects.push(id);
				if (this.options.workFunction) {
					var resultObject = this.options.resultObjectList.resultObjects[id];
					if ($.isFunction(this.options.followupFunction))
						this.options.followupFunction.call(resultObject);
					else
						resultObject[this.options.followupFunction]();
				}
			}
			this.followupMonitor.pingData = {
					'ids' : this.followupObjects.join('\n')
			}; 
			return true;
		} else
			alert("Error while attempting to perform action: " + data);
		return false;
	};

	BatchCallbackButton.prototype.followup = function(data) {
		for (var id in data) {
			if (this.options.resultObjectList.resultObjects[id].updateVersion(data[id])) {
				var index = $.inArray(id, this.followupObjects);
				if (index != -1) {
					this.followupObjects.splice(index, 1);
					
					var resultObject = this.options.resultObjectList.resultObjects[id];
					resultObject.setState("idle");
					
					if (this.options.completeFunction) {
						if ($.isFunction(this.options.completeFunction))
							this.options.completeFunction.call(resultObject);
						else
							resultObject.resultObject(this.options.completeFunction);
					}
				}
			}
		}
		this.followupMonitor.pingData = {
				'ids' : this.followupObjects.join('\n')
		}; 
		return this.followupObjects.length == 0;
	};
	
	BatchCallbackButton.prototype.completeState = function(id) {
		this.targetIds = null;
		this.enable();
	};

	BatchCallbackButton.prototype.getTargetIds = function() {
		var targetIds = [];

		$.each(this.options.resultObjects, function() {
			var resultObject = this;
			if (this.isSelected()) {
				targetIds.push(resultObject.getPid());
			}
		});

		return targetIds;
	};
	return BatchCallbackButton;
});
/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
/*
 * @author Ben Pennell
 */
define('ConfirmationDialog', [ 'jquery', 'jquery-ui', 'PID', 'RemoteStateChangeMonitor'], function(
		$, ui, PID, RemoteStateChangeMonitor) {
	function ConfirmationDialog(options) {
		this._create(options);
	};
	
	$.extend(ConfirmationDialog.prototype, {
		options : {
			promptText : 'Are you sure?',
			confirmFunction : undefined,
			confirmTarget : undefined,
			confirmText : 'Yes',
			cancelTarget : undefined,
			cancelFunction : undefined,
			cancelText : 'Cancel',
			solo : true,
			additionalButtons : undefined,
			autoOpen : false,
			addClass : undefined
		},
		
		dialogOptions : {
			modal : false,
			minHeight : 60,
			autoOpen : false,
			resizable : false,
			dialogClass : "no_titlebar confirm_dialog",
			position : {
				my : "right top",
				at : "right bottom"
			}
		},
		
		_create : function(options) {
			$.extend(this.options, options);
			if ('dialogOptions' in this.options)
				$.extend(this.dialogOptions, this.options.dialogOptions);
			var self = this;
			
			this.confirmDialog = $("<div class='confirm_dialogue'></div>");
			if (this.options.promptText === undefined) {
				this.confirmDialog.append("<p>Are you sure?</p>");
			} else {
				this.confirmDialog.append("<p>" + this.options.promptText + "</p>");
			}
			$("body").append(this.confirmDialog);
			
			var buttonsObject = this._generateButtons();
			
			$.extend(this.dialogOptions, {
				open : function() {
					if (self.options.solo) {
						$.each($('div.ui-dialog-content'), function (i, e) {
							if ($(this).dialog("isOpen") && this !== self.confirmDialog[0]) 
								$(this).dialog("close");
						});
					}
				},
				buttons : buttonsObject
			});
			this.confirmDialog.dialog(this.dialogOptions);
			if (this.options.addClass)
				this.confirmDialog.addClass(this.options.addClass);
			if (this.options.autoOpen)
				this.open();
		},
		
		_generateButtons : function() {
			var buttonsObject = {}, self = this;
			
			buttonsObject[this.options.cancelText] = function() {
				if (self.options.cancelFunction) {
					var result = self.options.cancelFunction.call(self.options.cancelTarget);
					if (result !== undefined && !result)
						return;
				}
				$(this).dialog("close");
			};
			
			buttonsObject[this.options.confirmText] = function() {
				if (self.options.confirmFunction) {
					var result = self.options.confirmFunction.call(self.options.confirmTarget);
					if (result !== undefined && !result)
						return;
				}
				$(this).dialog("close");
			};
			
			// Add any additional buttons in
			if (this.options.additionalButtons) {
				for (var index in this.options.additionalButtons)
					buttonsObject[index] = this.options.additionalButtons[index];
			}
			return buttonsObject;
		},
		
		open : function () {
			this.confirmDialog.dialog('open');
		},
		
		close : function () {
			this.confirmDialog.dialog('close');
		},
		
		remove : function() {
			this.confirmDialog.dialog('close');
			this.confirmDialog.remove();
		}
	});
	return ConfirmationDialog;
});define('CreateContainerForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createContainerForm', 
		'ModalLoadingOverlay', 'AbstractFileUploadForm'], 
		function($, ui, _, RemoteStateChangeMonitor, createContainerForm, ModalLoadingOverlay, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Create container',
			createFormTemplate : createContainerForm,
			showUploadProgress : false
	};
	
	function CreateContainerForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	CreateContainerForm.prototype.constructor = CreateContainerForm;
	CreateContainerForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	CreateContainerForm.prototype.validationErrors = function() {
		var errors = [];
		var description = $("input[type='file']", this.$form).val();
		// Validate input
		if (!this.containerName)
			errors.push("You must specify a name for the " + this.containerType);
		return errors;
	};
	
	CreateContainerForm.prototype.preprocessForm = function() {
		this.containerName = $("input[name='name']", this.$form).val();
		this.containerType = $("select", this.$form).val();
	};
	
	CreateContainerForm.prototype.getSuccessMessage = function(data) {
		return this.containerType + " " + this.containerName + " has been successfully created.";
	};
	
	CreateContainerForm.prototype.getErrorMessage = function(data) {
		return "An error occurred while creating " + this.containerType + " " + this.containerName;
	};
	
	return CreateContainerForm;
});/**
 * Implements functionality and UI for the generic Ingest Package form
 */
define('CreateSimpleObjectForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createSimpleObjectForm', 
		'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'], 
		function($, ui, _, RemoteStateChangeMonitor, simpleObjectTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Add Simple Object',
			createFormTemplate : simpleObjectTemplate
	};
	
	function CreateSimpleObjectForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	CreateSimpleObjectForm.prototype.constructor = CreateSimpleObjectForm;
	CreateSimpleObjectForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	CreateSimpleObjectForm.prototype.preprocessForm = function() {
		var label = $("input[name='name']", this.$form).val();
		if (!label && this.ingestFile) {
			$("input[name='name']", this.$form).val(this.ingestFile.name);
		}
	};
	
	// Validate the form and retrieve any errors
	CreateSimpleObjectForm.prototype.validationErrors = function() {
		var errors = [];
		var dataFile = $("input[type='file']", this.$form).val();
		if (!dataFile)
			errors.push("You must select a file to ingest");
		return errors;
	};
	
	CreateSimpleObjectForm.prototype.getSuccessMessage = function(data) {
		return this.ingestFile.name + " has been successfully uploaded for ingest.  You will receive an email when it completes.";
	};
	
	CreateSimpleObjectForm.prototype.getErrorMessage = function(data) {
		var message = "Failed to ingest file " + this.ingestFile.name + ".";
		if (data && data.errorStack && !this.closed) {
			message += "  See errors below.";
			this.setError(data.errorStack);
		}
		return message;
	};
	
	return CreateSimpleObjectForm;
});define('DeleteBatchButton', [ 'jquery', 'BatchCallbackButton' ], function($, BatchCallbackButton) {
	function DeleteBatchButton(options, element) {
		this._create(options, element);
	};
	
	DeleteBatchButton.prototype.constructor = DeleteBatchButton;
	DeleteBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
		workPath: "delete",
		childWorkLinkName : "delete",
		confirm: true,
		confirmMessage: "Delete selected object(s)?",
		animateSpeed: 'fast'
	};
	
	DeleteBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};

	DeleteBatchButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (resultObject.isSelected() && resultObject.isEnabled()) {
				targetIds.push(resultObject.getPid());
			}
		}
		return targetIds;
	};
	
	DeleteBatchButton.prototype.followup = function(data) {
		var removedIds;
		var emptyData = jQuery.isEmptyObject(data.length);
		if (emptyData){
			removedIds = this.followupObjects;
		} else {
			removedIds = [];
			for (var index in this.followupObjects) {
				var id = this.followupObjects[index];
				if (!(id in data)) {
					removedIds.push(id);
				}
			}
		}
		
		if (removedIds.length > 0) {
			if (emptyData)
				this.followupObjects = null;
			for (var index in removedIds) {
				var id = removedIds[index];
				// Don't bother trimming out followup objects if all ids are complete
				if (!emptyData) {
					var followupIndex = $.inArray(id, this.followupObjects);
					this.followupObjects.splice(followupIndex, 1);
				}
				
				var resultObject = this.options.resultObjectList.resultObjects[id];
				// Trigger the complete function on targeted child callback buttons
				if (this.options.completeFunction) {
					if ($.isFunction(this.options.completeFunction))
						this.options.completeFunction.call(resultObject);
					else
						resultObject[this.options.completeFunction]();
				} else {
					resultObject.setState("idle");
				}
			}
		}
		
		return !this.followupObjects;
	};
	
	return DeleteBatchButton;
});define('DeleteObjectButton', [ 'jquery', 'AjaxCallbackButton'], function($, AjaxCallbackButton) {
	function DeleteObjectButton(options) {
		this._create(options);
	};
	
	DeleteObjectButton.prototype.constructor = DeleteObjectButton;
	DeleteObjectButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			workLabel: "Deleting...",
			workPath: "delete/{idPath}",
			followupLabel: "Cleaning up...",
			followupPath: "services/rest/item/{idPath}/solrRecord/version",
			confirm: true,
			confirmMessage: "Delete this object?",
			animateSpeed: 'fast',
			workDone: DeleteObjectButton.prototype.deleteWorkDone,
			followup: DeleteObjectButton.prototype.deleteFollowup,
			complete: DeleteObjectButton.prototype.complete
		};
		
	DeleteObjectButton.prototype._create = function(options) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workDone = DeleteObjectButton.prototype.deleteWorkDone;
		merged.followup = DeleteObjectButton.prototype.deleteFollowup;
		merged.complete = DeleteObjectButton.prototype.complete;
		AjaxCallbackButton.prototype._create.call(this, merged);
		
		if (this.options.parentObject)
			this.options.confirmAnchor = this.options.parentObject.element;
	};

	DeleteObjectButton.prototype.deleteFollowup = function(data) {
		if (data == null) {
			return true;
		}
		return false;
	};
	
	DeleteObjectButton.prototype.complete = function() {
		if (this.options.metadata)
			this.alertHandler.alertHandler("success", "Successfully deleted item " + metadata.title + " (" + metadata.id + ")");
		else this.alertHandler.alertHandler("success", "Successfully deleted item " + data);
	};

	DeleteObjectButton.prototype.completeState = function() {
		if (this.options.parentObject != null)
			this.options.parentObject.deleteElement();
		this.destroy();
	};

	DeleteObjectButton.prototype.deleteWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "An error occurred while attempting to delete object " + this.pid;
			}
		} else jsonData = data;
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	return DeleteObjectButton;
});define('EditAccessControlForm', [ 'jquery', 'jquery-ui', 'ModalLoadingOverlay', 'ConfirmationDialog', 'AlertHandler', 
         'editable', 'moment', 'qtip'], function($, ui, ModalLoadingOverlay, ConfirmationDialog) {
	$.widget("cdr.editAccessControlForm", {
		_create : function() {
			var self = this;
			self.aclNS = this.options.namespace;
			
			this.alertHandler = $("#alertHandler");
			
			this.accessControlModel = $($(this.options.xml).children()[0]).clone();
			this.originalDocument = this.xml2Str(this.accessControlModel);
			this.aclPrefix = this.getNamespacePrefix(this.accessControlModel, self.aclNS);
			
			$.fn.editable.defaults.mode = 'inline';
			this.addEmbargo = $(".add_embargo", this.element).editable({
				emptytext: 'Add embargo',
				format: 'MM/DD/YYYY',
				viewformat: 'MM/DD/YYYY',
				template: 'MM/DD/YYYY',
				clear: true,
				combodate: {
					minYear: moment().year(),
					maxYear: moment().add('years', 75).year(),
					minuteStep: 1,
					yearDescending: true
				}
			}).on('save', function(e, params) {
				if (params.newValue == null || params.newValue == "") {
					self.removeAttribute(self.accessControlModel, 'embargo-until', self.aclPrefix);
					return;
				}
				var formattedDate = moment(params.newValue).format('YYYY-MM-DD[T]HH:mm:ss');
				self.addAttribute(self.accessControlModel, 'embargo-until', formattedDate, self.aclNS, self.aclPrefix);
			});
			
			$(".roles_granted .remove_group", this.element).hide();
			
			$(".boolean_toggle", this.element).click(function(){
				$.proxy(self.toggleField(this), self);
				return false;
			});
			
			$(".inherit_toggle", this.element).click(function(){
				$.proxy(self.toggleField(this), self);
				var rolesGranted = $('.roles_granted', self.element);
				rolesGranted.toggleClass('inheritance_disabled');
			});
			
			$(".edit_role_granted a", this.element).click(function(){
				$(".roles_granted a", self.element).show();
				$(".edit_role_granted", self.element).hide();
				$(".add_role_granted", self.element).show();
				return false;
			});
			
			$(".add_group_name, .add_role_name", this.element).keypress(function(e){
				var code = (e.keyCode ? e.keyCode : e.which);
				if (code == 13) {
					$(".add_role_button", self.element).click();
					e.preventDefault();
				}
			});
			
			$('.add_group_name').one('focus', function(){
				var addGroup = $(this);
				$.getJSON(self.options.groupSuggestionsURL, function(data){
					addGroup.autocomplete({
						source : data
					});
				});
			});
			
			$(".add_role_button", this.element).click(function(){
				var roleValue = $(".add_role_name", self.element).val();
				var groupName = $.trim($(".add_group_name", self.element).val());
				if (roleValue == "" || groupName == "" || self.groupRoleExists(self.accessControlModel, roleValue, groupName, self.aclPrefix))
					return false;
				
				var roleRow = $("tr.role_groups[data-value='" + roleValue +"']", self.element);
				if (roleRow.length == 0) {
					roleRow = $("<tr class='role_groups' data-value='" + roleValue + "'><td class='role'>" + 
							roleValue + "</td><td class='groups'></td></tr>");
					$(".edit_role_granted", self.element).before(roleRow);
				}
				
				var grantElement = $(self.addElement(self.accessControlModel, 'grant', self.aclNS, self.aclPrefix));
				self.addAttribute(grantElement, 'role', roleValue, self.aclNS, self.aclPrefix);
				self.addAttribute(grantElement, 'group', groupName, self.aclNS, self.aclPrefix);
				
				$(".groups", roleRow).append("<span>" + groupName + "</span><a class='remove_group'>x</a><br/>");
				$('.add_group_name').autocomplete('search');
			});
			
			$(this.element).on("click", ".roles_granted .remove_group", function(){
				var groupName = $(this).prev("span").html();
				var roleValue = $(this).parents('.role_groups')[0].getAttribute('data-value');
				self.accessControlModel.children().each(function(){
					var group = self.getAttribute($(this), 'group', self.aclNS);
					var role = self.getAttribute($(this), 'role', self.aclNS);
					if (group == groupName && role == roleValue) {
						$(this).remove();
						return false;
					}
				});
				
				$(this).prev("span").remove();
				$(this).next("br").remove();
				var parentTd = $(this).parent();
				if (parentTd.children("span").length == 0){
					parentTd.parent().remove();
				}
				$(this).remove();
			});
			
			var containing = this.options.containingDialog;
			$('.update_button').click(function(){
				var container = ((self.options.containingDialog)? self.options.containingDialog : $(body));
				var overlay = new ModalLoadingOverlay(container);
				$.ajax({
					url : self.options.updateUrl,
					type : 'PUT',
					data : self.xml2Str(self.accessControlModel),
					success : function(data) {
						containing.data('can-close', true);
						overlay.remove();
						if (self.options.containingDialog != null) {
							self.options.containingDialog.dialog('close');
						}
						self.alertHandler.alertHandler('success', 'Access control changes saved');
						$("#res_" + self.options.pid.substring(self.options.pid.indexOf(':') + 1)).data('resultObject').refresh();
					},
					error : function(data) {
						overlay.remove();
						self.alertHandler.alertHandler('error', 'Failed to save changes: ' + data);
					}
				});
			});
			
			if (this.options.containingDialog) {
				containing.data('can-close', false);
				var confirmationDialog = new ConfirmationDialog({
					'promptText' : 'There are unsaved access control changes, close without saving?',
					'confirmFunction' : function() {
						containing.data('can-close', true);
						containing.dialog('close');
					},
					'solo' : false,
					'dialogOptions' : {
						modal : true,
						minWidth : 200,
						maxWidth : 400,
						position : {
							at : "center center"
						}
					}
				});
				
				containing.on('dialogbeforeclose', function(){
					if (!containing.data('can-close') && self.isDocumentChanged()) {
						confirmationDialog.open();
						return false;
					} else {
						return true;
					}
				});
			}
		},
		
		toggleField: function(fieldElement) {
			var fieldName = $(fieldElement).attr("data-field");
			if ($.trim($(fieldElement).html()) == "Yes") {
				$(fieldElement).html("No");
				this.addAttribute(this.accessControlModel, fieldName, 'false', this.aclNS, this.aclPrefix);
				return false;
			} else { 
				$(fieldElement).html("Yes");
				this.addAttribute(this.accessControlModel, fieldName, 'true', this.aclNS, this.aclPrefix);
				return true;
			}
		},
		
		getNamespacePrefix: function(node, namespace) {
			var prefix = null;
			var attributes = node[0].attributes;
			$.each(attributes, function(key, value) {
				if (value.value == namespace) {
					var index = value.nodeName.indexOf(":");
					if (index == -1)
						prefix = "";
					else prefix = value.localName;
					return false;
				}
			});
			
			return prefix;
		},
		
		isDocumentChanged : function() {
			return this.originalDocument != this.xml2Str(this.accessControlModel);
		},
		
		groupRoleExists: function(xmlNode, roleName, groupName, namespacePrefix) {
			var prefix = "";
			if (namespacePrefix)
				prefix = namespacePrefix + "\\:";
			return $(prefix + "grant[" + prefix + "role='" + roleName + "'][" + prefix + "group='" + groupName + "']", xmlNode).length > 0;
		},
		
		addElement: function(xmlNode, localName, namespace, namespacePrefix) {
			var nodeName = localName;
			if (namespacePrefix != null && namespacePrefix != "") 
				nodeName = namespacePrefix + ":" + localName;
			var newElement = xmlNode[0].ownerDocument.createElementNS(namespace, nodeName);
			$(newElement).text("");
			xmlNode.append(newElement);
			return newElement;
		},
		
		removeAttribute: function(xmlNode, attrName, namespacePrefix) {
			if (namespacePrefix != null && namespacePrefix != "")
				xmlNode.removeAttr(namespacePrefix + ":" + attrName);
			else xmlNode.removeAttr(attrName);
		},
		
		getAttribute: function(xmlNode, attrName, namespace) {
			var attributes = xmlNode[0].attributes;
			for (var index in attributes) {
				if (attributes[index].localName == attrName && attributes[index].namespaceURI == namespace)
					return attributes[index].nodeValue;
			}
			return null;
		},
		
		addAttribute: function(xmlNode, attrName, attrValue, namespace, namespacePrefix) {
			if (namespacePrefix != null) {
				if (namespacePrefix == "")
					xmlNode.attr(attrName, attrValue);
				else xmlNode.attr(namespacePrefix + ":" + attrName, attrValue);
				return;
			}
			xmlNode = xmlNode[0];
			
		    var attr;
		    if (xmlNode.ownerDocument.createAttributeNS)
		       attr = xmlNode.ownerDocument.createAttributeNS(namespace, attrName);
		    else
		       attr = xmlNode.ownerDocument.createNode(2, attrName, namespace);

		    attr.nodeValue = attrValue;

		    //Set the new attribute into the xmlNode
		    if (xmlNode.setAttributeNodeNS)
		    	xmlNode.setAttributeNodeNS(attr);  
		    else
		    	xmlNode.setAttributeNode(attr);  
		    
		    return attr;
		},
		
		xml2Str: function(xmlNodeObject) {
			if (xmlNodeObject == null)
				return;
			var xmlNode = (xmlNodeObject instanceof jQuery? xmlNodeObject[0]: xmlNodeObject);
			var xmlStr = "";
			try {
				// Gecko-based browsers, Safari, Opera.
				xmlStr = (new XMLSerializer()).serializeToString(xmlNode);
			} catch (e) {
				try {
					// Internet Explorer.
					xmlStr = xmlNode.xml;
				} catch (e) {
					return false;
				}
			}
			return xmlStr;
		}
	});
});/**
 * Implements functionality and UI for the generic Ingest Package form
 */
define('IngestPackageForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/ingestPackageForm', 
		'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm'], 
		function($, ui, _, RemoteStateChangeMonitor, ingestPackageTemplate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm) {
	
	var defaultOptions = {
			title : 'Ingest package',
			createFormTemplate : ingestPackageTemplate
	};
	
	function IngestPackageForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	IngestPackageForm.prototype.constructor = IngestPackageForm;
	IngestPackageForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	// Validate the form and retrieve any errors
	IngestPackageForm.prototype.validationErrors = function() {
		var errors = [];
		var packageFile = $("input[type='file']", this.$form).val();
		if (!packageFile)
			errors.push("You must select a package file to ingest");
		return errors;
	};
	
	IngestPackageForm.prototype.getSuccessMessage = function(data) {
		return "Package " + this.ingestFile.name + " has been successfully uploaded for ingest.  You will receive an email when it completes.";
	};
	
	IngestPackageForm.prototype.getErrorMessage = function(data) {
		var message = "Failed to submit package " + this.ingestFile.name + " for ingest.";
		if (data && data.errorStack && !this.closed) {
			message += "  See errors below.";
			this.setError(data.errorStack);
		}
		return message;
	};
	
	return IngestPackageForm;
});define('MetadataObject', [ 'jquery', 'PID' ], function($, PID) {
	function MetadataObject(metadata) {
		this.init(metadata);
	};
	
	$.extend(MetadataObject.prototype, {
		data: null,
		
		init: function(metadata) {
			this.data = metadata;
			if (this.data === undefined || this.data == null) {
				this.data = {};
			}
			//this.setPID(this.data.id);
		},
		
		setPID: function(pid) {
			this.pid = new PID(pid);
		},
		
		isPublished: function() {
			if (!$.isArray(this.data.status)){
				return true;
			}
			return $.inArray("Unpublished", this.data.status) == -1;
		},
		
		publish: function () {
			if (!$.isArray(this.data.status)){
				this.data.status = [];
			} else {
				this.data.status.splice($.inArray("Unpublished", this.data.status), 1);
			}
			this.data.status.push("Published");
		},
		
		unpublish: function () {
			if (!$.isArray(this.data.status)){
				this.data.status = [];
			} else {
				this.data.status.splice($.inArray("Published", this.data.status), 1);
			}
			this.data.status.push("Unpublished");
		}
	});
	
	return MetadataObject;
});define('ModalLoadingOverlay', [ 'jquery', 'jquery-ui', 'editable', 'moment', 'qtip'], function($) {
	var defaultOptions = {
		text : null,
		type : "icon", // text, icon, determinate
		iconSize : 'large',
		autoOpen : true,
		dialog : false
	};
	
	function ModalLoadingOverlay(element, options) {
		this.options = $.extend({}, defaultOptions, options);
		this.element = element;
		this.init();
	}
	
	ModalLoadingOverlay.prototype.init = function() {
		this.overlay = $('<div class="load_modal"></div>');
		if (this.options.type == "determinate") {
			this.loadingBar = $("<div></div>").progressbar({
				value : 0
			});
			this.overlay.append(this.loadingBar);
		} else if (this.options.type == "icon")
			this.overlay.addClass('icon_' + this.options.iconSize);
		else if (this.options.type == "text")
			this.textIcon = $('<div class="text_icon icon_' + this.options.iconSize + '"></div>').appendTo(this.overlay);
		
		if (this.options.text)
			this.setText(this.options.text);
		
		this.overlay.appendTo(document.body);
		
		$(window).resize($.proxy(this.resize, this));
		var self = this;
		if (this.options.dialog) {
			this.options.dialog.on("dialogdrag", function(event, ui){
				self.resize();
			});
		}
		
		if (this.options.autoOpen)
			this.open();
		else this.close();
	};
	
	ModalLoadingOverlay.prototype.close = function() {
		this.overlay.hide();
	};
	
	ModalLoadingOverlay.prototype.remove = function() {
		this.overlay.remove();
	};
	
	ModalLoadingOverlay.prototype.open = function() {
		this.overlay.css({'visibility': 'hidden', 'display' : 'block'});
		if (this.element != $(document))
			this.resize();
		this.overlay.css('visibility', 'visible');
	};
	
	ModalLoadingOverlay.prototype.resize = function() {
		this.overlay.css({'width' : this.element.innerWidth(), 'height' : this.element.innerHeight(),
			'top' : this.element.offset().top, 'left' : this.element.offset().left});
		this.adjustContentPosition();
	};
	
	ModalLoadingOverlay.prototype.adjustContentPosition = function() {
		if (this.options.type == "determinate") {
			var topOffset = (this.element.innerHeight() - this.loadingBar.outerHeight()) / 2,
				leftOffset = (this.element.innerWidth() - this.loadingBar.width()) / 2;
			this.loadingBar.css({top : topOffset, left : leftOffset});
		} else {
			if (this.textSpan) {
				var topOffset = (this.element.innerHeight() - this.textSpan.outerHeight()) / 2;
				this.textSpan.css('top', topOffset);
				if (this.textIcon)
					this.textIcon.css('top', topOffset);
			}
		}
	};
	
	ModalLoadingOverlay.prototype.setText = function(text) {
		if (!this.textSpan) {
			this.textSpan = $('<span>' + text + '</span>');
			if (this.options.type == "text")
				this.overlay.prepend(this.textSpan);
			else if (this.options.type == "determinate")
				this.loadingBar.append(this.textSpan);
			else
				this.overlay.append(this.textSpan);
		} else {
			this.textSpan.html(text);
		}
		this.adjustContentPosition();
	};
	
	// Updates the progress bar to a percentage of completion.  Only applies to determinate overlays
	ModalLoadingOverlay.prototype.setProgress = function(value) {
		this.loadingBar.progressbar("value", value);
	};
	
	return ModalLoadingOverlay;
});define('PID', ['jquery'], function($) {
	function PID(pidString) {
		this.init(pidString);
	};
	
	$.extend(PID.prototype, {
		uriPrefix: "info:fedora/",
		pid: null,
		
		init: function(pidString) {
			if (pidString.indexOf(this.uriPrefix) == 0) {
				this.pid = pidString.substring(this.uriPrefix.length());
			} else {
				this.pid = pidString;
			}
		},
		
		getPid: function() {
			return this.pid;
		},
		
		getURI: function() {
			return this.urlPrefix + this.pid;
		},
		
		getPath: function() {
			return this.pid.replace(":", "/");
		}
	});
	
	return PID;
});/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
define('ParentResultObject', [ 'jquery', 'ResultObject'], 
		function($, ResultObject) {
	
	function ParentResultObject(options) {
		ResultObject.call(this, options);
	};
	
	ParentResultObject.prototype.constructor = ParentResultObject;
	ParentResultObject.prototype = Object.create( ResultObject.prototype );
	
	ParentResultObject.prototype.init = function(metadata) {
		this.metadata = metadata;
		this.pid = metadata.id;
		this.actionMenuInitialized = false;
		this.element = this.options.element;
		this.element.data('resultObject', this);
		this.links = [];
	};
	
	return ParentResultObject;
});define('PublishBatchButton', [ 'jquery', 'BatchCallbackButton' ], function($, BatchCallbackButton) {
	function PublishBatchButton(options, element) {
		this._create(options, element);
	};
	
	PublishBatchButton.prototype.constructor = PublishBatchButton;
	PublishBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
		resultObjectList : undefined,
		workPath: "services/rest/edit/publish",
		childWorkLinkName : 'publish'
	};
	
	PublishBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};

	PublishBatchButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (resultObject.isSelected() && $.inArray("Unpublished", resultObject.getMetadata().status) != -1
					&& resultObject.isEnabled()) {
				targetIds.push(resultObject.getPid());
			}
		}
		return targetIds;
	};
	return PublishBatchButton;
});define('PublishObjectButton', [ 'jquery', 'jquery-ui', 'AjaxCallbackButton', 'ResultObject'], function($, ui, AjaxCallbackButton) {
	function PublishObjectButton(options) {
		this._create(options);
	};
	
	PublishObjectButton.prototype.constructor = PublishObjectButton;
	PublishObjectButton.prototype = Object.create( AjaxCallbackButton.prototype );
	
	var defaultOptions = {
			defaultPublish: false,
			followupPath: "services/rest/item/{idPath}/solrRecord/version"
		};
		
	PublishObjectButton.prototype._create = function(options) {
		var merged = $.extend({}, defaultOptions, options);
		merged.workDone = this.publishWorkDone;
		merged.followup = this.publishFollowup;
		AjaxCallbackButton.prototype._create.call(this, merged);
		
		this.published = this.options.defaultPublish;
		if (this.published) {
			this.publishedState();
		} else {
			this.unpublishedState();
		}
	};

	PublishObjectButton.prototype.publishFollowup = function(data) {
		if (data) {
			return this.options.parentObject.updateVersion(data);
		}
		return false;
	};
	
	PublishObjectButton.prototype.completeState = function() {
		if (this.options.parentObject) {
			this.options.parentObject.refresh(true);
		} else {
			this.toggleState();
		}
		this.enable();
	};
	
	PublishObjectButton.prototype.toggleState = function() {
		if (this.published) {
			this.unpublishedState();
		} else {
			this.publishedState();
		}
	};

	PublishObjectButton.prototype.publishedState = function() {
		this.published = true;
		this.setWorkURL("services/rest/edit/unpublish/{idPath}");
		this.options.workLabel = "Unpublishing...";
		this.options.followupLabel = "Unpublishing....";
	};

	PublishObjectButton.prototype.unpublishedState = function() {
		this.published = false;
		this.setWorkURL("services/rest/edit/publish/{idPath}");
		this.options.workLabel = "Publishing...";
		this.options.followupLabel = "Publishing....";
	};

	PublishObjectButton.prototype.publishWorkDone = function(data) {
		var jsonData;
		if ($.type(data) === "string") {
			try {
				jsonData = $.parseJSON(data);
			} catch (e) {
				throw "Failed to change publication status for " + (this.options.metadata? this.options.metadata.title : this.pid);
			}
		} else {
			jsonData = data;
		}
		
		
		this.completeTimestamp = jsonData.timestamp;
		return true;
	};
	
	return PublishObjectButton;
});define('RemoteStateChangeMonitor', ['jquery'], function($) {
	function RemoteStateChangeMonitor(options) {
		this.init(options);
	};
	
	$.extend(RemoteStateChangeMonitor.prototype, {
		defaultOptions : {
			'pingFrequency' : 1000,
			'statusChanged' : undefined,
			'statusChangedTarget' : undefined,
			'checkStatus' : undefined,
			'checkStatusTarget' : undefined,
			'checkErrorTarget' : undefined,
			'checkStatusAjax' : {
			}
		},
		pingId : null,
		pingData : null,
		
		init: function(options) {
			this.options = $.extend({}, this.defaultOptions, options);
			this.options.checkStatusAjax.success = $.proxy(this.pingSuccessCheck, this);
			this.options.checkStatusAjax.error = $.proxy(this.pingError, this);
		},
		
		performPing : function() {
			if (this.pingData)
				this.options.checkStatusAjax.data = this.pingData;
			$.ajax(this.options.checkStatusAjax);
		},
		
		pingSuccessCheck : function(data) {
			var isDone = this.options.checkStatus.call(this.options.checkStatusTarget, data);
			if (isDone) {
				if (this.pingId != null) {
					clearInterval(this.pingId);
					this.pingId = null;
				}
				this.options.statusChanged.call(this.options.statusChangedTarget, data);
			} else if (this.pingId == null) {
				this.pingId = setInterval($.proxy(this.performPing, this), this.options.pingFrequency);
			}
		},
		
		pingError : function() {
			this.options.checkError.apply(this.options.checkErrorTarget, arguments);
			if (this.pingId != null) {
				clearInterval(this.pingId);
				this.pingId = null;
			}
		}
	});
	
	return RemoteStateChangeMonitor;
});/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
define('ResultObject', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/resultEntry',
		'ModalLoadingOverlay', 'DeleteObjectButton',	'PublishObjectButton', 'EditAccessControlForm'], 
		function($, ui, _, RemoteStateChangeMonitor, resultEntryTemplate, ModalLoadingOverlay) {
	var defaultOptions = {
			animateSpeed : 100,
			metadata : null,
			selected : false,
			selectable : true,
			selectCheckboxInitialState : false
		};
	
	function ResultObject(options) {
		this.options = $.extend({}, defaultOptions, options);
		this.selected = false;
		this.init(this.options.metadata);
	};
	
	ResultObject.prototype.init = function(metadata) {
		this.metadata = metadata;
		this.pid = metadata.id;
		this.actionMenuInitialized = false;
		this.isContainer = this.metadata.type != "File";
		var newElement = $(resultEntryTemplate({metadata : metadata, isContainer : this.isContainer}));
		this.checkbox = null;
		if (this.element) {
			if (this.actionMenu)
				this.actionMenu.remove();
			this.element.replaceWith(newElement);
		}
		this.element = newElement;
		this.element.data('resultObject', this);
		this.links = [];
		if (this.options.selected || this.selected)
			this.select();
	};
	
	ResultObject.prototype._destroy = function () {
		if (this.overlay) {
			this.overlay.close();
		}
	};

	ResultObject.prototype.initializePublishLinks = function(baseElement) {
		var links = baseElement.find(".publish_link");
		if (!links)
			return;
		this.links['publish'] = links;
		var obj = this;
		$(links).publishObjectButton({
			pid : obj.pid,
			parentObject : obj,
			defaultPublish : $.inArray("Unpublished", this.metadata.status) == -1
		});
	};

	ResultObject.prototype.initializeDeleteLinks = function(baseElement) {
		var links = baseElement.find(".delete_link");
		if (!links)
			return;
		this.links['delete'] = links;
		var obj = this;
		$(links).deleteObjectButton({
			pid : obj.pid,
			parentObject : obj
		});
	};

	ResultObject.prototype.disable = function() {
		this.options.disabled = true;
		this.element.css("cursor", "default");
		this.element.find(".ajaxCallbackButton").each(function(){
			$(this)[$(this).data("callbackButtonClass")].call($(this), "disable");
		});
	};

	ResultObject.prototype.enable = function() {
		this.options.disabled = false;
		this.element.css("cursor", "pointer");
		this.element.find(".ajaxCallbackButton").each(function(){
			$(this)[$(this).data("callbackButtonClass")].call($(this), "enable");
		});
	};
	
	ResultObject.prototype.isEnabled = function() {
		return !this.options.disabled;
	};

	ResultObject.prototype.toggleSelect = function() {
		if (this.selected) {
			this.unselect();
		} else {
			this.select();
		}
	};
	
	ResultObject.prototype.getElement = function () {
		return this.element;
	};
	
	ResultObject.prototype.getPid = function () {
		return this.pid;
	};
	
	ResultObject.prototype.getMetadata = function () {
		return this.metadata;
	};

	ResultObject.prototype.select = function() {
		if (!this.options.selectable)
			return;
		this.selected = true;
		this.element.addClass("selected");
		if (!this.checkbox)
			this.checkbox = this.element.find("input[type='checkbox']");
		this.checkbox.prop("checked", true);
	};

	ResultObject.prototype.unselect = function() {
		if (!this.options.selectable)
			return;
		this.selected = false;
		this.element.removeClass("selected");
		if (!this.checkbox)
			this.checkbox = this.element.find("input[type='checkbox']");
		this.checkbox.prop("checked", false);
	};
	
	ResultObject.prototype.highlight = function() {
		this.element.addClass("highlighted");
	};
	
	ResultObject.prototype.unhighlight = function() {
		this.element.removeClass("highlighted");
	};

	ResultObject.prototype.isSelected = function() {
		return this.element.hasClass("selected");
	};

	ResultObject.prototype.setState = function(state) {
		if ("idle" == state || "failed" == state) {
			this.enable();
			this.element.removeClass("followup working").addClass("idle");
			this.updateOverlay('close');
		} else if ("working" == state) {
			this.updateOverlay('open');
			this.disable();
			this.element.switchClass("idle followup", "working", this.options.animateSpeed);
		} else if ("followup" == state) {
			this.element.removeClass("idle").addClass("followup", this.options.animateSpeed);
		}
	};

	ResultObject.prototype.getActionLinks = function(linkNames) {
		return this.links[linkNames];
	};
	
	ResultObject.prototype.isPublished = function() {
		if (!$.isArray(this.metadata.status)){
			return true;
		}
		return $.inArray("Unpublished", this.metadata.status) == -1;
	};
	
	ResultObject.prototype.publish = function() {
		var links = this.links['publish'];
		if (links.length == 0)
			return;
		$(links[0]).publishObjectButton('activate');
	};
	
	ResultObject.prototype['delete'] = function() {
		var links = this.links['delete'];
		if (links.length == 0)
			return;
		$(links[0]).deleteObjectButton('activate');
	};

	ResultObject.prototype.deleteElement = function() {
		var obj = this;
		if (this.overlay)
			this.overlay.remove();
		obj.element.hide(obj.options.animateSpeed, function() {
			obj.element.remove();
			if (obj.options.resultObjectList) {
				obj.options.resultObjectList.removeResultObject(obj.pid);
			}
		});
	};
	
	ResultObject.prototype.updateVersion = function(newVersion) {
		if (newVersion != this.metadata._version_) {
			this.metadata._version_ = newVersion;
			return true;
		}
		return false;
	};
	
	ResultObject.prototype.setStatusText = function(text) {
		this.updateOverlay('setText', [text]);
	};
	
	ResultObject.prototype.updateOverlay = function(fnName, fnArgs) {
		// Check to see if overlay is initialized
		if (!this.overlay) {
			this.overlay = new ModalLoadingOverlay(this.element, {
				text : 'Working...',
				type : 'text',
				iconSize : 'small',
				autoOpen : false
			});
		}
		this.overlay[fnName].apply(this.overlay, fnArgs);
	};
	
	ResultObject.prototype.refresh = function(immediately) {
		this.updateOverlay('open');
		this.setStatusText('Refreshing...');
		if (immediately) {
			this.refreshData(true);
			return;
		}
		var self = this;
		var followupMonitor = new RemoteStateChangeMonitor({
			'checkStatus' : function(data) {
				return (data != self.metadata._version_);
			},
			'checkStatusTarget' : this,
			'statusChanged' : function(data) {
				self.refreshData(true);
			},
			'statusChangedTarget' : this, 
			'checkStatusAjax' : {
				url : "services/rest/item/" + self.pid + "/solrRecord/version",
				dataType : 'json'
			}
		});
		
		followupMonitor.performPing();
	};
	
	ResultObject.prototype.refreshData = function(clearOverlay) {
		var self = this;
		$.ajax({
			url : self.options.resultObjectList.options.refreshEntryUrl + self.pid,
			dataType : 'json',
			success : function(data, textStatus, jqXHR) {
				self.init(data);
				if (self.overlay)
					self.overlay.element = self.element;
				if (clearOverlay)
					self.updateOverlay("close");
			},
			error : function(a, b, c) {
				if (clearOverlay)
					self.updateOverlay("close");
				console.log(c);
			}
		});
	};
	
	return ResultObject;
});define('ResultObjectActionMenu', [ 'jquery', 'jquery-ui', 'DeleteObjectButton', 'PublishObjectButton', 'contextMenu'],
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
								document.location.href = "describe/" + metadata.id;
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
});define('ResultObjectList', ['jquery', 'MetadataObject', 'ResultObject' ], function($, MetadataObject, ResultObject) {
	function ResultObjectList(options) {
		this.init(options);
	};
	
	$.extend(ResultObjectList.prototype, {
		defaultOptions: {
			resultIdPrefix : "entry_",
			metadataObjects : undefined,
			refreshEntryUrl : "entry/",
			parent : null,
			splitLoadLimit : 70
		},
		resultObjects: {},
		
		init: function(options) {
			this.options = $.extend({}, this.defaultOptions, options);
			var self = this;
			//console.time("Initialize entries");
			//console.profile();
			var metadataObjects = self.options.metadataObjects;
			for (var i = 0; i < metadataObjects.length && i < self.options.splitLoadLimit; i++) {
				var metadata = metadataObjects[i];
				self.resultObjects[metadata.id] = new ResultObject({metadata : metadata, resultObjectList : self});
				if (self.options.parent)
					self.options.parent.append(self.resultObjects[metadata.id].element);
			}
			if (metadataObjects.length > self.options.splitLoadLimit) {
				setTimeout(function(){
					//console.time("Second batch");
					for (var i = self.options.splitLoadLimit; i < metadataObjects.length; i++) {
						var metadata = metadataObjects[i];
						self.resultObjects[metadata.id] = new ResultObject({metadata : metadata, resultObjectList : self});
						if (self.options.parent)
							self.options.parent.append(self.resultObjects[metadata.id].element);
					}
					//console.timeEnd("Second batch");
				}, 100);
			}
			//console.timeEnd("Initialize entries");
		},
		
		getResultObject: function(id) {
			return this.resultObjects[id];
		},
		
		removeResultObject: function(id) {
			if (id in this.resultObjects) {
				delete this.resultObjects[id];
			}
		},
		
		refreshObject: function(id) {
			var resultObject = this.getResultObject(id);
			$.ajax({
				url : this.options.refreshEntryUrl + resultObject.getPid(),
				dataType : 'json',
				success : function(data, textStatus, jqXHR) {
					resultObject.init(data);
				},
				error : function(A, B, C) {
					console.log(B);
				}
			});
		},
		
		getSelected: function() {
			var selected = [];
			for (var index in this.resultObjects) {
				if (this.resultObjects[index].selected)
					selected.push(this.resultObjects[index]);
			}
			return selected;
		}
	});
	
	return ResultObjectList;
});define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'URLUtilities', 'ParentResultObject', 'AddMenu', 
		'ResultObjectActionMenu', 'PublishBatchButton', 'UnpublishBatchButton', 'DeleteBatchButton', 'ConfirmationDialog', 'detachplus'], 
		function($, ui, ResultObjectList, URLUtilities, ParentResultObject, AddMenu, ResultObjectActionMenu,
				PublishBatchButton, UnpublishBatchButton, DeleteBatchButton, ConfirmationDialog) {
	$.widget("cdr.resultTableView", {
		options : {
			enableSort : true,
			ajaxSort : false,
			metadataObjects : undefined,
			enableArrange : false,
			enableMove : false,
			pagingActive : false,
			container : undefined
		},
		
		_create : function() {
			this.$resultTable = this.element.find('.result_table').eq(0);
			var fragment = $(document.createDocumentFragment());
			this.resultObjectList = new ResultObjectList({
				'metadataObjects' : this.options.metadataObjects, 
				parent : this.$resultTable.children('tbody')
			});
			if (this.options.container) {
				this.containerObject = new ParentResultObject({metadata : this.options.container, 
						resultObjectList : this.resultObjectList, element : $(".container_entry")});
				this._initializeAddMenu();
			}
			
			if (this.options.enableSort)
				this._initSort();
			this._initBatchOperations();
			this._initEventHandlers();
			this.actionMenu = new ResultObjectActionMenu({
				selector : ".action_gear",
				containerSelector : ".res_entry,.container_entry"
			});
			this._initMoveLocations();
			this._initReordering();
		},
		
		// Initialize sorting headers according to whether or not paging is active
		_initSort : function() {
			var $resultTable = this.$resultTable;
			var self = this;
			if (this.options.pagingActive) {
				// Paging active, so need to make server callback to perform sort
				var sortParam = URLUtilities.getParameter('sort');
				var sortOrder = URLUtilities.getParameter('sortOrder');
				$("th.sort_col", $resultTable).each(function(){
					var $this = $(this);
					$this.addClass('sorting');
					var sortField = $this.attr('data-field');
					if (sortField) {
						var order = '';
						if (sortParam == sortField) {
							if (sortOrder) {
								$this.addClass('asc');
							} else {
								$this.addClass('desc');
								order = 'reverse';
							}
						}
						var sortUrl = URLUtilities.setParameter(self.options.resultUrl, 'sort', sortField);
						sortUrl = URLUtilities.setParameter(sortUrl, 'sortOrder', order);
						this.children[0].href = sortUrl;
					}
				});
			} else {
				// Paging off, perform sorting locally
				$("th.sort_col", $resultTable).each(function(){
					var $th = $(this),
					thIndex = $th.index(),
					dataType = $th.attr("data-type");
					$th.addClass('sorting');
					
					$th.click(function(){
						if (!$th.hasClass('sorting')) return;
						//console.time("Sort total");
						var inverse = $th.hasClass('desc');
						$('.sorting', $resultTable).removeClass('asc desc');
						if (inverse)
							$th.addClass('asc');
						else 
							$th.addClass('desc');
						
						// Apply sort function based on data-type
						if (dataType == 'index') {
							self._originalOrderSort(inverse);
						} else if (dataType == 'title') {
							self._titleSort(inverse);
						} else {
							self._alphabeticSort(thIndex, inverse);
						}
						inverse = !inverse;
						//console.timeEnd("Sort total");
					});
				});
			}
		},
		
		// Base row sorting function
		_sortEntries : function($entries, matchMap, getSortable) {
			//console.time("Reordering elements");
			var $resultTable = this.$resultTable;
			
			$resultTable.detach(function(){
				var fragment = document.createDocumentFragment();
				if (matchMap) {
					if ($.isFunction(getSortable)) {
						for (var i = 0, length = matchMap.length; i < length; i++) {
							fragment.appendChild(getSortable.call($entries[matchMap[i].index]));
						}
					} else {
						for (var i = 0, length = matchMap.length; i < length; i++) {
							fragment.appendChild($entries[matchMap[i].index].parentNode);
						}
					}
				} else {
					if ($.isFunction(getSortable)) {
						for (var i = 0, length = $entries.length; i < length; i++) {
							fragment.appendChild(getSortable.call($entries[i]));
						}
					} else {
						for (var i = 0, length = $entries.length; i < length; i++) {
							fragment.appendChild($entries[i].parentNode);
						}
					}
				}
				var resultTable = $resultTable[0];
				resultTable.appendChild(fragment);
			});
			
			//console.timeEnd("Reordering elements");
		},
		
		// Simple alphanumeric result entry sorting
		_alphabeticSort : function(thIndex, inverse) {
			var $resultTable = this.$resultTable;
			var matchMap = [];
			//console.time("Finding elements");
			var $entries = $resultTable.find('tr.res_entry').map(function() {
				return this.children[thIndex];
			});
			//console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				matchMap.push({
					index : i,
					value : $entries[i].children[0].innerHTML.toUpperCase()
				});
			}
			//console.time("Sorting");
			matchMap.sort(function(a, b){
				if(a.value == b.value)
					return 0;
				return a.value > b.value ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			//console.timeEnd("Sorting");
			this._sortEntries($entries, matchMap);
		},
		
		// Sort by the order the items appeared at page load
		_originalOrderSort : function(inverse) {
			//console.time("Finding elements");
			var $entries = [];
			for (var index in this.resultObjectList.resultObjects) {
				var resultObject = this.resultObjectList.resultObjects[index];
				$entries.push(resultObject.getElement()[0]);
			}
			if (inverse)
				$entries = $entries.reverse();
			
			//console.timeEnd("Finding elements");

			this._sortEntries($entries, null, function(){
				return this;
			});
		},
		
		// Sort with a combination of alphabetic and number detection
		_titleSort : function(inverse) {
			var $resultTable = this.$resultTable;
			var titleRegex = new RegExp('(\\d+|[^\\d]+)', 'g');
			var matchMap = [];
			//console.time("Finding elements");
			var $entries = $resultTable.find('.res_entry > .itemdetails');
			//console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				var text = $entries[i].children[0].children[0].innerHTML.toUpperCase();
				var textParts = text.match(titleRegex);
				matchMap.push({
					index : i,
					text : text,
					value : (textParts == null) ? [] : textParts
				});
			}
			//console.time("Sorting");
			matchMap.sort(function(a, b) {
				if (a.text == b.text)
					return 0;
				var i = 0;
				for (; i < a.value.length && i < b.value.length && a.value[i] == b.value[i]; i++);
				
				// Whoever ran out of entries first, loses
				if (i == a.value.length)
					if (i == b.value.length)
						return 0;
					else return inverse ? 1 : -1;
				if (i == b.value.length)
					return inverse ? -1 : 1;
				
				// Do int comparison of unmatched elements
				var aInt = parseInt(a.value[i]);
				if (!isNaN(aInt)) {
						var bInt = parseInt(b.value[i]);
						if (!isNaN(bInt))
							return aInt > bInt ?
									inverse ? -1 : 1
									: inverse ? 1 : -1;
				}
				return a.text > b.text ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			//console.timeEnd("Sorting");
			this._sortEntries($entries, matchMap);
		},
		
		_initBatchOperations : function() {
			var self = this;
			
			$(".select_all").click(function(){
				var checkbox = $(this).children("input");
				var toggleFn = checkbox.prop("checked") ? "select" : "unselect";
				var resultObjects = self.resultObjectList.resultObjects;
				for (var index in resultObjects) {
					resultObjects[index][toggleFn]();
				}
			}).children("input").prop("checked", false);
			
			var publishButton = $(".publish_selected", self.element);
			var publishBatch = new PublishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.setStatusText('Publishing...');
						this.updateOverlay('open');
					}, 
				'followupFunction' : function() {
					this.setStatusText('Publishing....');
				}, 
				'completeFunction' : function(){
					this.refresh(true);
				}
			}, publishButton);
			publishButton.click(function(){
				publishBatch.activate();
			});
			var unpublishButton = $(".unpublish_selected", self.element);
			var unpublishBatch = new UnpublishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.setStatusText('Unpublishing...');
						this.updateOverlay('open');
					}, 
				'followupFunction' : function() {
					this.setStatusText('Unpublishing....');
				}, 
				'completeFunction' : function(){
					this.refresh(true);
				}
			}, unpublishButton);
			unpublishButton.click(function(){
				unpublishBatch.activate();
			});
			var deleteButton = $(".delete_selected", self.element);
			var deleteBatch = new DeleteBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						this.setStatusText('Deleting...');
						this.updateOverlay('open');
					}, 
				'followupFunction' : function() {
						this.setStatusText('Cleaning up...');
					}, 
				'completeFunction' : 'deleteElement',
				confirmAnchor : deleteButton
			}, deleteButton);
			deleteButton.click(function(){
				deleteBatch.activate();
			});
		},
		
		_initEventHandlers : function() {
			var self = this;
			$(document).on('click', ".res_entry", function(e){
				$(this).data('resultObject').toggleSelect();
			});
			this.$resultTable.on('click', ".res_entry a", function(e){
				e.stopPropagation();
			});
		},
		
		//Initializes the droppable elements used in move operations
		_initMoveLocations : function() {
			// Jquery result containing all elements to use as move drop zones
			this.$dropLocations = $();
			this.addMoveDropLocation(this.$resultTable, ".res_entry.container.move_into .title", function($dropTarget){
				var dropObject = $dropTarget.closest(".res_entry").data("resultObject");
				// Needs to be a valid container with sufficient perms
				if (!dropObject || !dropObject.isContainer || $.inArray("addRemoveContents", dropObject.metadata.permissions) == -1) return false;
				return dropObject.metadata;
			});
		},
		
		addMoveDropLocation : function($dropLocation, dropTargetSelector, dropTargetGetDataFunction) {
			var self = this;
			this.$dropLocations = this.$dropLocations.add($dropLocation);
			$dropLocation.on("mouseenter", dropTargetSelector, function() {
				//console.log("Hovering", this);
				$(this).addClass("drop_hover");
			}).on("mouseleave", dropTargetSelector, function() {
				//console.log("Blur", this);
				$(this).removeClass("drop_hover");
			});
			$dropLocation.droppable({
				drop : function(event, ui) {
					// Locate which element is being dropped on
					var $dropTarget = $(document.elementFromPoint(event.pageX - $(window).scrollLeft(), event.pageY - $(window).scrollTop()));
					// Verify that it is the correct type of element and retrieve metadata
					var metadata = dropTargetGetDataFunction($dropTarget);
					if (!metadata) return false;
					// Activate move drop mode
					self.dropActive = true;
					
					// Confirm the move operation before performing it
					var representative = ui.draggable.data("resultObject");
					var repTitle = representative.metadata.title;
					if (repTitle.length > 50) repTitle = repTitle.substring(0, 50) + "...";
					var destTitle = metadata.title;
					if (destTitle.length > 50) destTitle = destTitle.substring(0, 50) + "...";
					var promptText = "Move \"<a class='result_object_link' data-id='" + representative.pid + "'>" + repTitle + "</a>\"";
					if (self.dragTargets.length > 1)
						promptText += " and " + (self.dragTargets.length - 1) + " other object" + (self.dragTargets.length - 1 > 1? "s" :"");
					promptText += " into \"<a class='result_object_link' data-id='" + metadata.id + "'>" + destTitle + "</a>\"?";
					var confirm = new ConfirmationDialog({
						promptText : promptText,
						modal : true,
						autoOpen : true,
						addClass : "move",
						dialogOptions : {
							width : 'auto',
							maxWidth : 400,
							position : "center center"
						},
						confirmFunction : function() {
							// Perform the move operation and clean up the result entries
							if (self.dragTargets) {
								var moveData = {
										newParent : metadata.id,
										ids : []
									};
								$.each(self.dragTargets, function() {
									moveData.ids.push(this.pid);
									this.element.hide();
								});
								// Store a reference to the targeted item list since moving happens asynchronously
								var moveObjects = self.dragTargets;
								$.ajax({
									url : "/services/rest/edit/move",
									type : "POST",
									data : JSON.stringify(moveData),
									contentType: "application/json; charset=utf-8",
									dataType: "json",
									success : function(data) {
										$.each(moveObjects, function() {
											this.deleteElement();
										});
										self.options.alertHandler.alertHandler("success", "Moved " + moveObjects.length + " object" + (moveObjects.length > 1? "s" : "") 
												+ " to " + destTitle);
									},
									error : function() {
										$.each(moveObjects, function() {
											this.element.show();
										});
										self.options.alertHandler.alertHandler("error", "Failed to move " + moveObjects.length + " object" + (moveObjects.length > 1? "s" : "") 
												+ " to " + destTitle);
										
									}
								});
							}
							self.dragTargets = null;
							self.$dropLocations.removeClass("moving");
							self.dropActive = false;
						},
						cancelFunction : function() {
							// Cancel and revert the page state
							if (self.dragTargets) {
								$.each(self.dragTargets, function() {
									this.element.show();
								});
								self.dragTargets = null;
							}
							self.$dropLocations.removeClass("moving");
							self.dropActive = false;
						}
					});
				},
				tolerance: 'pointer',
				over: function(event, ui) {
					console.log("Over " + this);
					$(".ui-sortable-placeholder").hide();
				},
				out: function(event, ui) {
					$(".ui-sortable-placeholder").show();
				}
			});
		},
		
		// Initializes draggable elements used in move and reorder operations
		_initReordering : function() {
			var self = this;
			var arrangeMode = false;
			var $resultTable = this.$resultTable;
			
			function setSelected(element) {
				var resultObject = element.closest(".res_entry").data("resultObject");
				if (resultObject.selected) {
					var selecteResults = self.resultObjectList.getSelected();
					self.dragTargets = selecteResults;
				} else {
					self.dragTargets = [resultObject];
				}
			}
			
			$resultTable.sortable({
				delay : 200,
				items: '.res_entry',
				cursorAt : { top: -2, left: -5 },
				forceHelperSize : false,
				scrollSpeed: 100,
				connectWith: '.result_table, .structure_content',
				placeholder : 'arrange_placeholder',
				helper: function(e, element){
					if (!self.dragTargets)
						setSelected(element);
					var representative = element.closest(".res_entry").data("resultObject");
					var metadata = representative.metadata;
					// Indicate how many extra items are being moved
					var additionalItemsText = "";
					if (self.dragTargets.length > 1)
						additionalItemsText = " (and " + (self.dragTargets.length - 1) + " others)";
					// Return helper for representative entry
					var helper = $("<div class='move_helper'><span><img src='/static/images/admin/type_" + metadata.type.toLowerCase() + ".png'/>" + metadata.title + "</span>" + additionalItemsText + "</div>");
					//helper.width(300);
					return helper;
				},
				appendTo: document.body,
				start: function(e, ui) {
					// Hide the original items for a reorder operation
					if (self.dragTargets && false) {
						$.each(self.dragTargets, function() {
							this.element.hide();
						});
					} else {
						ui.item.show();
					}
					// Set the table to move mode and enable drop zone hover highlighting
					self.$dropLocations.addClass("moving")
						.on("mouseenter", ".res_entry.container.move_into .title", function() {
							//console.log("Hovering");
							$(this).addClass("drop_hover");
						}).on("mouseleave", ".res_entry.container.move_into .title", function() {
							//console.log("Blur");
							$(this).removeClass("drop_hover");
						});
				},
				stop: function(e, ui) {
					// Move drop mode overrides reorder
					if (self.dropActive) {
						return false;
					}
					if (self.dragTargets) {
						$.each(self.dragTargets, function() {
							this.element.show();
						});
						self.dragTargets = null;
					}
					self.$dropLocations.removeClass("moving");
					return false;
					
					/*if (!moving && !arrangeMode)
						return false;
					var self = this;
					if (this.selected) {
						$.each(this.selected, function(index){
							if (index < self.itemSelectedIndex)
								ui.item.before(self.selected[index]);
							else if (index > self.itemSelectedIndex)
								$(self.selected[index - 1]).after(self.selected[index]);
						});
					}*/
				},
				update: function (e, ui) {
					/*if (!moving && !arrangeMode)
						return false;
					if (ui.item.hasClass('selected') && this.selected.length > 0)
						this.selected.hide().show(300);
					else ui.item.hide().show(300);*/
				}
			});
		},
		
		setEnableSort : function(value) {
			this.options.enableSort = value;
			if (value) {
				$("th.sort_col").removeClass("sorting");
			} else {
				$("th.sort_col").addClass("sorting");
			}
		},
		
		// Initialize the menu for adding new items
		_initializeAddMenu : function() {
			this.addMenu = new AddMenu({
				container : this.options.container,
				selector : "#add_menu",
				alertHandler : this.options.alertHandler
			});
		}
	});
});
	define('SearchMenu', [ 'jquery', 'jquery-ui', 'URLUtilities', 'StructureView'], function(
		$, ui, URLUtilities) {
	$.widget("cdr.searchMenu", {
		options : {
			filterParams : '',
			selectedId : false
		},
		
		_create : function() {
			var self = this;
			this.element.children('.query_menu').accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true
			});
			this.element.children('.filter_menu').accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true,
				active: false,
				beforeActivate: function(event, ui) {
					if (ui.newPanel.attr('data-href') != null && !ui.newPanel.data('contentLoaded')) {
						var isStructureBrowse = (ui.newPanel.attr('id') == "structure_facet");
						$.ajax({
							url : URLUtilities.uriEncodeParameters(ui.newPanel.attr('data-href')),
							dataType : isStructureBrowse? 'json' : null,
							success : function(data) {
								if (isStructureBrowse) {
									var $structureView = $('<div/>').html(data);
									$structureView.structureView({
										rootNode : data.root,
										showResourceIcons : true,
										showParentLink : true,
										queryPath : 'list',
										filterParams : self.options.filterParams,
										selectedId : self.options.selectedId
									});
									$structureView.addClass('inset facet');
									// Inform the result view that the structure browse is ready for move purposes
									if (self.options.resultTableView)
										self.options.resultTableView.resultTableView('addMoveDropLocation', 
											$structureView.find(".structure_content"),
											'.entry > .primary_action', 
											function($dropTarget){
												var dropObject = $dropTarget.closest(".entry_wrap").data("structureEntry");
												// Needs to be a valid container with sufficient perms
												if (!dropObject || dropObject.options.isSelected || $.inArray("addRemoveContents", dropObject.metadata.permissions) == -1)
													return false;
												return dropObject.metadata;
											});
									data = $structureView;
								}
								ui.newPanel.html(data);
								ui.newPanel.data('contentLoaded', true);
							}
						});
					}
				},
				activate : $.proxy(self._adjustHeight, self)
			}).accordion('option', 'active', 0);
			
			this.element.resizable({
				handles: 'e',
				alsoResize : ".structure.inset.facet",
				minWidth: 300,
				maxWidth: 600
			}).css('visibility', 'visible');
			
			$(window).resize($.proxy(self._adjustHeight, self));
		},
		
		_adjustHeight : function () {
			console.log("called");
			var activeMenu = this.element.find(".filter_menu .ui-accordion-content-active");
			if (activeMenu.length == 0) {
				return;
			}
			console.log("height");
			var top = activeMenu.offset().top;
			var innerHeight = activeMenu.innerHeight();
			var height = activeMenu.height();
			var verticalPadding = innerHeight - height;
			var windowHeight = $(window).height();
			var siblingHeight = 0;
			activeMenu.parent().nextAll().each(function(){
				siblingHeight += $(this).outerHeight() + 4;
			});
			if ((top + innerHeight + siblingHeight) > windowHeight) {
				activeMenu.height(windowHeight - top - siblingHeight - verticalPadding);
			} else {
				activeMenu.height('auto');
			}
		}
	});
});define('URLUtilities', ['jquery'], function($) {
	return {
		uriEncodeParameters : function(url) {
			var newParameterString = "", tempArray = url.split("?");
			if (tempArray.length < 2)
				return url;
			var baseURL = tempArray[0], parameterString = tempArray[1];
			if (parameterString) {
				tempArray = parameterString.split("&");
				for (var i=0; i<tempArray.length; i++){
					if (newParameterString.length > 0)
						newParameterString += '&';
					var paramPair = tempArray[i].split('=');
					newParameterString += paramPair[0] + "=" + encodeURIComponent(paramPair[1]);
				}
			}
			return baseURL + "?" + newParameterString;
		},
		
		getParameter : function (name) {
			return decodeURI(
					(RegExp(name + '=' + '([^&]*?)(&|$)').exec(location.search)||[,null])[1]
			);
		},
		
		setParameter : function(url, key, paramVal){
			var baseURL = this.removeParameter(url, key);
			if (baseURL.indexOf('?') == -1)
				baseURL += '?';
			else baseURL += '&';
			return baseURL + key + "=" + paramVal;
		},
		
		removeParameter : function (url, key) {
			var newParameterString = "", tempArray = url.split("?");
			var baseURL = tempArray[0], parameterString = tempArray[1];
			if (parameterString) {
				tempArray = parameterString.split("&");
				for (var i=0; i<tempArray.length; i++){
					if(tempArray[i].split('=')[0] != key){
						if (newParameterString.length > 0)
							newParameterString += '&';
						newParameterString += tempArray[i];
					}
				}
			}
			if (newParameterString)
				return baseURL + "?" + newParameterString;
			return baseURL;
		}
	};
});define('UnpublishBatchButton', [ 'jquery', 'BatchCallbackButton' ], function($, BatchCallbackButton) {
	function UnpublishBatchButton(options, element) {
		this._create(options, element);
	};
	
	UnpublishBatchButton.prototype.constructor = UnpublishBatchButton;
	UnpublishBatchButton.prototype = Object.create( BatchCallbackButton.prototype );
	
	var defaultOptions = {
			resultObjectList : undefined,
			workPath: "services/rest/edit/unpublish",
			childWorkLinkName : 'publish'
		};
	
	UnpublishBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		BatchCallbackButton.prototype._create.call(this, merged, element);
	};

	UnpublishBatchButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (resultObject.isSelected() && $.inArray("Unpublished", resultObject.getMetadata().status) == -1
					&& resultObject.isEnabled()) {
				targetIds.push(resultObject.getPid());
			}
		}
		return targetIds;
	};
	return UnpublishBatchButton;
});define('AbstractStatusMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'tpl!../templates/admin/statusMonitor/overview', 'tpl!../templates/admin/statusMonitor/details', 'moment'], 
		function($, ui, _, overviewTemplate, detailsTemplate) {

	var defaultOptions = {
		name : undefined,
		jobConfig : {
			url : undefined,
			detailsUrl : undefined,
		},
		overviewConfig : {
			url : undefined,
			refresh : 10000,
			render : undefined,
			template : overviewTemplate
		}
	};

	function AbstractStatusMonitor() {
	};
	
	AbstractStatusMonitor.prototype.init = function() {
		this.monitorId = "status_monitor_" + this.options.name.replace(" ", "_");
		this.element = $("<div></div>").attr("id", this.monitorId);
		this.jobConfig = $.extend({}, this.options.jobConfig);
		this.overviewConfig = $.extend({}, this.options.overviewConfig);
		this.initializeOverview();
		this.createJobTable();
		for (var index in this.options.jobConfig.jobTypes) {
			var jobType = this.options.jobConfig.jobTypes[index];
			this.initializeJobType(jobType);
		}
		this.createDetailsView();
		return this;
	};
	
	AbstractStatusMonitor.prototype.createJobTable = function() {
		var jobContainer = $("<div/>").addClass("status_monitor_job_container").appendTo(this.element);
		this.jobTable = $("<table id='" + this.options.name + "_monitor_jobs' class='status_monitor_jobs'></table>").appendTo(jobContainer);
		var $colgroup = $("<colgroup/>").appendTo(this.jobTable);
		var $headerRow = $("<tr class='column_headers'/>").appendTo(this.jobTable);
		for (var index in this.jobConfig.fields) {
			$colgroup.append("<col class='col_" + this.jobConfig.fields[index].replace(" ", "_") + "'></col>")
			$headerRow.append("<th>" + this.jobConfig.fields[index] + "</th>");
		}
		// Box shadow for first row to support webkit
		$("<div/>").addClass("column_headers_shadow").appendTo(jobContainer);
	};
	
	AbstractStatusMonitor.prototype.createDetailsView = function() {
		this.detailsWrapper = $(detailsTemplate(this.options)).appendTo(this.element);
		this.detailsView = this.detailsWrapper.find(".status_details");
		this.detailsContent = this.detailsView.find(".status_details_content");
		this.detailsView.find(".hide_status_details").click($.proxy(this.deactivateDetails, this));
		$(window).scroll($.proxy(this.positionDetailsView, this));
	};
	
	AbstractStatusMonitor.prototype.positionDetailsView = function() {
		if (!this.detailsView)
			return;
		var self = this, $window = $(window);
		// Prevent details from scrolling off top of the page
		var detailsTop = this.detailsWrapper.offset().top;
		if ($window.scrollTop() >= detailsTop) {
			self.detailsView.css({
				position : 'fixed',
				top : 0
			});
			
		} else {
			self.detailsView.css({
				position : 'absolute',
				top : 0
			});
		}
		// Adjust details height to make sure it will if on the screen
		var heightPadding = self.detailsContent.position().top + self.detailsContent.innerHeight() - self.detailsContent.height() + 5;
		if (self.detailsView.height() > $window.height()) {
			self.detailsContent.height($window.height() - heightPadding);
		} else {
			if ($window.height() - heightPadding > self.detailsContent.height())
				self.detailsContent.height("auto");
		}
	};
	
	AbstractStatusMonitor.prototype.deactivateDetails = function() {
		if (this.detailsType)
			clearTimeout(this.detailsType.repeatId);
		this.element.removeClass("show_details");
	};
	
	// Deactivate this status monitor, preventing it from refreshing any further
	AbstractStatusMonitor.prototype.deactivate = function() {
		this.active = false;
		// Cancel all the refresh timeouts
		for (var index in this.options.jobConfig) {
			var jobType = this.jobConfig[index];
			clearTimeout(jobType.repeatId);
		}
		if (this.detailsType)
			clearTimeout(this.detailsType.repeatId);
		clearTimeout(this.overviewConfig.repeatId);
		return this;
	};
	
	// Activate this status monitor, and begin refreshing status
	AbstractStatusMonitor.prototype.activate = function() {
		this.active = true;
		// Start refreshing of overview panel
		this.refreshType(this.overviewConfig, true);
		// Start refreshing of job types
		for (var index in this.jobConfig.jobTypes) {
			var jobType = this.jobConfig.jobTypes[index];
			this.refreshType(jobType, true);
		}
		// Restart details refreshing if a job is selected
		if (this.detailsType)
			this.refreshType(this.detailsType, true);
		return this;
	};
	
	// Setup the overview panel for this monitor
	AbstractStatusMonitor.prototype.initializeOverview = function() {
		this.overviewPanel = $("<div></div>").appendTo(this.element);
		if (!this.overviewConfig.render)
			this.overviewConfig.render = this.renderOverview;
	};
	
	// Render the overview panel
	AbstractStatusMonitor.prototype.renderOverview = function(typeConfig) {
		this.overviewPanel.html(typeConfig.template({data : typeConfig.results, type : typeConfig, dateFormat : this.dateFormat}));
	};
	
	// Instantiate the configuration and placement for the provided job type
	AbstractStatusMonitor.prototype.initializeJobType = function(jobBase) {
		var jobType = $.extend({}, this.options.jobConfig, jobBase);
		jobType.url = jobType.url.replace("{name}", jobType.name);
		jobType.detailsUrl = jobType.detailsUrl.replace("{name}", jobType.name);
		if (!('render' in jobType))
			jobType.render = this.renderJobType;
		
			// Instantiate the placeholder where these types of jobs will be located and store reference in config
		var placeholder = $("<tr id='" + this.options.name + "_monitor_ph_" + jobType.name + "'></tr>");
		jobType.placeholder = placeholder;
		this.jobTable.append(placeholder);
		// Register click events for viewing details
		var self = this;
		this.jobTable.on("click", ".monitor_job." + jobType.name, function(){
			// Cleanup previous details callback if there is one
			if (self.detailsType)
				clearTimeout(self.detailsType.repeatId);
			var $this = $(this); 
			$this.siblings(".selected").removeClass("selected");
			$this.addClass("selected");
			var jobId = this.getAttribute("data-id");
			var detailsType = $.extend({}, jobType);
			// Store the currently active details definition
			self.detailsType = detailsType;
			detailsType.url = jobType.detailsUrl.replace("{id}", jobId);
			detailsType.id = jobId;
			detailsType.template = jobType.detailsTemplate;
			detailsType.render = self.renderJobDetails;
			if (detailsType.detailsRefresh)
				detailsType.refresh = detailsType.detailsRefresh;
			self.refreshType(detailsType, true);
			self.element.addClass("show_details");
			self.positionDetailsView();
		});
		// Add job type to config
		this.jobConfig.jobTypes.push(jobType);
	};
	
	// Default job type rendering, calls the job type's template for each job result
	AbstractStatusMonitor.prototype.renderJobType = function(typeConfig) {
		$(".monitor_job." + typeConfig.name).remove();
		for (var index in typeConfig.results.jobs) {
			var selected = this.detailsType && typeConfig.results.jobs[index].id == this.detailsType.id;
			typeConfig.placeholder.after(typeConfig.template({data : typeConfig.results.jobs[index], type : typeConfig, dateFormat : this.dateFormat, selected : selected}));
		}
	};
	
	AbstractStatusMonitor.prototype.renderJobDetails = function(typeConfig) {
		this.detailsContent.html(typeConfig.template({data : typeConfig.results, type : typeConfig, dateFormat : this.dateFormat}));
	};
	
	// Standard date format function to be use in templates
	AbstractStatusMonitor.prototype.dateFormat = function(dateObject) {
		return moment(dateObject).format("MM/DD/YYYY h:mma");
	};
	
	// Refresh results from a type configuration.  This applies to both overview types and job types.
	// If repeat is true, then the refresh will repeat until interrupted
	AbstractStatusMonitor.prototype.refreshType = function(typeConfig, repeat) {
		var self = this;
		$.getJSON(typeConfig.url, {}, function(json) {
			// Store results in this job type
			typeConfig.results = json;
			// Update display
			typeConfig.render.call(self, typeConfig);
			// If this a repeating refresh, start the next repeat
			if (repeat) {
				typeConfig.repeatId = setTimeout(function() {
					if (self.active)
						self.refreshType(typeConfig, repeat);
				}, typeConfig.refresh);
			}
		});
	};
	
	AbstractStatusMonitor.prototype.getDefaultOptions = function() {
		return defaultOptions;
	};
	
	return AbstractStatusMonitor;
});define('EnhancementMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/enhancementMonitorJob', 'tpl!../templates/admin/statusMonitor/enhancementMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, enhancementMonitorJobTemplate, enhancementMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "enhancement",
		jobConfig : {
			url : "services/rest/enhancement/{name}?begin=0&end=20",
			template : enhancementMonitorJobTemplate,
			detailsUrl : "/services/rest/enhancement/job/{id}?type={name}",
			detailsTemplate : enhancementMonitorDetailsTemplate,
			fields : ["Status", "Label", "Enhancements", "Triggered by"],
			jobTypes : [
				{name : "active", refresh : 10000},
				{name : "queued", refresh : 10000},
				{name : "blocked", refresh : 10000},
				{name : "finished", refresh : 10000},
				{name : "failed", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/rest/enhancement"
		}
	};
			
	function EnhancementMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	EnhancementMonitor.prototype.constructor = EnhancementMonitor;
	EnhancementMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return EnhancementMonitor;
});define('IndexingMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/indexingMonitorJob', 'tpl!../templates/admin/statusMonitor/indexingMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, indexingMonitorJobTemplate, indexingMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "indexing",
		jobConfig : {
			url : "/services/rest/indexing/jobs?begin=0&end=20",
			template : indexingMonitorJobTemplate,
			detailsUrl : "/services/rest/indexing/jobs/job/{id}",
			detailsTemplate : indexingMonitorDetailsTemplate,
			fields : ["Status", "Label", "Action", "Progress"],
			jobTypes : [
				{name : "all", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/rest/indexing"
		}
	};
			
	function IndexingMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	IndexingMonitor.prototype.constructor = IndexingMonitor;
	IndexingMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return IndexingMonitor;
});define('IngestMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/ingestMonitorJob', 'tpl!../templates/admin/statusMonitor/ingestMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, ingestMonitorJobTemplate, ingestMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "ingest",
		jobConfig : {
			url : "/services/rest/ingest/{name}/",
			template : ingestMonitorJobTemplate,
			detailsUrl : "/services/rest/ingest/job/{id}",
			detailsTemplate : ingestMonitorDetailsTemplate,
			fields : ["Status", "Submitter", "Submit time", "Ingested", "First object", "Note"],
			jobTypes : [
				{name : "active", refresh : 5000, detailsRefresh : 1000},
				{name : "queued", refresh : 10000},
				{name : "finished", refresh : 10000},
				{name : "failed", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/rest/ingest/"
		}
	};
			
	function IngestMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	IngestMonitor.prototype.constructor = IngestMonitor;
	IngestMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return IngestMonitor;
});define('StatusMonitorManager', [ 'jquery', 'jquery-ui', 'underscore', 'IngestMonitor', 'IndexingMonitor', 'EnhancementMonitor'],
		function($, ui, _, IngestMonitor, IndexingMonitor, EnhancementMonitor) {
			
	function StatusMonitorManager(element, options) {
		this.element = element;
		this.tabList = $("<ul/>").attr("id", "status_monitor_tabs").appendTo(this.element);
		this.monitors = [];
		this.addMonitors();
		var self = this;
		this.element.tabs({
			beforeActivate : function(event, ui) {
				// Deactivate the currently active monitor
				self.deactivate();
				// Activate the selected monitor
				var index = ui.newTab.index();
				self.activate(index);
			},
			activate : function() {
				self.monitors[self.activeMonitorIndex].positionDetailsView();
			}
		});
		this.activeMonitorIndex = 0;
	};
	
	StatusMonitorManager.prototype.deactivate = function(index) {
		index = arguments.length > 0? index : this.activeMonitorIndex;
		this.monitors[index].deactivate();
	};
	
	StatusMonitorManager.prototype.activate = function(index) {
		index = arguments.length > 0? index : this.activeMonitorIndex;
		this.activeMonitorIndex = index;
		this.monitors[index].activate();
	};
	
	StatusMonitorManager.prototype.addMonitors = function() {
		this.addMonitor(new IngestMonitor());
		this.addMonitor(new IndexingMonitor());
		this.addMonitor(new EnhancementMonitor());
	};
	
	StatusMonitorManager.prototype.addMonitor = function(monitor) {
		this.monitors.push(monitor);
		monitor.init();
		monitor.element.appendTo(this.element);
		this.tabList.append("<li><a href='" + document.URL + "#" + monitor.monitorId + "'>" + monitor.options.name + "</a></li>");
	};
	
	return StatusMonitorManager;
});