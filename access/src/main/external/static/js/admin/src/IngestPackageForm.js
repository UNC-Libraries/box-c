define('IngestPackageForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/ingestPackageForm', 
		'ModalLoadingOverlay'], 
		function($, ui, _, RemoteStateChangeMonitor, createFormTemplate, ModalLoadingOverlay) {
	
	function IngestPackageForm(options) {
		this.options = $.extend({}, options);
	};
	
	IngestPackageForm.prototype.open = function(pid) {
		var self = this;
		this.formContents = createFormTemplate({pid : pid});
		
		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.dialog.dialog({
			autoOpen: true,
			width: 500,
			height: 'auto',
			maxHeight: 300,
			minWidth: 500,
			modal: true,
			title: 'Ingest package',
			close: function() {
				self.dialog.remove();
			}
		});
		
		this.$form = this.dialog.first();
		$("input[type='file']", this.$form).change(function(){
			self.ingestFile = this.files[0];
		});
		
		this.overlay = new ModalLoadingOverlay($form, {autoOpen : false});
		this.submitted = false;
		
		this.$form.submit(function(){
			
			
			if (submitted)
				return false;
			/*errors = self.validationErrors($form);
			if (errors && errors.length > 0) {
				self.options.alertHandler.alertHandler("error", errors);
				return false;
			}*/
			
			submitted = true;
			overlay.show();
			self.submitAjax();
		});
		
		/*$("#upload_create_container").load(function(){
			if (!this.contentDocument.body.innerHTML)
				return;
			try {
				overlay.hide();
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
					overlay.close();
					dialog.dialog("close");
				}
				$(this).empty();
			} catch (e) {
				submitted = false;
				self.options.alertHandler.alertHandler("error", "An error occurred while creating container");
				console.log(e);
			}
		});*/
	};
	
	IngestPackageForm.prototype.submitAjax = function() {
		//var file = document.getElementById('ingest_package_file').files[0];
		var formData = this.$form[0].getFormData();
		
		var xhr = new XMLHttpRequest();
		xhr.upload.addEventListener("progress", this.uploadProgress, false);
		xhr.addEventListener("load", this.uploadComplete, false);
		xhr.addEventListener("error", this.uploadFailed, false);
		xhr.addEventListener("abort", this.uploadCancelled, false);
		xhr.open("POST", this.$form[0].action);
		xhr.send(formData);
	};
	
	IngestPackageForm.prototype.uploadProgress = function(event) {
		// Update progress bar
	};
	
	IngestPackageForm.prototype.uploadComplete = function(event) {
		this.options.alertHandler.alertHandler("success", "Package " + this.filename + " has been successfully uploaded for ingest.  You will receive an email when it completes.");
		this.overlay.close();
		this.dialog.dialog("close");
	};
	
	IngestPackageForm.prototype.uploadFailed = function(event) {
		this.options.alertHandler.alertHandler("error", "Failed to ingest package " + this.filename + ", see the errors below.");
		this.overlay.close();
		this.submitted = false;
	};
	
	IngestPackageForm.prototype.uploadCancelled = function(event) {
		this.options.alertHandler.alertHandler("info", "Cancelled ingest of package " + this.filename);
		this.overlay.close();
		this.submitted = false;
	};
	
	IngestPackageForm.prototype.validationErrors = function($form) {
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
	
	return IngestPackageForm;
});