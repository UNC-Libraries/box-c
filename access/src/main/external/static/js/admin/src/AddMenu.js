define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'CreateContainerForm', 'IngestPackageForm', 'qtip'],
		function($, ui, _, CreateContainerForm, IngestPackageForm) {
	
	function AddMenu(options) {
		this.options = $.extend({}, options);
		this.init();
	};
	
	AddMenu.prototype.getMenuItems = function() {
		var items = {};
		if ($.inArray('addRemoveContents', this.options.container.permissions) == -1)
			return items;
		items["addContainer"] = {name : "Add container"};
		items["ingestPackage"] = {name : "Ingest Package"};
		return items;
	};
	
	AddMenu.prototype.init = function() {
		var self = this;
		
		var items = self.getMenuItems();
		if (items.length == 0)
			return;
		var createContainerForm = new CreateContainerForm({
			alertHandler : this.options.alertHandler
		});
		var ingestPackageForm = new IngestPackageForm({
			alertHandler : this.options.alertHandler
		});
		
		$.contextMenu({
			selector: this.options.selector,
			trigger: 'left',
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
						createContainerForm.open(self.options.container.id);
						break;
					case "ingestPackage" :
						ingestPackageForm.open(self.options.container.id);
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