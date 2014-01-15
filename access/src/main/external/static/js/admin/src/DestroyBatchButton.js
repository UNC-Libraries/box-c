define('DestroyBatchButton', ['jquery', 'BatchActionButton'], function($, BatchActionButton) {
	function DestroyBatchButton(options, element) {
		this._create(options, element);
	};
	
	DestroyBatchButton.prototype.constructor = DestroyBatchButton;
	DestroyBatchButton.prototype = Object.create( BatchActionButton.prototype );
	
	var defaultOptions = {
		confirm: {
			confirmText : 'Confirm',
			confirmAnchor : null,
			dialogOptions : {
				width : 400,
				modal : true,
				position : 'center'
			}
		}
	};
	
	DestroyBatchButton.prototype._create = function(options, element) {
		var merged = $.extend({}, defaultOptions, options);
		merged.confirmMessage = $("<h3>Permanently destroy selected object" +  + "?</h3>")
		
		merged.confirmAnchor = element;
		BatchActionButton.prototype._create.call(this, merged, element);
	};
	
	DestroyBatchButton.prototype.activate = function() {
		if (this.options.disabled)
			return;
		
		var containsCollection = false;
		var deleteList = $("<ul class='confirm_selected_list'></ul>");
		// Add valid targets to the confirmation text
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (this.isValidTarget(resultObject)) {
				if (resultObject.metadata.type == 'Collection') {
					containsCollection = true;
					deleteList.append("<li class='collection'>" + resultObject.metadata.title + " (Collection)</li>");
				} else {
					deleteList.append("<li>" + resultObject.metadata.title + "</li>");
				}
			}
		}
		
		var message = $("<p></p>");
		message.append("<h3>Permanently destroy " + deleteList.children().length + " selected object" + (deleteList.children().length != 1? "s" : "") + "?</h3>");
		
		var warning;
		var undoWarning = "This action <span class='bold'>cannot</span> be undone.";
		if (containsCollection) {
			this.options.confirm.confirmMatchText = "delete";
			warning = "All objects listed below, including at least one <span class='bold'>collection</span>, will be permanently removed from the repository along with all of their contents.";
			undoWarning += "  To confirm, type '<span class='bold'>delete</span>' below.";
		} else {
			warning = "All objects listed below will be permanently removed from the repository along with all of their contents.";
		}
		message.append("<p>" + warning + "</p>")
		
		message.append("<p>" + undoWarning + "</p>")
			.append(deleteList);
			
		this.options.confirm.promptText = message;
		
		BatchActionButton.prototype.activate.call(this);
	}
	
	DestroyBatchButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected() && resultObject.isEnabled()
			&& $.inArray("Deleted", resultObject.getMetadata().status) != -1;
	};
	
	DestroyBatchButton.prototype.doWork = function() {
		this.disable();
		this.targetIds = this.getTargetIds();
	
		for (var index in this.targetIds) {
			this.actionHandler.addEvent({
				action : 'DestroyResult',
				target : this.options.resultObjectList.resultObjects[this.targetIds[index]],
				confirm : false
			});
		}
		this.enable();
	};
	
	return DestroyBatchButton;
});