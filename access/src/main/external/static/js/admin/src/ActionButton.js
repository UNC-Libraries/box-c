define('ActionButton', ['jquery', 'ResultObjectList', 'ConfirmationDialog'], function($, ResultObjectList, ConfirmationDialog) {

	function ActionButton(options, element) {
		this._create(options, element);
	};
	
	var defaultOptions = {
			confirmAnchor : undefined,
			confirmMessage : "Are you sure?",
			confirm : false,
			animateSpeed: 'fast'
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
			var dialogOptions = {
					width : 'auto',
					modal : true
				};
				
			if (this.options.confirmAnchor) {
				dialogOptions['position'] = {};
				dialogOptions['position']['of'] = this.options.confirmAnchor; 
			}
		
			var confirmationDialog = new ConfirmationDialog({
				'promptText' : this.options.confirmMessage,
				'confirmFunction' : this.doWork,
				'confirmTarget' : this,
				'dialogOptions' : dialogOptions,
				autoOpen : true
			});
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
