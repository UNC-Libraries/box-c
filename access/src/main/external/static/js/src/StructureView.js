define('StructureView', [ 'jquery', 'jquery-ui', 'StructureEntry'], function($, ui) {
	$.widget("cdr.structureView", {
		options : {
			showResourceIcons : true,
			indentSuppressed : false,
			showParentLink : false
		},
		_create : function() {
			
			this.element.wrapInner("<div class='structure_content'/>");
			this.$content = this.element.children();
			this.element.addClass('structure');
			if (!this.options.showResourceIcons)
				this.element.addClass('no_resource_icons');
			
			if (this.options.showParentLink) {
				this._generateParentLink();
			}
			
			// Instantiate entries recursively
			this.$content.find(".entry_wrap").structureEntry({
				indentSuppressed : this.options.indentSuppressed
			});
		},
		
		_generateParentLink : function() {
			var self = this;
			var $parentLink = $("<a class='parent_link'>parent</a>");
			if (self.$content.children(".entry_wrap").hasClass('root'))
				$parentLink.addClass('disabled');
				
			$parentLink.click(function(){
				if ($parentLink.hasClass('disabled'))
					return false;
				var $oldRoot = self.$content.children(".entry_wrap");
				var parentURL = $oldRoot.structureEntry('getParentURL');
				$.ajax({
					url : parentURL,
					success : function(data) {
						var $newRoot = $(data);
						// Initialize the new results
						$newRoot.find(".entry_wrap").add($newRoot).structureEntry({
							indentSuppressed : self.options.indentSuppressed
						});
						$newRoot.structureEntry('insertTree', $oldRoot);
						self.$content.append($newRoot);
						if (self.$content.children(".entry_wrap").hasClass('root'))
							$parentLink.addClass('disabled');
					}
				});
				return false;
			});
			
			this.$content.before($parentLink);
		}
	});
});