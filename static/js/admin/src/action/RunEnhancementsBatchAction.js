define('RunEnhancementsBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!../templates/admin/runEnhancementsForm"], function($, AbstractBatchAction, runEnhancementsTemplate) {

	function RunEnhancementsBatchAction(context) {
		this._create(context);
	};
	
	RunEnhancementsBatchAction.prototype.constructor = RunEnhancementsBatchAction;
	RunEnhancementsBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	RunEnhancementsBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("editAccessControl", target.metadata.permissions) != -1;
	};
	
	RunEnhancementsBatchAction.prototype.getTargets = function(targets) {
		if (this.context.targets) {
			return this.context.targets;
		} 
		return AbstractBatchAction.prototype.getTargets.call(this);
	};
	
	RunEnhancementsBatchAction.prototype.execute = function() {
		var self = this;
		
		var exportContainerMode = this.context.exportContainerMode;
		
		this.targets = this.getTargets();
		var title;
		if (this.targets.length == 1) {
			title = "Run enhancements on " + this.targets[0].metadata.title.substring(0, 30);
		} else {
			title = "Run enhancements on " + this.targets.length + " objects";
		}
		
		var form = runEnhancementsTemplate();
		this.dialog = $("<div class='containingDialog'>" + form + "</div>");
		this.dialog.dialog({
			autoOpen: true,
			width: 'auto',
			minWidth: '500',
			modal: true,
			title: title
		});
		this.$form = this.dialog.first();
		
		this.$form.submit(function(e){
			var force = document.getElementById('run_enhancements_force').checked;

			var pids = [];
			for (var index in self.targets) {
				pids.push(self.targets[index].getPid());
			}
			
			$.ajax({
				url : "runEnhancements",
				type : "POST",
				contentType: "application/json; charset=utf-8",
				dataType: "json",
				data : JSON.stringify({
					force : force,
					pids : pids
				})
			}).done(function(response) {
				self.context.view.$alertHandler.alertHandler("message", response.message);
				self.dialog.remove();
			}).fail(function() {
				self.context.view.$alertHandler.alertHandler("error", "Failed to run enhancements for " + self.targets.length + " objects");
			});
			
			e.preventDefault();
		});
	}
	
	return RunEnhancementsBatchAction;
});