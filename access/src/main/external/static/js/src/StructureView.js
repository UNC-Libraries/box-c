define([ 'jquery', 'jquery-ui', 'PID'], function(
		$, ui, PID) {
	$.widget("cdr.structureView", {
		_create : function() {
			
			this.element.addClass('structure');
			
			// Instantiate entries recursively
			this.element.children(".entry_wrap");
			
		}
	});
});