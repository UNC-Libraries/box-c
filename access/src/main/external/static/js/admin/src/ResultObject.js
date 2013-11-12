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
		this.isDeleted = $.inArray("Deleted", this.metadata.status) != -1;
		var newElement = $(resultEntryTemplate({metadata : metadata, isContainer : this.isContainer, isDeleted : this.isDeleted}));
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
				url : "/services/api/status/item/" + self.pid + "/solrRecord/version",
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
});