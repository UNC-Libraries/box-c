/**
 * Implements functionality and UI for the generic add container form
 */
define('AbstractForm', [ 'jquery', 'jquery-ui', 'underscore', 'ModalCreate',
		'ModalLoadingOverlay', 'AlertHandler'],
	function($, ui, _, ModalCreate, ModalLoadingOverlay) {
		function AbstractForm(options) {}

		AbstractForm.prototype.open = function(resultObject) {
			var dialogBox = new ModalCreate(this.options);
			var formContents = dialogBox.formContents(resultObject);
			var self = this;
			this.closed = false;
			this.resultObject = resultObject;

			this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
			this.$form = this.dialog.first();
			this.dialog.dialog = dialogBox.modalDialog(this.dialog, self);

			this.overlay = new ModalLoadingOverlay(this.$form, {
				autoOpen : false,
				type : 'icon',
				text : null,
				dialog : this.dialog
			});
			// Flag to track when the form has been submitted and needs to be locked
			this.submitted = false;

			this.$form.submit($.proxy(this.submit, self));
		};

		AbstractForm.prototype.submit = function(e) {
			e.preventDefault();
			this.preprocessForm(this.resultObject);
			
			errors = this.validationErrors();
			if (errors && errors.length > 0) {
				this.options.alertHandler.alertHandler("error", errors);
				return false;
			}

			this.submitted = true;
			this.overlay.open();
			this.submitAjax();

			return false;
		}

		AbstractForm.prototype.close = function() {
			if (this.closed) return;
			this.remove();
		};

		AbstractForm.prototype.remove = function() {
			if (this.closed) return;
			this.closed = true;
			this.dialog.remove();
			if (this.overlay)
				this.overlay.remove();
			if (this.closeConfirm)
				this.closeConfirm.remove();
		};

		AbstractForm.prototype.submitAjax = function() {
			var self = this, $form = this.$form.find("form"), formData = new FormData($form[0]);

			// Set up the request for XHR2 clients, register events
			this.xhr = new XMLHttpRequest();
			// Finished sending to queue without any network errors
			this.xhr.addEventListener("load", function(event) {
				var data = null;
				try {
					data = JSON.parse(this.responseText);
				} catch (e) {
					if (typeof console !== "undefined") console.log("Failed to parse ingest response", e);
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
			}, false);

			// Make request to either the computed action url, or url retrieved from form
			var action_url = this.action_url ? this.action_url : this.$form.find("form")[0].action;

			this.xhr.open("POST", action_url);
			this.xhr.send(formData);
		};

		AbstractForm.prototype.setError = function(errorText) {
			$(".errors", this.$form).show();
			$(".error_stack", this.$form).html(errorText);
			this.dialog.dialog("option", "position", "center");
		};

		AbstractForm.prototype.preprocessForm = function() {
		};

		AbstractForm.prototype.hideOverlay = function() {
			if (this.closed) return;
			if (this.closeConfirm)
				this.closeConfirm.close();
			this.overlay.close();
		};

		return AbstractForm;
	});