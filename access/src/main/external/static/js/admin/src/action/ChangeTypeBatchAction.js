define('ChangeTypeBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!../templates/admin/changeTypeForm"], function($, AbstractBatchAction, changeTypeTemplate) {
	function ChangeTypeBatchAction(context) {
		this._create(context);
	};
	
	ChangeTypeBatchAction.prototype.constructor = ChangeTypeBatchAction;
	ChangeTypeBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	ChangeTypeBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("changeResourceType", target.metadata.permissions) != -1
			&& ("Collection" == target.getMetadata().type
			|| "Aggregate" == target.getMetadata().type
			|| "Folder" == target.getMetadata().type);
	};
	
	ChangeTypeBatchAction.prototype.getTargets = function(targets) {
		if (this.context.targets) {
			return this.context.targets;
		} 
		return AbstractBatchAction.prototype.getTargets.call(this);
	};
	
	ChangeTypeBatchAction.prototype.execute = function() {
		var self = this;
		
		this.targets = this.getTargets();
		var title;
		var defaultType;
		if (this.targets.length == 1) {
			title = "Change type for " + this.targets[0].metadata.title.substring(0, 30);
			defaultType = this.targets[0].metadata.type;
		} else {
			title = "Change type for " + this.targets.length + " containers";
		}
		
		var changeTypeForm = changeTypeTemplate({defaultType : defaultType});
		this.dialog = $("<div class='containingDialog'>" + changeTypeForm + "</div>");
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
			var newType = $("#change_type_new_type", self.$form).val();
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
				url : "/services/api/edit/changeType",
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
				self.context.view.$alertHandler.alertHandler("error", "Failed to change type for " + self.targets.length + " object(s) to type " 
					+ newType);
			});
			
			e.preventDefault();
		});
	}
	
	return ChangeTypeBatchAction;
});