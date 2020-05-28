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

				// Check that we are not moving an object to itself
				try {
					$.each(self.manager.dragTargets, function() {
						if (this.pid == metadata.id) {
							throw "Invalid destination.  Object " + this.pid + " cannot be move into itself.";
						}
					});
				} catch (e) {
					self.manager.options.alertHandler.alertHandler("error", e);
					return false;
				}
				
				// Activate move drop mode
				self.manager.dropActive = true;
				
				// Confirm the move operation before performing it
				var representative = ui.draggable.data("resultObject");
				var promptText = '';
				var moveSingleObj = self.manager.dragTargets.length === 1;

				var destTitle = self._formatTitle(metadata.title);
				var targetAdminUnit = self._getAdminUnit(metadata.objectPath);

				var repTitle = self._formatTitle(representative.metadata.title);
				var currentAdminUnit = self._getAdminUnit(representative.metadata.objectPath);

				if (currentAdminUnit.pid !== targetAdminUnit.pid) {
					if (moveSingleObj) {
						promptText += " &quot;" + repTitle + "&quot; is ";
					} else {
						promptText += self.manager.dragTargets.length + " items are ";
					}
					promptText += "being moved from adminUnit &quot;" +
						self._formatTitle(currentAdminUnit.name) + "&quot; to &quot;" + destTitle + "&quot;" +
						" in adminUnit &quot;" + self._formatTitle(targetAdminUnit.name) + "&quot;.";
				}

				if (promptText === '') {
					promptText += "Move ";

					if (moveSingleObj) {
						promptText += "&quot;" + repTitle + "&quot;";
					} else {
						promptText += self.manager.dragTargets.length + " items";
					}

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
		if (active) {
			this.element.addClass("moving");
			this.element.on("click.dropClickBlocking", "a", function(e) {
				e.preventDefault();
			}).on("mouseenter.dropTargetHover", this.options.dropTargetSelector, function() {
				$(this).addClass("drop_hover");
			}).on("mouseleave.dropTargetLeave", this.options.dropTargetSelector, function() {
				$(this).removeClass("drop_hover");
			});
		} else {
			this.element.removeClass("moving");
			this.element.off("click.dropClickBlocking").off("mouseenter.dropTargetHover").off("mouseleave.dropTargetLeave");
		}
	};

	MoveDropLocation.prototype._getAdminUnit = function(objPath) {
		if (objPath.length > 1) {
			return objPath[1];
		}

		return undefined;
	};

	MoveDropLocation.prototype._formatTitle = function (title) {
		if (title.length > 50) {
			title = title.substring(0, 50) + "...";
		}

		return title;
	}
	
	return MoveDropLocation;
});