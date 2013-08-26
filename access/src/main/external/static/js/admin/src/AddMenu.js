define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'tpl!../templates/admin/addMenu', 'CreateContainerForm', 'IngestPackageForm', 'qtip'],
		function($, ui, _, addMenuTemplate, CreateContainerForm, IngestPackageForm) {
	
	function AddMenu($menuButton, options) {
		this.$menuButton = $menuButton;
		this.options = $.extend({}, options);
		this.init();
	};
	
	AddMenu.prototype.init = function() {
		//var $addMenuButton = $("#add_menu", this.element);
		this.$addMenu = $(addMenuTemplate());
		var self = this;
		
		// Set up the dropdown menu
		this.$menuButton.qtip({
			content: self.$addMenu,
			position: {
				at: "bottom right",
				my: "top right"
			},
			style: {
				classes: 'qtip-light',
				tip: false
			},
			show: {
				event: 'click',
				delay: 0
			},
			hide: {
				delay: 2000,
				event: 'unfocus mouseleave click',
				fixed: true, // Make sure we can interact with the qTip by setting it as fixed
				effect: function(offset) {
					$(this).fadeOut(100);
				}
			}
		});
		
		this.$addMenu.children().click(function(){
			this.$menuButton.qtip('hide');
		});
		
		var createContainerForm = new CreateContainerForm({
			alertHandler : self.options.alertHandler
		});
		var ingestPackageForm = new IngestPackageForm({
			alertHandler : self.options.alertHandler
		});
		this.$addMenu.children(".add_container_link").click(function(){
			createContainerForm.open(self.options.container.id);
		});
		
		this.$addMenu.children(".ingest_package_link").click(function(){
			ingestPackageForm.open(self.options.container.id);
		});
	};
	
	return AddMenu;
});