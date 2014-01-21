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
		
		if (this.options.actionClass) {
			// Local instance of an action, which can be used for validation
			this.action = new this.options.actionClass(this.options.context);
		}
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
		if (this.options.disabled)
			return;
		if (this.options.context) {
			this.actionHandler.addEvent(this.options.context);
		}
	};
	
	return ActionButton;
});
