define('RunEnhancementsBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!../templates/admin/runEnhancementsForm"], function($, AbstractBatchAction, runEnhancementsTemplate) {

	function RunEnhancementsBatchAction(context) {
		this._create(context);
	};
	
	RunEnhancementsBatchAction.prototype.constructor = RunEnhancementsBatchAction;
	RunEnhancementsBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	RunEnhancementsBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("reindex", target.metadata.permissions) != -1;
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

		var targetIds = "";
		for (var index in this.targets) {
			targetIds += this.targets[index].getPid() + "\n";
		}
		var form = runEnhancementsTemplate({ targetIds : targetIds });
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
			var recursive = document.getElementById('run_enhancements_recursive').checked;
			var targetIdsString = document.getElementById('run_enhancements_ids').value;

			var pids = targetIdsString.split("\n").map((id) => id.trim()).filter((id) => id.length > 0);

			$.ajax({
				url : "/services/api/runEnhancements",
				type : "POST",
				contentType: "application/json; charset=utf-8",
				dataType: "json",
				data : JSON.stringify({
					force : force,
					pids : pids,
					recursive : recursive
				})
			}).done(function(response) {
				self.context.view.$alertHandler.alertHandler("message", response.message);
				self.dialog.dialog("destroy");
			}).fail(function() {
				self.context.view.$alertHandler.alertHandler("error", "Failed to run enhancements for " + self.targets.length + " objects");
			});
			
			e.preventDefault();
		});
	}
	
	return RunEnhancementsBatchAction;
});