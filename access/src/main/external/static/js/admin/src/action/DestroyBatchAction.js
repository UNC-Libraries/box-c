define('DestroyBatchAction', [ 'jquery', 'AbstractBatchAction'], function($, AbstractBatchAction) {
	function DestroyBatchAction(context) {
		this._create(context);
	};
	
	DestroyBatchAction.prototype.constructor = DestroyBatchAction;
	DestroyBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	DestroyBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("purgeForever", target.metadata.permissions) != -1
			&& $.inArray("Deleted", target.getMetadata().status) != -1;
	};
	
	DestroyBatchAction.prototype.execute = function() {
		var containsCollection = false;
		var deleteList = $("<ul class='confirm_selected_list'></ul>");
		// Add valid targets to the confirmation text
		for (var id in this.resultList.resultObjects) {
			var resultObject = this.resultList.resultObjects[id];
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
		
		var confirmMatchText;
		var warning;
		var undoWarning = "This action <span class='bold'>cannot</span> be undone.";
		if (containsCollection) {
			confirmMatchText = "delete";
			warning = "All objects listed below, including at least one <span class='bold'>collection</span>, will be permanently removed from the repository along with all of their contents.";
			undoWarning += "  To confirm, type '<span class='bold'>delete</span>' below.";
		} else {
			warning = "All objects listed below will be permanently removed from the repository along with all of their contents.";
		}
		message.append("<p>" + warning + "</p>")
		
		message.append("<p>" + undoWarning + "</p>")
			.append(deleteList);
			
		this.context.confirm = {
			promptText : message,
			confirmText : 'Confirm',
			confirmAnchor : null,
			confirmMatchText : confirmMatchText,
			dialogOptions : {
				width : 400,
				modal : true,
				position : 'center'
			}
		};
		
		AbstractBatchAction.prototype.execute.call(this);
	}
	
	DestroyBatchAction.prototype.doWork = function() {
		var validTargets = this.getTargets();
		
		for (var index in validTargets) {
			this.actionHandler.addEvent({
				action : 'DestroyResult',
				target : validTargets[index],
				confirm : false
			});
		}
	};
	
	return DestroyBatchAction;
});