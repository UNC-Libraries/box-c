/**
 * Implements functionality and UI for the generic Ingest Package form
 */
define('AbstractFileUploadForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'ModalCreate',
		'ModalLoadingOverlay', 'ConfirmationDialog', 'StringUtilities', 'ResultObject', 'AlertHandler'], 
		function($, ui, _, RemoteStateChangeMonitor, ModalCreate, ModalLoadingOverlay, ConfirmationDialog, StringUtilities, ResultObject) {
	
	var defaultOptions = {
		iframeSelector : "#upload_file_frame",
		showUploadProgress : true
	};
	
	function AbstractFileUploadForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	};
	
	AbstractFileUploadForm.prototype.getDefaultOptions = function () {
		return defaultOptions;
	};
	
	AbstractFileUploadForm.prototype.open = function(resultObject) {
		var dialogBox = new ModalCreate(this.options);
		var formContents = dialogBox.formContents(resultObject);
		var self = this;
		this.closed = false;
		
		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.$form = this.dialog.first();
		this.dialog.dialog = dialogBox.modalDialog(this.dialog, self);
		
		$("input[type='file']", this.$form).change(function(){
			self.ingestFile = this.files[0];
			if (self.ingestFile) {
				var fileInfo = "";
				if (self.ingestFile.type)
					fileInfo += self.ingestFile.type + ", ";
				fileInfo += StringUtilities.readableFileSize(self.ingestFile.size);
				$(".file_info", self.$form).html(fileInfo);
			} else
				$(".file_info", self.$form).html("");
		});
		
		this.overlay = new ModalLoadingOverlay(this.$form, {
			autoOpen : false,
			type : this.options.showUploadProgress? 'determinate' : 'icon',
			text : this.options.showUploadProgress? 'uploading...' : null,
			dialog : this.dialog
		});
		// Flag to track when the form has been submitted and needs to be locked
		this.submitted = false;
		// Flag to track when a file upload is in progress
		this.uploadInProgress = false;
		this.$form.submit(function(){
			if (self.submitted)
				return false;
			self.preprocessForm(resultObject);
			errors = self.validationErrors();
			if (errors && errors.length > 0) {
				self.options.alertHandler.alertHandler("error", errors);
				return false;
			}
			
			self.submitted = true;
			self.overlay.open();
			if (self.supportsAjaxUpload()) {
				self.submitAjax();
			} else {
				// Older browser or IE that doesn't support ajax file uploads, use iframe submit approach
				self.submitIFrame();
				return true;
			}
			return false;
		});
	};
	
	AbstractFileUploadForm.prototype.close = function() {
		var self = this;
		if (this.closed) return;
		if (this.uploadInProgress && this.options.showUploadProgress) {
			this.closeConfirm = new ConfirmationDialog({
				promptText : 'Your submission is currently uploading, do you wish to close this window and abort the upload?',
				confirmText : 'Close and continue',
				cancelText : 'Stay on page',
				'solo' : false,
				'dialogOptions' : {
					autoOpen : true,
					modal : true,
					width : 400,
					height: 'auto',
					position : {
						at : "right top",
						my : "right bottom",
						of : self.dialog
					}
				},
				confirmFunction : function() {
					self.unloadFunction = function(e) {
						return "There is an ongoing upload which will be interrupted if you leave this page, do you wish to continue?";
					};
					$(window).bind('beforeunload', self.unloadFunction);
					self.remove();
					this.closeConfirm = null;
					return false;
				},
				additionalButtons : {
					'Close and abort' : function() {
						$(this).dialog("close");
						if (self.xhr)
							self.xhr.abort();
						self.remove();
						this.closeConfirm = null;
					}
				}
			});
			return false;
		} else {
			this.remove();
		}
	};
	
	AbstractFileUploadForm.prototype.remove = function() {
		if (this.closed) return;
		this.closed = true;
		this.dialog.remove();
		if (this.overlay)
			this.overlay.remove();
		if (this.closeConfirm)
			this.closeConfirm.remove();
	};
	
	AbstractFileUploadForm.prototype.supportsAjaxUpload = function() {
		return new XMLHttpRequest().upload;
	};
	
	AbstractFileUploadForm.prototype.submitAjax = function() {
		var self = this, $form = this.$form.find("form"), formData = new FormData($form[0]);
		this.uploadInProgress = true;
		
		// Set up the request for XHR2 clients, register events
		this.xhr = new XMLHttpRequest();
		// Finished sending to queue without any network errors
		this.xhr.addEventListener("load", function(event) {
			self.uploadCompleted();
			var data = null;
			try {
				data = JSON.parse(this.responseText);
			} catch (e) {
				if (typeof console != "undefined") console.log("Failed to parse ingest response", e);
			}
			// Check for upload errors
			if (this.status >= 400) {
				self.options.alertHandler.alertHandler("error", self.getErrorMessage(data));
			} else {
				// Ingest queueing was successful, let the user know and close the form
				self.options.alertHandler.alertHandler("success", self.getSuccessMessage(data));
				self.remove();
			}
		}, false);
		
		// Failed due to network problems
		this.xhr.addEventListener("error", function(event) {
			self.options.alertHandler.alertHandler("error", self.getErrorMessage());
			self.uploadCompleted();
		}, false);
		
		if (this.options.showUploadProgress) {
			// Upload aborted by the user
			this.xhr.addEventListener("abort", function(event) {
				self.options.alertHandler.alertHandler("message", "Cancelled upload of " + self.ingestFile.name);
				self.uploadCompleted();
			}, false);
			
			// Update the progress bar
			this.xhr.upload.addEventListener("progress", function(event) {
				if (self.closed) return;
				if (event.total > 0) {
					var percent = event.loaded / event.total * 100;
					self.overlay.setProgress(percent);
				}
				if (event.loaded == event.total) {
					self.uploadInProgress = false;
					self.overlay.setText("upload complete, processing...");
				}
			}, false);
		}
		
		// Make request to either the computed action url, or url retrieved from form
		var action_url = this.action_url ? this.action_url : this.$form.find("form")[0].action;
		this.xhr.open("POST", action_url);
		this.xhr.send(formData);
	};
	
	AbstractFileUploadForm.prototype.uploadCompleted = function() {
		this.hideOverlay();
		this.submitted = false;
		this.uploadInProgress = false;
		if (this.unloadFunction) {
			$(window).unbind('beforeunload', this.unloadFunction);
			this.unloadFunction = null;
		}
	};
	
	// Traditional iframe file upload approach, monitor an iframe for changes to know when the submit completes
	AbstractFileUploadForm.prototype.submitIFrame = function() {
		var self = this;
		$(this.options.iframeSelector).load(function(){
			if (!this.contentDocument.body.innerHTML)
				return;
			self.uploadCompleted();
			var data = null;
			try {
				if (this.contentDocument.body.innerHTML)
					data = JSON.parse(this.contentDocument.body.innerHTML);
			} catch (e) {
				if (typeof console != "undefined") console.log("Failed to parse ingest response", e);
			}
			if (data.error) {
				self.options.alertHandler.alertHandler("error", self.getErrorMessage(data));
			} else {
				self.options.alertHandler.alertHandler("success", self.getSuccessMessage(data));
				self.remove();
			}
		});
	};
	
	AbstractFileUploadForm.prototype.setError = function(errorText) {
		$(".errors", this.$form).show();
		$(".error_stack", this.$form).html(errorText);
		this.dialog.dialog("option", "position", "center");
	};
	
	// Validate the form and retrieve any errors
	AbstractFileUploadForm.prototype.validationErrors = function() {
		var errors = [];
		var packageFile = $("input[type='file']", this.$form).val();
		if (!packageFile)
			errors.push("You must select a file to ingest");
		return errors;
	};
	
	AbstractFileUploadForm.prototype.preprocessForm = function() {
	};
	
	AbstractFileUploadForm.prototype.hideOverlay = function() {
		if (this.closed) return;
		if (this.closeConfirm)
			this.closeConfirm.close();
		this.overlay.close();
		if (this.options.showUploadProgress) {
			this.overlay.setProgress(0);
			this.overlay.setText("uploading...");
		}
	};
	
	return AbstractFileUploadForm;
});