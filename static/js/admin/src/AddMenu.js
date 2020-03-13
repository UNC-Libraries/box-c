define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'CreateContainerForm', 'IngestPackageForm', 'CreateWorkObjectForm', 'AddFileForm', 'ImportMetadataXMLForm', 'IngestFromSourceForm', 'qtip'],
		function($, ui, _, CreateContainerForm, IngestPackageForm, CreateWorkObjectForm, AddFileForm, ImportMetadataXMLForm, IngestFromSourceForm) {
	
	function AddMenu(options) {
		this.options = $.extend({}, options);
		this.container = this.options.container;
		this.init();
		this.refresh();
	};
	
	AddMenu.prototype.getMenuItems = function() {
		var canIngest = $.inArray('ingest', this.container.permissions) !== -1;
		var items = {};

		if (canIngest) {
			var self = this;
			var folderOrCollection = this.container.type === "Folder" || this.container.type === "Collection";
			var workOrFile = this.container.type === "Work" || this.container.type === "File";
			
			if (folderOrCollection
				|| (this.container.type === "AdminUnit" && $.inArray('createCollection', this.container.permissions) !== -1)
				|| (this.container.type === "ContentRoot" && $.inArray('createAdminUnit', this.container.permissions) !== -1)
			) {
				items["addContainer"] = {
					name : "Add " + CreateContainerForm.prototype.getContainerType(this.container)
				};
			}
			
			if (folderOrCollection) {
				items["addWork"] = { name : "Add Work" };
			}
			
			if (this.container.type == "Work") {
				items["addFile"] = { name : "Add File" };
			}
			
			if (!workOrFile) {
				// items["ingestPackage"] = { name : "Add Ingest Package" }
				items["ingestSource"] = { name : "Add from File Server" }
			}
		}
		if ($.inArray('bulkUpdateDescription', this.container.permissions) !== -1) {
			items["importMetadata"] = {name : "Import MODS"};
		}
		
		return items;
	};
	
	AddMenu.prototype.setContainer = function(container) {
		this.container = container;
		return this;
	};
	
	AddMenu.prototype.refresh = function() {
		var items = this.getMenuItems();
		if ($.isEmptyObject(items)) {
			$(this.options.selector).hide();
			return;
		} else {
			$(this.options.selector).show();
		}
	}
	
	AddMenu.prototype.init = function() {
		var self = this;
		
		this.menu = $.contextMenu({
			selector: this.options.selector,
			trigger: 'left',
			build: function($triggerEvent, e) {
				var items = self.getMenuItems();
				
				return {
					className: 'add_to_container_menu', 
					events : {
						show: function() {
							this.addClass("active");
						},
						hide: function() {
							this.removeClass("active");
						}
					},
					items: items,
					callback : function(key, options) {
						switch (key) {
							case "addContainer" :
								new CreateContainerForm({
									alertHandler : self.options.alertHandler
								}).open(self.container);
								break;
							case "ingestPackage" :
								new IngestPackageForm({
									alertHandler : self.options.alertHandler
								}).open(self.container.id);
								break;
							case "ingestSource" :
								new IngestFromSourceForm({
									alertHandler : self.options.alertHandler
								}).open(self.container.id);
								break;
							case "addWork" :
								new CreateWorkObjectForm({
									alertHandler : self.options.alertHandler
								}).open(self.container.id);
								break;
							case "addFile" :
								new AddFileForm({
									alertHandler : self.options.alertHandler
								}).open(self.container.id);
								break;
							case "importMetadata" :
								new ImportMetadataXMLForm({
									alertHandler : self.options.alertHandler
								}).open();
								break;
						}
					},
					position : function(options, x, y) {
						options.$menu.position({
							my : "right top",
							at : "right bottom",
							of : options.$trigger
						});
					}
				}
			}
		});
	};
	
	return AddMenu;
});