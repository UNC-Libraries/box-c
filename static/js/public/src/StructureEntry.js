define('StructureEntry', [ 'jquery', 'jquery-ui', 'tpl!../templates/structureEntry'], function($, ui, structureEntryTemplate) {
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
		this.isAContainer = this.metadata.type != "File";
		
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
		
		var childrenPresent = this.countChildrenPresent(this);
		
		this.hasContent = this.childEntries && this.childEntries.length > 0;
		this.contentLoaded = false;
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
		if (this.options.structureView.options.indentSuppressed || !this.metadata.title)
			this.element.addClass('suppressed');
		this.skipLastIndent = this.element.hasClass('view_all');
		
		this._renderIndent();
		
		for (var i in this.childEntries) {
			this.childEntries[i].initializeElement(rootElement);
		}
	};
		
	StructureEntry.prototype.getTemplate = function() {
		var toggleClass = '';
		
		var childrenPresent = this.countChildrenPresent(this);
		this.hasContent = this.childEntries && this.childEntries.length > 0;
		
		if (this.hasContent) {
			toggleClass = 'collapse';
		} else if ((this.metadata.counts && this.metadata.counts.containers) ||
				(this.options.structureView.options.retrieveFiles && this.metadata.counts && this.metadata.counts.child)) {
			toggleClass = 'expand';
		}
		
		var childCount = this.metadata.counts && this.metadata.counts.child? this.metadata.counts.child : null;
		
		var primaryAction = this.options.structureView.options.queryPath + "/" + this.metadata.id;
		if (!this.isAContainer)
			primaryAction = "record/" + this.metadata.id;
		else if (this.options.structureView.options.filterParams)
			primaryAction += "?" + decodeURIComponent(this.options.structureView.options.filterParams).replace('"', '%22');
		
		var downloadUrl = null;
		if ($.inArray('viewOriginal', this.metadata.permissions) != -1 && $.inArray('DATA_FILE', this.metadata.datastream) != -1){
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
	
	StructureEntry.prototype.countChildrenPresent = function() {
		if (!this.childEntries)
			return 0;
		
		var count = this.childEntries.length;
		for (var i in this.childEntries) {
			count += this.childEntries[i].countChildrenPresent();
		}
		
		return count;
	};
	
	StructureEntry.prototype.toggleChildren = function(onlyOpen) {
		var self = this;
		var $toggleButton = this.$entry.find('.cont_toggle');
		var $childrenContainer = this.element.children(".children");
		if ($toggleButton.hasClass('expand')) {
			if (!this.hasContent && !this.contentLoaded) {
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
							var existingEntries = self.childEntries;
							self.childEntries = [];
							$childrenContainer.empty();
							if (data.root && data.root.children && data.root.children.length > 0) {
								// Add all children into the new children set, both existing and new children
								for (var i in data.root.children) {
									var resultChild = data.root.children[i];
									// Check if there is an existing child for this result id
									var childEntry = self.findEntryById(resultChild.entry.id, existingEntries);
									if (!childEntry) {
										// No existing child, so use the new data to create child
										childEntry = new StructureEntry({
											node : resultChild,
											structureView : self.options.structureView
										});
									}
									
									self.childEntries.push(childEntry);
									$childrenContainer.append(childEntry.getTemplate());
								}
								
								for (var i in self.childEntries) {
									self.childEntries[i].initializeElement(self.element);
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
		} else if (!onlyOpen && $toggleButton.hasClass('collapse')) {
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
	
	StructureEntry.prototype.deselect = function() {
		this.element.removeClass("selected");
		this.options.isSelected = false;
	};
	
	StructureEntry.prototype.findEntryById = function(id, childEntries) {
		if (this.metadata.id == id)
			return this;
			
		var entries = childEntries? childEntries : this.childEntries;
		for (var index in entries) {
			var result = entries[index].findEntryById(id);
			if (result)
				return result;
		}
		return null;
	};
	
	return StructureEntry;
});