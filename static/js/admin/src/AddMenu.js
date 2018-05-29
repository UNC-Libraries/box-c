define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'CreateContainerForm', 'IngestPackageForm', 'CreateWorkObjectForm', 'AddFileForm', 'ImportMetadataXMLForm', 'IngestFromSourceForm', 'qtip'],
		function($, ui, _, CreateContainerForm, IngestPackageForm, CreateWorkObjectForm, AddFileForm, ImportMetadataXMLForm, IngestFromSourceForm) {
	
	function AddMenu(options) {
		this.options = $.extend({}, options);
		this.container = this.options.container;
		this.init();
	};
	
	AddMenu.prototype.getMenuItems = function() {
		var items = {};

		if ($.inArray('ingest', this.container.permissions) != -1) {
			var self = this;

			items["addContainer"] = {
				name : "Add Container",
				visible: function(key, opt){
					return self.allowIngestInto(self);
				}
			};
			items["ingestPackage"] = {
				name : "Add Ingest Package",
				visible: function(key, opt){
					return self.allowIngestInto(self);
				}
			};
			items["ingestSource"] = {
				name : "Add from File Server",
				visible: function(key, opt){
					return self.allowIngestInto(self);
				}
			};
			items["addWork"] = {name : "Add Work",
				visible: function(key, opt){
					return self.allowIngestInto(self);
				}
			};
			items["addFile"] = {name : "Add File",
				visible: function(key, opt){
					if (self.container.type == "Work") {
						return true;
					}
					return false;
				}
			};

		}
		if ($.inArray('bulkUpdateDescription', this.container.permissions) != -1) {
			items["importMetadata"] = {name : "Import MODS"};
		}
		
		return items;
	};

	AddMenu.prototype.allowIngestInto = function(self) {
		if (self.container.type == "Work" || self.container.type == "File") {
			return false;
		}
		return true;
	};
	
	AddMenu.prototype.setContainer = function(container) {
		this.container = container;
		return this;
	};
	
	AddMenu.prototype.init = function() {
		var self = this;
		
		var items = self.getMenuItems();
		if ($.isEmptyObject(items))
			return;
		
		this.menu = $.contextMenu({
			selector: this.options.selector,
			trigger: 'left',
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
						}).open(self.container.id);
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
		});
	};
	
	return AddMenu;
});