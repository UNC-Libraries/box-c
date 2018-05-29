define('ResultObject', [ 'jquery', 'jquery-ui', 'underscore', 'ModalLoadingOverlay'], 
		function($, ui, _, ModalLoadingOverlay) {
	var defaultOptions = {
			animateSpeed : 100,
			metadata : null,
			selected : false,
			selectable : true,
			selectCheckboxInitialState : false,
			template : undefined
		};
	
	function ResultObject(options) {
		this.options = $.extend({}, defaultOptions, options);
		this.selected = false;
		this.init(this.options.metadata);
	};
	
	ResultObject.prototype.init = function(metadata) {
		this.metadata = metadata;
		this.pid = metadata.id;
		this.isContainer = this.metadata.type != "File";
		this.isDeleted = $.inArray("Marked For Deletion", this.metadata.status) != -1;

		var validationProblem = "";
		if (this.metadata.tags) {
			var tagIndex = -1;
			for (var index in this.metadata.tags) {
				var tag = this.metadata.tags[index];
				if (tag.label == "invalid term") {
					var details =tag.details;
					for (var detailsIndex in details) {
						var detailParts = details[detailsIndex].split("|");
						validationProblem +=  "<br/>&nbsp;&middot;&nbsp;" + detailParts[0] + ": " + detailParts[1];
					}
					
					tagIndex = index;
					break;
				}
			}
			
			if (tagIndex != -1) {
				delete this.metadata.tags[tagIndex];
			}
		}
		
		var newElement = $(this.options.template({metadata : metadata, isContainer : this.isContainer, 
				isDeleted : this.isDeleted, validationProblem : validationProblem}));
		this.checkbox = null;
		if (this.element) {
			if (this.actionMenu)
				this.actionMenu.remove();
			this.element.replaceWith(newElement);
		}
		this.element = newElement;
		this.element.data('resultObject', this);
		if (this.options.selected || this.selected)
			this.select();
	};
	
	ResultObject.prototype._destroy = function () {
		if (this.overlay) {
			this.overlay.close();
		}
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
	
	ResultObject.prototype.isSelectable = function() {
		return this.options.selectable;
	};
	
	ResultObject.prototype.setSelectable = function(selectable) {
		if (selectable) {
			this.checkbox.removeAttr("disabled");
		} else {
			this.checkbox.attr("disabled", true);
		}
		this.options.selectable = selectable;
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
		} else if ("moving" == state) {
			this.unselect();
			this.setSelectable(false);
			this.element.addClass("working moving");
		}
	};
	
	ResultObject.prototype.isPublished = function() {
		if (!$.isArray(this.metadata.status)){
			return true;
		}
		return $.inArray("Unpublished", this.metadata.status) == -1;
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
	
	ResultObject.prototype.getDatastream = function(dsName) {
		if (this.metadata.datastream) {
			for (var dsIndex in this.metadata.datastream) {
				var ds = this.metadata.datastream[dsIndex];
				if (ds.indexOf(dsName + "|") == 0) {
					var fields = ds.split("|");
					return {
						name : fields[0],
						mimeType : fields[1],
						extension : fields[2],
						fileSize : fields[3],
						checksum : fields[4],
						defaultWebObject : fields.length > 5? fields[5] : null
					};
				}
			}
		}
		return false;
	};
	
	return ResultObject;
});