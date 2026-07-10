define('RunEnhancementsBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!templates/admin/runEnhancementsForm"], function($, AbstractBatchAction, runEnhancementsTemplate) {

	function RunEnhancementsBatchAction(context) {
		this._create(context);
	};

	RunEnhancementsBatchAction.prototype.constructor = RunEnhancementsBatchAction;
	RunEnhancementsBatchAction.prototype = Object.create(AbstractBatchAction.prototype);

	RunEnhancementsBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("reindex", target.metadata.permissions) != -1;
	};

	RunEnhancementsBatchAction.prototype.getTargets = function() {
		if (this.context.targets) {
			return this.context.targets;
		}
		return AbstractBatchAction.prototype.getTargets.call(this);
	};

	RunEnhancementsBatchAction.prototype.getPids = function() {
		var targetIdsString = this.$dialog.find("#run_enhancements_ids").val() || "";

		return targetIdsString.split("\n").map(function(id) {
			return id.trim();
		}).filter(function(id) {
			return id.length > 0;
		});
	};

	RunEnhancementsBatchAction.prototype.execute = function() {
		var self = this;

		this.targets = this.getTargets();
		var title;
		if (this.targets.length === 1) {
			title = "Run enhancements on " + this.targets[0].metadata.title.substring(0, 30);
		} else {
			title = "Run enhancements on " + this.targets.length + " objects";
		}

		var targetIds = "";
		for (var index in this.targets) {
			targetIds += this.targets[index].getPid() + "\n";
		}
		var form = runEnhancementsTemplate({ targetIds: targetIds });
		this.$dialog = $("<div class='containingDialog'>" + form + "</div>");
		this.$dialog.dialog({
			autoOpen: true,
			width: "auto",
			minWidth: "500",
			modal: true,
			title: title,
			close: function() {
				self.$dialog.remove();
				self.$dialog = null;
			}
		});

		this.$dialog.find("form").first().off("submit").on("submit", function(e) {
			var $form = self.$dialog.find("form").first();
			var force = $form.find("#run_enhancements_force").prop("checked");
			var recursive = $form.find("#run_enhancements_recursive").prop("checked");
			// Collect all checked enhancement checkboxes.
			// Split any comma-separated checkbox values into individual enhancement names.
			// Convert it to a plain array and flatten the results.
			var enhancements = [...$form.find('input[name="enhancements"]:checked')
					.map(function() {
						return this.value.split(",");
					})
					.get()
					.flat()
			];
			var pids = self.getPids();

			if (!self.validateForm(enhancements, pids, $form)) {
				e.preventDefault();
				return false;
			}

			$.ajax({
				url: "/services/api/runEnhancements",
				type: "POST",
				contentType: "application/json; charset=utf-8",
				dataType: "json",
				data: JSON.stringify({
					force: force,
					pids: pids,
					recursive: recursive,
					enhancements: enhancements
				})
			}).done(function(response) {
				self.context.view.$alertHandler.alertHandler("message", response.message);
				self.$dialog.dialog("close");
			}).fail(function() {
				self.context.view.$alertHandler.alertHandler("error", "Failed to run enhancements for " + self.targets.length + " objects");
			});

			e.preventDefault();
		});
	};

	RunEnhancementsBatchAction.prototype.validateForm = function(enhancements, pids, $form) {
		var $errors = $(".errors", $form);
		var $errorStack = $(".error_stack", $form);
		var errors = [];

		$errors.hide();
		$errorStack.empty();

		if (!this.targets || this.targets.length === 0 || pids.length === 0) {
			errors.push("Select at least one target to run enhancements on.");
		}

		if (enhancements.length === 0) {
			errors.push("Select at least one enhancement to run.");
		}

		if (errors.length > 0) {
			errors.forEach(function(error) {
				$errorStack.append($("<div>").text(error));
			});
			$errors.show();
			this.$dialog.dialog("option", "position", "center");

			return false;
		}

		return true;
	};

	return RunEnhancementsBatchAction;
});