define('ActionEventHandler', [ 'jquery'], function($) {
	function ActionEventHandler(options) {
		this._create(options);
	};
	
	var defaultOptions = {
	};
	
	ActionEventHandler.prototype._create = function(options) {
		this.options = $.extend({}, defaultOptions, options);
		
		this.baseContext = {
			actionHandler : this
		};
	};
	
	ActionEventHandler.prototype.addToBaseContext = function(key, value) {
		this.baseContext[key] = value;
	};
	
	ActionEventHandler.prototype.addEvent = function(event) {
		this._trigger(event);
	};
	
	ActionEventHandler.prototype._trigger = function(event) {
		var eventContext = $.extend({}, this.baseContext, event);
		
		require([eventContext.action + "Action"], function(actionClass) {
			var action = new actionClass(eventContext);
			action.execute();
		});
	};
	
	return ActionEventHandler;
});