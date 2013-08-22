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
				self.unhighlight();
			}
		});
		
		$("#upload_create_container").load(function(){
			if (!this.contentDocument.body.innerHTML)
				return;
			try {
				var response = JSON.parse(this.contentDocument.body.innerHTML);
				if (response.error) {
					if (self.options.alertHandler)
						self.options.alertHandler.alertHandler("error", "An error occurred while creating container");
				} else if (response.pid) {
					if (self.options.alertHandler) {
						var name = $("#create_container_form input[name='name']").val();
						var type = $("#create_container_form select").val();
						self.options.alertHandler.alertHandler("success", "Created " + type + " " + name);
					}
				}
				$(this).empty();
			} catch (e) {
				self.options.alertHandler.alertHandler("error", "An error occurred while creating container");
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
	
	return CreateContainerForm;
});