define('EditTypeBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!../templates/admin/editTypeForm"], function($, AbstractBatchAction, editTypeTemplate) {
	function EditTypeBatchAction(context) {
		this._create(context);
	};
	
	EditTypeBatchAction.prototype.constructor = EditTypeBatchAction;
	EditTypeBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	EditTypeBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("editResourceType", target.metadata.permissions) != -1
			&& ("Collection" == target.getMetadata().type
			|| "Work" == target.getMetadata().type
			|| "Folder" == target.getMetadata().type);
	};
	
	EditTypeBatchAction.prototype.getTargets = function(targets) {
		if (this.context.targets) {
			return this.context.targets;
		} 
		return AbstractBatchAction.prototype.getTargets.call(this);
	};
	
	EditTypeBatchAction.prototype.execute = function() {
		var self = this;
		
		this.targets = this.getTargets();
		var title;
		var defaultType;
		if (this.targets.length == 1) {
			title = "Edit Type for " + this.targets[0].metadata.title.substring(0, 30);
			defaultType = this.targets[0].metadata.type;
		} else {
			title = "Edit Type for " + this.targets.length + " containers";
		}
		
		var editTypeForm = editTypeTemplate({defaultType : defaultType});
		this.dialog = $("<div class='containingDialog'>" + editTypeForm + "</div>");
		this.dialog.dialog({
			autoOpen: true,
			width: 'auto',
			minWidth: '500',
			height: 'auto',
			modal: true,
			title: title
		});
		this.$form = this.dialog.first();
		
		this.$form.submit(function(e){
			var newType = $("#edit_type_new_type", self.$form).val();
			var pids = [];
			for (var index in self.targets) {
				pids.push(self.targets[index].getPid());
				// Trigger refreshing of results
				self.context.actionHandler.addEvent({
					action : 'RefreshResult',
					target : self.targets[index],
					waitForUpdate : true
				});
			}
			
			$.ajax({
				url : "/services/api/edit/editType",
				type : "POST",
				contentType: "application/json; charset=utf-8",
				dataType: "json",
				data : JSON.stringify({
					newType : newType,
					pids : pids
				})
			}).done(function(reponse) {
				self.context.view.$alertHandler.alertHandler("message", "Conversion of " + self.targets.length + " object(s) to type " 
					+ newType + " has started.");
				self.dialog.remove();
			}).fail(function() {
				self.context.view.$alertHandler.alertHandler("error", "Failed to edit type for " + self.targets.length + " object(s) to type " 
					+ newType);
			});
			
			e.preventDefault();
		});
	}
	
	return EditTypeBatchAction;
});