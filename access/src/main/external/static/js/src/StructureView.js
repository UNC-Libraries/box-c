define([ 'jquery', 'jquery-ui', 'StructureEntry'], function(
		$, ui, PID) {
	$.widget("cdr.structureView", {
		_create : function() {
			
			this.element.addClass('structure');
			
			// Instantiate entries recursively
			this.element.find(".entry_wrap").structureEntry();
			
		}
	});
});