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
			selectedId : false,
			onChangeEvent : undefined
		},
		_create : function() {
			
			this.element.wrapInner("<div class='structure_content'/>");
			this.$content = this.element.children();
			this.element.addClass('structure');
			if (!this.options.showResourceIcons)
				this.element.addClass('no_resource_icons');
			
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
		
		changeFolder : function(uuid) {
			if (uuid.indexOf(":") != -1) {
				uuid = uuid.substring(uuid.indexOf(":") + 1);
			}
			
			this.deselectAll();
			
			var entry = $("#str_" + uuid, this.element);
			if (entry.length == 0) {
				console.log("Failed to open folder", uuid);
				return;
			}
			
			entry = entry.data('structureEntry');
			entry.toggleChildren(true);
			entry.select();
		},
		
		deselectAll : function() {
			$(".entry_wrap.selected").each(function(){
				$(this).data('structureEntry').deselect();
			});
		},
		
		// Trigger the change event function in case some other part of the code needs to know the view changed sizes
		onChangeEvent : function(target) {
			if (this.options.onChangeEvent)
				this.options.onChangeEvent(target);
		}
	});
});