define('CreateContainerForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createContainerForm', 
		'ModalLoadingOverlay'], 
		function($, ui, _, RemoteStateChangeMonitor, createFormTemplate, ModalLoadingOverlay) {
	
	var defaultOptions = {};
	
	function CreateContainerForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	};
	
	CreateContainerForm.prototype.open = function(pid) {
		var self = this, formContents = createFormTemplate({pid : pid});
		
		var dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		dialog.dialog({
			autoOpen: true,
			width: 500,
			height: 'auto',
			maxHeight: 300,
			minWidth: 500,
			modal: true,
			title: 'Create container',
			close: function() {
				dialog.remove();
			}
		});
		
		var $form = dialog.first();
		var overlay = new ModalLoadingOverlay($form, {autoOpen : false});
		var submitted = false;
		$form.submit(function(){
			if (submitted)
				return false;
			errors = self.validationErrors($form);
			if (errors && errors.length > 0) {
				self.options.alertHandler.alertHandler("error", errors);
				return false;
			}
			
			submitted = true;
			overlay.open();
		});
		
		$("#upload_create_container").load(function(){
			if (!this.contentDocument.body.innerHTML)
				return;
			try {
				overlay.close();
				var response = JSON.parse(this.contentDocument.body.innerHTML);
				var containerName = $("input[name='name']", $form).val();
				if (response.error) {
					if (self.options.alertHandler)
						self.options.alertHandler.alertHandler("error", "An error occurred while creating container");
					submitted = false;
				} else if (response.pid) {
					if (self.options.alertHandler) {
						var type = $("#create_container_form select").val();
						self.options.alertHandler.alertHandler("success", "Created " + type + " " + containerName + ", refresh the page to view");
					}
					overlay.remove();
					dialog.dialog("close");
				}
				$(this).empty();
			} catch (e) {
				submitted = false;
				self.options.alertHandler.alertHandler("error", "An error occurred while creating container");
				console.log(e);
			}
		});
		
		/*var fileUpload = dialog.find("input[type='file']").fileupload({
			autoUpload: false,
			submit: function (e, data) {
				/*$("#create_container_button").on('click', function () {
					data.submit();
				});* /
				return false;
			}
		});
		
		$("#create_container_button").on('click', function () {
			fileUpload.fileupload("submit");
			fileUpload.fileupload("send");
		});*/
		
		/*$("#create_container_button").on('click', function () {
			$("form[name='create_container_form']").submit();
			
			//data.submit();
		});*/
	};
	
	CreateContainerForm.prototype.validationErrors = function($form) {
		var errors = [];
		var containerName = $("input[name='name']", $form).val(),
			containerType = $("select", $form).val(),
			description = $("input[type='file']", $form).val();
		// Validate input
		if (!containerName)
			errors.push("You must specify a name for the folder");
		if (containerType == "collection" && !description)
			errors.push("A MODS description file must be provided when creating a collection");
		return errors;
	};
	
	return CreateContainerForm;
});