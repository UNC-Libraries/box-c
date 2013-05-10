define([ 'jquery', 'jquery-ui', 'StructureEntry'], function(
		$, ui, PID) {
	$.widget("cdr.structureView", {
		options : {
			showResourceIcons : true
		},
		_create : function() {
			
			this.element.addClass('structure');
			if (!this.options.showResourceIcons)
				this.element.addClass('no_resource_icons');
			
			// Instantiate entries recursively
			this.element.find(".entry_wrap").structureEntry();
			
		}
	});
});