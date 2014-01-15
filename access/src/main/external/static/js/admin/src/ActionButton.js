define('ActionButton', ['jquery', 'ResultObjectList', 'ConfirmationDialog'], function($, ResultObjectList, ConfirmationDialog) {

	function ActionButton(options, element) {
		this._create(options, element);
	};
	
	var defaultOptions = {
			confirm : false
		};

	ActionButton.prototype._create = function(options, element) {
		this.options = $.extend({}, defaultOptions, options);
		this.actionHandler = this.options.actionHandler;
	};
	
	ActionButton.prototype.activate = function() {
		if (this.options.disabled)
			return;
			
		if (this.options.confirm) {
			var op = this;
			
			var confirmOptions = $.extend({
				confirmFunction : this.doWork,
				confirmTarget : this,
				autoOpen : true,
				dialogOptions : {
					width : 'auto',
					modal : true
				}
			}, this.options.confirm);
				
			if (confirmOptions.confirmAnchor) {
				confirmOptions.dialogOptions['position'] = {};
				confirmOptions.dialogOptions['position']['of'] = confirmOptions.confirmAnchor; 
			}
		
			var confirmationDialog = new ConfirmationDialog(confirmOptions);
		} else {
			this.doWork();
		}
	};
	
	ActionButton.prototype.disable = function() {
		this.options.disabled = true;
		if (this.element) {
			this.element.css("cursor", "default");
			this.element.addClass("disabled");
			this.element.attr('disabled', 'disabled');
		}
	};

	ActionButton.prototype.enable = function() {
		this.options.disabled = false;
		if (this.element) {
			this.element.css("cursor", "pointer");
			this.element.removeClass("disabled");
			this.element.removeAttr('disabled');
		}
	};
	
	ActionButton.prototype.doWork = function() {
	};
		
	ActionButton.prototype.getTargetIds = function() {
		var targetIds = [];
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (this.isValidTarget(resultObject))
				targetIds.push(resultObject.getPid());
		}
		return targetIds;
	};

	ActionButton.prototype.hasTargets = function() {
		for (var id in this.options.resultObjectList.resultObjects) {
			var resultObject = this.options.resultObjectList.resultObjects[id];
			if (this.isValidTarget(resultObject))
				return true;
		}
		return false;
	};
	
	ActionButton.prototype.isValidTarget = function(resultObject) {
		return resultObject.isSelected();
	};
	
	return ActionButton;
});
