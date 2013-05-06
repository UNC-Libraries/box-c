define([ 'jquery', 'jquery-ui', 'PID'], function(
		$, ui, PID) {
	$.widget("cdr.structureEntry", {
		_create : function() {
			
			// Setup expand/collapse based on class
			$(".cont_toggle").click(function(){
				
			});
			
			// Determine indent tier
			this.indentTier = this.element.parents(".entry_wrap").length;
			
			
			// Render indent
			$(".indent_wrap")
		}
	
	// Get contents, populate
	// Wrap tree
	});
});