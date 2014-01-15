define('DestroyResultAction', [ 'jquery', 'AjaxCallbackAction'], function($, AjaxCallbackAction) {
	function DestroyResultAction(context) {
		this._create(context);
	};
	
	DestroyResultAction.prototype.constructor = DestroyResultAction;
	DestroyResultAction.prototype = Object.create( AjaxCallbackAction.prototype );
	
	DestroyResultAction.prototype.actionName = "Destroy";
		
	DestroyResultAction.prototype._create = function(context) {
		this.context = context;
		
		var targetIsCollection = this.context.target.metadata && this.context.target.metadata.type == "Collection";
		
		var options = {
			workMethod: $.post,
			workLabel: "Destroying...",
			workPath: "/services/api/edit/destroy/{idPath}",
			followupLabel: "Cleaning up...",
			followupPath: "/services/api/status/item/{idPath}/solrRecord/version",
			confirm : 'confirm' in this.context && !this.context.confirm? false : {
				promptText: targetIsCollection ?
					$("<h3>Are you sure you want to destroy this collection?</h3>"
						+ "<p>This action will permanently destroy <span class='bold'>" + this.context.target.metadata.title.substring(0, 50) + "</span> and all of its contents.  It <span class='bold'>cannot</span> be undone.</p>"
						+ "<p>Please type in the name of the collection to confirm.</p>") :
					$("<h3>Permanently destroy " + (this.context.target.metadata? this.context.target.metadata.title.substring(0, 50) : "this object") 
						+ "?</h3><p>This action <span class='bold'>cannot</span> be undone.</p>"),
				confirmMatchText: targetIsCollection ? this.context.target.metadata.title.substring(0, 10) : undefined,
				confirmText : "Confirm",
				confirmAnchor : null,
				dialogOptions : {
					width : 400,
					modal : true,
					position : 'center'
				}
			}
		};
		
		AjaxCallbackAction.prototype._create.call(this, options);
	};

	DestroyResultAction.prototype.followup = function(data) {
		if (data == null) {
			return true;
		}
		return false;
	};

	DestroyResultAction.prototype.completeState = function() {
		if (this.context.target != null) {
			if (this.context.target.metadata)
				this.alertHandler.alertHandler("success", "Permanently destroyed item " 
						+ this.context.target.metadata.title + " (" + this.context.target.metadata.id + ")");
			else this.alertHandler.alertHandler("success", "Permanently destroyed item " + data);
			this.context.target.deleteElement();			
		}
	};

	return DestroyResultAction;
});