define('ActionEventHandler', [ 'jquery'], function($) {
	function ActionEventHandler(options) {
		this._create(options);
	};
	
	var defaultOptions = {
	};
	
	ActionEventHandler.prototype._create = function(options) {
		this.options = $.extend({}, defaultOptions, options);
		
		this.baseContext = $.extend({
			actionHandler : this
		}, options.baseContext? options.baseContext : {});
	};
	
	ActionEventHandler.prototype.addToBaseContext = function(key, value) {
		this.baseContext[key] = value;
	};
	
	ActionEventHandler.prototype.addEvent = function(event) {
		this._trigger(event);
	};
	
	ActionEventHandler.prototype._trigger = function(event) {
		var eventContext = $.extend({}, this.baseContext, event);
		
		if ($.isFunction(eventContext.action)) {
			var action = new eventContext.action(eventContext);
			action.execute();
		} else {
			require([eventContext.action + "Action"], function(actionClass) {
				var action = new actionClass(eventContext);
				action.execute();
			});
		}
	};
	
	return ActionEventHandler;
});