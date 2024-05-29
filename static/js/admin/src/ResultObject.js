define('ResultObject', [ 'jquery', 'jquery-ui', 'underscore', 'ModalLoadingOverlay', 'ResourceTypeUtilities'], 
		function($, ui, _, ModalLoadingOverlay, ResourceTypeUtilities) {
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
		var tags = this.metadata.contentStatus || [];
		var self = this;

		if (this.metadata.status !== undefined) {
			tags = tags.concat(this.metadata.status);
		}

		if (tags.length > 0) {
			this.metadata.tags = tags.filter(function(d) {
				return !/^(has|not|no.primary|public.access|members.are.unordered|no.assigned.thumbnail|view.behavior|access.surrogate)/i.test(d);
			}).map(function(d) {
				var tagValue;

				if (/deletion/i.test(d)) {
					tagValue = 'deleted';
				} else if (/staff-only/i.test(d)) {
					tagValue = 'staff-only';
				} else if(/inherited patron settings/i.test(d)) {
					tagValue = 'inherited-settings';
				} else {
					tagValue = d.trim().toLowerCase()
						.replace(/parent\s+is\s+/, '')
						.replace(/\s+/g, '-');
				}

				return self._tagText(tagValue);
			});
		}
		
		var newElement = $(this.options.template({metadata : metadata,
				isContainer : this.isContainer, 
				isDeleted : this.isDeleted,
				validationProblem : validationProblem,
				icon : ResourceTypeUtilities.getIconNameForType(this.metadata.type)
		}));
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

	ResultObject.prototype._tagText = function(tag) {
		var helpText;

		switch (tag) {
			case 'deleted':
				helpText = 'Object has been marked for deletion'
				break;
			case 'described':
				helpText = 'Object has a MODS description';
				break;
			case 'embargoed':
				helpText = 'Object has an active embargo set';
				break;
			case 'patron-settings':
				helpText = 'Patron access settings for this object have been added';
				break;
			case 'is-primary-object':
				helpText = 'This file is the representative object for the work which contains it';
				break;
			case 'staff-only':
				helpText = 'Only users with staff roles can access this object';
				break;
			case 'inherited-settings':
				helpText = 'Object is inheriting patron access settings which have been modified (typically they are more restrictive)';
				break;
			case 'assigned-as-thumbnail':
				helpText = 'This file is the assigned thumbnail for the work which contains it';
				break;
			default:
				helpText = '';
		}

		return { label: helpText, value: tag };
	}
	
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
						extension : fields[3],
						fileSize : fields[4],
						checksum : fields[5],
						defaultWebObject : fields.length > 6? fields[6] : null
					};
				}
			}
		}
		return false;
	};
	
	return ResultObject;
});