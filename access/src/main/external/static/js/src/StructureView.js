define('StructureView', [ 'jquery', 'jquery-ui', 'StructureEntry'], function($, ui, StructureEntry) {
	$.widget("cdr.structureView", {
		options : {
			showResourceIcons : true,
			indentSuppressed : false,
			showParentLink : false,
			secondaryActions : false,
			hideRoot : false,
			rootNode : null,
			queryPath : 'structure',
			filterParams : '',
			excludeIds : null
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
			
			if (this.options.excludeIds) {
				this.excludeIds = this.options.excludeIds.split(" ");
			}
			
			this.rootEntry = new StructureEntry({
				node : this.options.rootNode,
				structureView : this,
				isRoot : true
			});
			
			this.rootEntry.render();
			this.$content.append(this.rootEntry.element);
			
			this._initHandlers();
		},
		
		_initHandlers : function() {
			this.element.on("click", ".cont_toggle", function(){
				var structureEntry = $(this).parents(".entry_wrap").first().data('structureEntry');
				structureEntry.toggleChildren();
				return false;
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
				var parentURL = $oldRoot.data("structureEntry").getParentURL();
				$.ajax({
					url : parentURL,
					dataType : 'json',
					success : function(data) {
						var newRoot = new StructureEntry({
							node : data.root,
							structureView : self,
							isRoot : true
						});
						newRoot.render();
						// Initialize the new results
						//$newRoot.find(".entry_wrap").add($newRoot).structureEntry({
						//	indentSuppressed : self.options.indentSuppressed
						//});
						newRoot.insertTree($oldRoot.data('structureEntry'));
						//$newRoot.structureEntry('insertTree', $oldRoot);
						self.$content.append(newRoot.element);
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