define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'CreateContainerForm', 'IngestPackageForm', 'CreateSimpleObjectForm', 'qtip'],
		function($, ui, _, CreateContainerForm, IngestPackageForm, CreateSimpleObjectForm) {
	
	function AddMenu(options) {
		this.options = $.extend({}, options);
		this.container = this.options.container;
		this.init();
	};
	
	AddMenu.prototype.getMenuItems = function() {
		var items = {};
		if ($.inArray('addRemoveContents', this.options.container.permissions) == -1)
			return items;
		items["addContainer"] = {name : "Add Container"};
		items["ingestPackage"] = {name : "Add Ingest Package"};
		items["simpleObject"] = {name : "Add Simple Object"};
		return items;
	};
	
	AddMenu.prototype.setContainer = function(container) {
		this.container = container;
	};
	
	AddMenu.prototype.init = function() {
		var self = this;
		
		var items = self.getMenuItems();
		if ($.isEmptyObject(items))
			return;
		var createContainerForm = new CreateContainerForm({
			alertHandler : this.options.alertHandler
		});
		var ingestPackageForm = new IngestPackageForm({
			alertHandler : this.options.alertHandler
		});
		var simpleObjectForm = new CreateSimpleObjectForm({
			alertHandler : this.options.alertHandler
		});
		
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
						createContainerForm.open(self.container.id);
						break;
					case "ingestPackage" :
						ingestPackageForm.open(self.container.id);
						break;
					case "simpleObject" :
						simpleObjectForm.open(self.container.id);
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