/**
 * Implements functionality and UI for the generic Ingest Package form
 */
define('IngestPackageForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/ingestPackageForm', 
		'ModalLoadingOverlay', 'ConfirmationDialog'], 
		function($, ui, _, RemoteStateChangeMonitor, createFormTemplate, ModalLoadingOverlay, ConfirmationDialog) {
	
	function IngestPackageForm(options) {
		this.options = $.extend({}, options);
	};
	
	IngestPackageForm.prototype.open = function(pid) {
		var self = this;
		var formContents = createFormTemplate({pid : pid});
		this.closed = false;
		
		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.$form = this.dialog.first();
		this.dialog.dialog({
			autoOpen: true,
			width: 'auto',
			minWidth: '500',
			height: 'auto',
			modal: true,
			title: 'Ingest package',
			close: $.proxy(self.close, self)
		});
		
		$("input[type='file']", this.$form).change(function(){
			self.ingestFile = this.files[0];
			if (self.ingestFile) {
				var fileInfo = "";
				if (self.ingestFile.type)
					fileInfo += self.ingestFile.type + ", ";
				fileInfo += self.readableFileSize(self.ingestFile.size);
				$(".file_info", self.$form).html(fileInfo);
			} else
				$(".file_info", self.$form).html("");
		});
		
		this.overlay = new ModalLoadingOverlay(this.$form, {
			autoOpen : false,
			type : 'determinate',
			text : 'uploading...'
		});
		// Flag to track when the form has been submitted and needs to be locked
		this.submitted = false;
		// Flag to track when a file upload is in progress
		this.uploadInProgress = false;
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
	
	IngestPackageForm.prototype.close = function() {
		var self = this;
		if (self.uploadInProgress) {
			this.closeConfirm = new ConfirmationDialog({
				promptText : 'Your submission currently uploading, do you wish to close this window and abort the upload?',
				confirmText : 'Close and continue',
				cancelText : 'Stay on page',
				confirmFunction : function() {
					self.remove();
					delete self.closeConfirm;
				},
				additionalButtons : {
					'Close and abort' : function() {
						$(this).dialog("close");
						self.xhr.abort();
						self.remove();
						delete self.closeConfirm;
					}
				}
			});
			return false;
		} else {
			this.remove();
		}
	};
	
	IngestPackageForm.prototype.remove = function() {
		if (this.closed) return;
		this.closed = true;
		this.dialog.remove();
		if (this.overlay)
			this.overlay.close();
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
	};
	
	IngestPackageForm.prototype.submitAjax = function() {
		var self = this, $form = this.$form.find("form"), formData = new FormData($form[0]);
		this.uploadInProgress = true;
		
		// Set up the request for XHR2 clients, register events
		this.xhr = new XMLHttpRequest();
		// Update the progress bar
		this.xhr.upload.addEventListener("progress", function(event) {
			if (event.total > 0) {
				var percent = event.loaded / event.total * 100;
				self.overlay.setProgress(percent);
			}
			if (event.loaded == event.total) {
				self.uploadInProgress = false;
				self.overlay.setText("upload complete, processing...");
			}
		}, false);
		
		// Finished sending to queue without any network errors
		this.xhr.addEventListener("load", function(event) {
			self.hideOverlay();
			self.submitted = false;
			self.uploadInProgress = false;
			var data = null;
			try {
				data = JSON.parse(this.responseText);
			} catch (e) {
				if (typeof console != "undefined") console.log("Failed to parse ingest response", e);
			}
			// Check for upload errors
			if (this.status >= 400) {
				var message = "Failed to submit package " + self.ingestFile.name + " for ingest.";
				if (data && data.errorStack && !self.closed) {
					message += "  See errors below.";
					self.setError(data.errorStack);
				}
				self.options.alertHandler.alertHandler("error", message);
			} else {
				// Ingest queueing was successful, let the user know and close the form
				self.options.alertHandler.alertHandler("success", "Package " + self.ingestFile.name + " has been successfully uploaded for ingest.  You will receive an email when it completes.");
				self.remove();
			}
		}, false);
		
		// Failed due to network problems
		this.xhr.addEventListener("error", function(event) {
			self.options.alertHandler.alertHandler("error", "Failed to ingest package " + self.ingestFile.name + ", see the errors below.");
			self.hideOverlay();
			self.submitted = false;
			self.uploadInProgress = false;
		}, false);
		
		// Upload aborted by the user
		this.xhr.addEventListener("abort", function(event) {
			self.options.alertHandler.alertHandler("info", "Cancelled ingest of package " + self.ingestFile.name);
			self.overlay.close();
			self.submitted = false;
			self.uploadInProgress = false;
		}, false);
		this.xhr.open("POST", this.$form.find("form")[0].action);
		this.xhr.send(formData);
	};
	
	IngestPackageForm.prototype.setError = function(errorText) {
		$(".errors", this.$form).show();
		$(".error_stack", this.$form).html(errorText);
		this.dialog.dialog("option", "position", "center");
	};
	
	// Validate the form and retrieve any errors
	IngestPackageForm.prototype.validationErrors = function() {
		var errors = [];
		var packageFile = $("input[type='file']", this.$form).val();
		if (!packageFile)
			errors.push("You must select a file to ingest");
		return errors;
	};
	
	IngestPackageForm.prototype.hideOverlay = function() {
		if (this.closed) return;
		this.overlay.hide();
		this.overlay.setProgress(0);
		this.overlay.setText("uploading...");
	};
	
	return IngestPackageForm;
});