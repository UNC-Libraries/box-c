define('MoveDropLocation', [ 'jquery', 'jquery-ui', 'ConfirmationDialog'], 
		function($, ui, ConfirmationDialog) {
			
	var defaultOptions = {
		dropTargetSelector : undefined,
		dropTargetGetDataFunction : undefined,
		manager : undefined
	};
	
	function MoveDropLocation(element, options) {
		this.element = element;
		this.options = $.extend({}, options);
		this.manager = this.options.manager;
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
				var repTitle = representative.metadata.title;
				if (repTitle.length > 50) repTitle = repTitle.substring(0, 50) + "...";
				var destTitle = metadata.title;
				if (destTitle.length > 50) destTitle = destTitle.substring(0, 50) + "...";
				var promptText = "Move \"<a class='result_object_link' data-id='" + representative.pid + "'>" + repTitle + "</a>\"";
				if (self.manager.dragTargets.length > 1)
					promptText += " and " + (self.manager.length - 1) + " other object" + (self.manager.dragTargets.length - 1 > 1? "s" :"");
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
						if (self.manager.dragTargets) {
							var moveData = {
									newParent : metadata.id,
									ids : []
								};
							$.each(self.manager.dragTargets, function() {
								moveData.ids.push(this.pid);
								this.element.hide();
							});
							// Store a reference to the targeted item list since moving happens asynchronously
							var moveObjects = self.manager.dragTargets;
							$.ajax({
								url : "/services/api/edit/move",
								type : "POST",
								data : JSON.stringify(moveData),
								contentType: "application/json; charset=utf-8",
								dataType: "json",
								success : function(data) {
									$.each(moveObjects, function() {
										this.deleteElement();
									});
									self.manager.options.alertHandler.alertHandler("success", "Moved " + moveObjects.length + " object" + (moveObjects.length > 1? "s" : "") 
											+ " to " + destTitle);
								},
								error : function() {
									$.each(moveObjects, function() {
										this.element.show();
									});
									self.manager.options.alertHandler.alertHandler("error", "Failed to move " + moveObjects.length + " object" + (moveObjects.length > 1? "s" : "") 
											+ " to " + destTitle);
									
								}
							});
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
	
	return MoveDropLocation;
});