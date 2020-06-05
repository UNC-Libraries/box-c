define('MoveDropLocation', [ 'jquery', 'jquery-ui', 'ConfirmationDialog'], 
		function($, ui, ConfirmationDialog) {
			
	var defaultOptions = {
		dropTargetSelector : undefined,
		dropTargetGetDataFunction : undefined,
		manager : undefined,
		actionHandler: undefined
	};
	
	function MoveDropLocation(element, options) {
		this.element = element;
		this.options = $.extend({}, options);
		this.manager = this.options.manager;
		this.actionHandler = this.options.actionHandler;
		this.create();
	}
	
	MoveDropLocation.prototype.create = function() {
		this.initDroppable();
	};
	
	MoveDropLocation.prototype.initDroppable = function() {
		var self = this;
		this.element.droppable({
			drop : function(event, ui) {
				// Locate which element is being dropped on
				var $dropTarget = $(document.elementFromPoint(event.pageX - $(window).scrollLeft(), event.pageY - $(window).scrollTop()));
				// Verify that the drop target matches the target selector
				if (!$dropTarget.is(self.options.dropTargetSelector))
					return false;
				// Verify that it is the correct type of element and retrieve metadata
				var metadata = self.options.dropTargetGetDataFunction($dropTarget);
				if (!metadata) return false;

				var destTitle = self._formatTitle(metadata.title);
				var targetAdminUnit = self._getAdminUnit(metadata.objectPath);
				var adminUnitList = [];

				// Check that we are not moving an object to itself
				try {
					$.each(self.manager.dragTargets, function() {
						if (this.pid === metadata.id) {
							throw "Invalid destination.  Object " + this.pid + " cannot be move into itself.";
						}

						// Return error message if dropTarget is invalid for the object being moved
						if (self._invalidTarget(this.metadata, metadata)) {
							throw "Invalid move location for " + self._formatTitle(this.metadata.title)
							+ " object" + (self.manager.dragTargets.length > 1 ? "s" : "")
							+ " to " + destTitle;
						}

						// Check if the object is being moved to another admin unit
						var currentAdminUnit = self._getAdminUnit(this.metadata.objectPath);

						if (currentAdminUnit !== undefined && targetAdminUnit !== undefined &&
							currentAdminUnit.pid !== targetAdminUnit.pid) {
							if (adminUnitList.findIndex((d) => d.pid === currentAdminUnit.pid) === -1) {
								adminUnitList.push(currentAdminUnit);
							}
						}
					});
				} catch (e) {
					self.manager.options.alertHandler.alertHandler("error", e);
					return false;
				}
				
				// Activate move drop mode
				self.manager.dropActive = true;
				
				// Confirm the move operation before performing it
				var promptText = '';
				var moveSingleObj = self.manager.dragTargets.length === 1;
				var representative = ui.draggable.data("resultObject");
				var repTitle = self._formatTitle(representative.metadata.title);

				// Multiple admin units
				var numAdminUnits = adminUnitList.length;

				if (numAdminUnits > 0) {
					promptText += self._msgText(moveSingleObj, repTitle);
					promptText += moveSingleObj ? " is" : " are";

					if (numAdminUnits === 1) {
						var unitTitle = self._formatTitle(adminUnitList[0].name);
						promptText += " being moved from adminUnit &quot;" +
							unitTitle + "&quot; to &quot;" + destTitle + "&quot;";
					} else {
						promptText += " being moved from multiple adminUnits" +
							" to &quot;" + destTitle + "&quot;";
					}

					promptText += " in adminUnit &quot;" + self._formatTitle(targetAdminUnit.name) + "&quot;.";
				}

				// Single admin unit
				if (promptText === '') {
					promptText += "Move " + self._msgText(moveSingleObj, repTitle);
					promptText += " into &quot;" + destTitle + "&quot;?";
				} else {
					promptText += " Continue with move?";
				}

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
						if (self.manager.dragTargets) {
							var event = {
								action : 'MoveObjects',
								newParent : metadata,
								targets : self.manager.dragTargets,
								alertHandler : self.manager.options.alertHandler
							};
							self.actionHandler.addEvent(event);
						}
						self.manager.deactivateMove();
					},
					cancelFunction : function() {
						// Cancel and revert the page state
						if (self.manager.dragTargets) {
							$.each(self.manager.dragTargets, function() {
								this.element.show();
							});
						}
						self.manager.deactivateMove();
					}
				});
			},
			tolerance: 'pointer',
			over: function(event, ui) {
				$(".ui-sortable-placeholder").hide();
			},
			out: function(event, ui) {
				$(".ui-sortable-placeholder").show();
			}
		});
	};
	
	MoveDropLocation.prototype.setMoveActive = function(active) {
		var targets = $('.structure_content a.res_link, .result_table a.res_link');

		if (active) {
			var self = this;

			// Highlight valid drop locations
			targets.each(function() {
				var selector = $(this);

				$.each(self.manager.dragTargets, function() {
					var destInfo = {
						id: selector.data("id"),
						path: selector.data("path"),
						type: selector.data("type")
					};

					if (!self._invalidTarget(this.metadata, destInfo)) {
						selector.addClass("moving");
					} else {
						selector.addClass("invalid_target");
					}
				});
			});

			this.element.on("click.dropClickBlocking", "a", function(e) {
				e.preventDefault();
			}).on("mouseenter.dropTargetHover", this.options.dropTargetSelector, function() {
				$(this).addClass("drop_hover");
			}).on("mouseleave.dropTargetLeave", this.options.dropTargetSelector, function() {
				$(this).removeClass("drop_hover");
			});
		} else {
			targets.removeClass("moving invalid_target"); // 3.3+ syntax for this changes to ["moving", "invalid_target"]
			this.element.off("click.dropClickBlocking").off("mouseenter.dropTargetHover").off("mouseleave.dropTargetLeave");
		}
	};

	MoveDropLocation.prototype._getAdminUnit = function(objPath) {
		if (objPath !== undefined && objPath.length > 1) {
			return objPath[1];
		}

		return undefined;
	};

	MoveDropLocation.prototype_adminUnitExists = function(adminUnit) {
		return adminUnit !== undefined && adminUnit.id
	}

	MoveDropLocation.prototype._msgText = function(singleObj, title) {
		return singleObj ? "&quot;" + title + "&quot;" : this.manager.dragTargets.length + " items";
	}

	MoveDropLocation.prototype._formatTitle = function (title) {
		if (title.length > 50) {
			title = title.substring(0, 50) + "...";
		}

		return title;
	}

	/**
	 * Check if destination object is a valid target
	 * @param target
	 * @param destination
	 * @returns {boolean|boolean}
	 * @private
	 */
	MoveDropLocation.prototype._invalidTarget = function(target, destination) {
		const TYPES = ['File', 'Work', 'Folder', 'Collection', 'AdminUnit', 'ContentRoot'];
		var targetLevel = TYPES.indexOf(target.type);
		var destLevel = TYPES.indexOf(destination.type);
		var ancestorPath = target.ancestorPath;

		var isRepoRoot = destLevel === 5;
		var isItself = target.id === destination.id;
		var isSameLevelNotFolder = targetLevel === destLevel && destLevel !== 2; // Check if objects are at the same level and aren't folders
		var isNonCollAdminUnit = targetLevel !== 3 && destLevel === 4;  // Check if trying to move a non-collection into an admin unit
		var isFileNonWork = targetLevel === 0 && destLevel !== 1; // Check if trying to move file to a non-work
		var isParent = ancestorPath[ancestorPath.length - 1].id === destination.id; // Check if dropping an object on its immediate parent
		var isChild = new RegExp(target.id).test(destination.path); // Check if dropping an object on one of its children

		if (isRepoRoot || isItself || isSameLevelNotFolder || isNonCollAdminUnit ||
			isFileNonWork || isParent || isChild) {
			return true;
		}

		return targetLevel > destLevel;
	};
	
	return MoveDropLocation;
});