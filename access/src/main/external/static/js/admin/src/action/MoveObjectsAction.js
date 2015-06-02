define('MoveObjectsAction', ['jquery'], function($) {
	
	// Context parameters:
	// newParent - Result object for destination container or PID
	// targets - Array of result objects
	// destTitle - title of the destination container.  Optional.
	function MoveObjectsAction(context) {
		this.context = context;
	};

	MoveObjectsAction.prototype.execute = function() {
		var action = this;
		var moveData = {
				destination : this.context.newParent.id,
				moved : []
			};
		var destTitle = this.context.destTitle? this.context.destTitle : this.context.newParent.title;
		$.each(this.context.targets, function() {
			moveData.moved.push(this.pid);
		});
		// Store a reference to the targeted item list since moving happens asynchronously
		$.ajax({
			url : "/services/api/edit/move",
			type : "POST",
			data : JSON.stringify(moveData),
			contentType: "application/json; charset=utf-8",
			dataType: "json",
		}).done(function(data) {
			action.context.view.moveMonitor.addMove(data.id, moveData.moved, destTitle);
			
			action.context.alertHandler.alertHandler("message", "Started moving " + action.context.targets.length 
					+ " object" + (action.context.targets.length > 1? "s" : "") 
					+ " to " + destTitle);
		}).fail(function() {
			$.each(action.context.targets, function() {
				this.element.show();
			});
			action.context.alertHandler.alertHandler("error", "Failed to move " + action.context.targets.length 
					+ " object" + (action.context.targets.length > 1? "s" : "") 
					+ " to " + destTitle);
			
		});
	};

	return MoveObjectsAction;
});