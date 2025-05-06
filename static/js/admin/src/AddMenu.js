define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'CreateContainerForm', 'IngestPackageForm', 'AddFileForm', 'ImportMetadataXMLForm', 'IngestFromSourceForm', 'qtip'],
		function($, ui, _, CreateContainerForm, IngestPackageForm, AddFileForm, ImportMetadataXMLForm, IngestFromSourceForm) {
	
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
			
			if (folderOrCollection
				|| (this.container.type === "AdminUnit" && $.inArray('createCollection', this.container.permissions) !== -1)
				|| (this.container.type === "ContentRoot" && $.inArray('createAdminUnit', this.container.permissions) !== -1)
			) {
				items["addContainer"] = {
					name : "Add " + CreateContainerForm.prototype.getContainerType(this.container)
				};
			}
			
			if (folderOrCollection) {
				items["addWork"] = { name : "Add Work", className: 'add-dcr-work' };
			}
			
			if (this.container.type === "Work") {
				items["addFile"] = { name : "Add File" };
				items["ingestSourceFilesOnly"] = { name : "Add Files from Server" }
			}
			
			if (folderOrCollection) {
				// items["ingestPackage"] = { name : "Add Ingest Package" }
				items["ingestSource"] = { name : "Add from File Server" }
			}
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
							case "ingestSourceFilesOnly" :
								new IngestFromSourceForm({
									alertHandler : self.options.alertHandler,
									filesOnlyMode : true
								}).open(self.container.id);
								break;
							case "addWork" :
								forms_app_store.setShowFormsModal(true);
								forms_app_store.setContainerId(self.container.id);
								forms_app_store.setAlertHandler(self.options.alertHandler);
								break;
							case "addFile" :
								new AddFileForm({
									alertHandler : self.options.alertHandler
								}).open(self.container.id);
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