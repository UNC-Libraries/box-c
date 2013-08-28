define('IngestPackageForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/ingestPackageForm', 
		'ModalLoadingOverlay'], 
		function($, ui, _, RemoteStateChangeMonitor, createFormTemplate, ModalLoadingOverlay) {
	
	function IngestPackageForm(options) {
		this.options = $.extend({}, options);
	};
	
	IngestPackageForm.prototype.open = function(pid) {
		var self = this;
		var formContents = createFormTemplate({pid : pid});
		
		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.$form = this.dialog.first();
		this.dialog.dialog({
			autoOpen: true,
			width: 'auto',
			minWidth: '500',
			height: 'auto',
			modal: true,
			title: 'Ingest package',
			close: function() {
				self.dialog.remove();
			}
		});
		
		$("input[type='file']", this.$form).change(function(){
			self.ingestFile = this.files[0];
			if (self.ingestFile)
				$(".file_info", self.$form).html(self.ingestFile.type + ", " + self.readableFileSize(self.ingestFile.size));
			else
				$(".file_info", self.$form).html("");
		});
		
		this.overlay = new ModalLoadingOverlay(this.$form, {autoOpen : false});
		this.submitted = false;
		
		this.$form.submit(function(){
			if (self.submitted)
				return false;
			errors = self.validationErrors();
			if (errors && errors.length > 0) {
				self.options.alertHandler.alertHandler("error", errors);
				return false;
			}
			
			self.submitted = true;
			self.overlay.show();
			self.submitAjax();
			return false;
		});
	};
	
	IngestPackageForm.prototype.readableFileSize = function(size) {
		var fileSize = 0;
		if (size > 1024 * 1024 * 1024)
			fileSize = (Math.round(size * 100 / (1024 * 1024 * 1024)) / 100).toString() + 'gb';
		if (size > 1024 * 1024)
			fileSize = (Math.round(size * 100 / (1024 * 1024)) / 100).toString() + 'mb';
		else
			fileSize = (Math.round(size * 100 / 1024) / 100).toString() + 'kb';
		return fileSize;
	}
	
	IngestPackageForm.prototype.submitAjax = function() {
		//var file = document.getElementById('ingest_package_file').files[0];
		var self = this, $form = this.$form.find("form"), formData = new FormData($form[0]);
		
		var xhr = new XMLHttpRequest();
		xhr.upload.addEventListener("progress", this.uploadProgress, false);
		xhr.addEventListener("load", function(event) {
			self.overlay.close();
			self.submitted = false;
			var data = null;
			try {
				data = JSON.parse(this.responseText);
			} catch (e) {
				if (typeof console != "undefined") console.log("Failed to parse ingest response", e);
			}
			if (this.status >= 400) {
				var message = "Failed to submit package " + self.ingestFile.name + " for ingest.";
				if (data && data.errorStack) {
					message += "  See errors below.";
					self.setError(data.errorStack);
				}
				self.options.alertHandler.alertHandler("error", message);
			} else {
				self.options.alertHandler.alertHandler("success", "Package " + self.ingestFile.name + " has been successfully uploaded for ingest.  You will receive an email when it completes.");
				self.dialog.dialog("close");
			}
		}, false);
		xhr.addEventListener("error", function(event) {
			this.options.alertHandler.alertHandler("error", "Failed to ingest package " + this.filename + ", see the errors below.");
			this.overlay.close();
			this.submitted = false;
		}, false);
		xhr.addEventListener("abort", function(event) {
			self.options.alertHandler.alertHandler("info", "Cancelled ingest of package " + self.ingestFile.name);
			self.overlay.close();
			self.submitted = false;
		}, false);
		xhr.open("POST", this.$form.find("form")[0].action);
		xhr.send(formData);
	};
	
	IngestPackageForm.prototype.uploadProgress = function(event) {
		// Update progress bar
	};
	
	IngestPackageForm.prototype.uploadCancelled = function(event) {
		this.options.alertHandler.alertHandler("info", "Cancelled ingest of package " + this.filename);
		this.overlay.close();
		this.submitted = false;
	};
	
	IngestPackageForm.prototype.setError = function(errorText) {
		$(".errors", this.$form).show();
		$(".error_stack", this.$form).html(errorText);
		this.dialog.dialog("option", "position", "center");
	};
	
	IngestPackageForm.prototype.validationErrors = function() {
		var errors = [];
		var packageFile = $("input[type='file']", this.$form).val();
		if (!packageFile)
			errors.push("You must select a file to ingest");
		return errors;
	};
	
	return IngestPackageForm;
});