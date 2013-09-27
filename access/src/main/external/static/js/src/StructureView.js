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
			excludeIds : null,
			retrieveFiles : false,
			selectedId : false
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
			
			// Generate the tree of entries starting from the root node
			this.rootEntry = new StructureEntry({
				node : this.options.rootNode,
				structureView : this,
				isRoot : true,
				isSelected : this.options.rootSelected
			});
			
			// Render the tree
			this.rootEntry.render();
			
			// If specified, select the selecte entry
			if (this.options.selectedId) {
				var selectedEntry = this.rootEntry.findEntryById(this.options.selectedId);
				if (selectedEntry)
					selectedEntry.select();
			}
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
			if (this.options.rootNode.isTopLevel)
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
						if (data.root.isTopLevel)
							$parentLink.addClass('disabled');
					}
				});
				return false;
			});
			
			this.$content.before($parentLink);
		}
	});
});