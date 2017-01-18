/**
 * Implements functionality and UI for adding an aggregate work to a collection or folder
 */
define('CreateWorkObjectForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createWorkObjectForm',
		'ModalCreate', 'ModalLoadingOverlay', 'ConfirmationDialog', 'ResultObject'],
		function($, ui, _, RemoteStateChangeMonitor, workObjectTemplate, ModalCreate, ModalLoadingOverlay, ConfirmationDialog, ResultObject) {
	
	var defaultOptions = {
		title : 'Add New Work',
		createFormTemplate : workObjectTemplate
	};

	function CreateWorkObjectForm(options) {
		this.options = $.extend({}, defaultOptions, options);
	}
	
	CreateWorkObjectForm.prototype.constructor = CreateWorkObjectForm;

	CreateWorkObjectForm.prototype.open = function(resultObject) {
		var self = this;
		this.closed = false;
		this.formSelected = false;

		var dialogBox = new ModalCreate(this.options);
		var formContents = dialogBox.formContents(resultObject);

		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.$form = this.dialog.first();
		
		// Initialize callback methods prior to creating dialog
		this.dialog.on("dialogclose", function(e, ui) {
			$(this).dialog("destroy");
		}).on("dialogresizestop", function(e, ui) {
			var height = $(this).dialog("option", "height") - 50;
			iframe.attr("height", height);
		}).on("dialogopen", function(e, ui) {
			self.resizeWidth();
		});
		$(window).on("resize", function(e, ui) {
			self.resizeWidth();
		});
		// Initialize the dialog widget
		dialogBox.modalDialog(this.dialog, self);

		var iframe = $("#work_submission_form", this.$form);
		// Retrieve the base url for the forms app from global settings
		var globalConfigs = requirejs.s.contexts._.config;
		var formsBaseUrl = globalConfigs.config.resultList.formsBaseUrl;

		// Load deposit form upon selection
		$("select[name='name']").on("change", function() {
			var ajaxIcon = $(".loading-icon");
			var collectionId = iframe.attr("title");

			iframe.attr("src", formsBaseUrl + "/" + $(this).val() + "?collection=" + collectionId + "&adminOnly=true");

			$(".admin-forms").addClass("in-admin-iframe");
			ajaxIcon.removeClass("in-admin-iframe");

			iframe.on("load", function () {
				$(this).removeClass("addwork");
				ajaxIcon.addClass("in-admin-iframe");
			});

			self.formSelected = true;
		});
	};
	
	CreateWorkObjectForm.prototype.close = function(e, ui) {
		if (this.formSelected) {
			if (confirm("Are you sure you'd like to exit?")) {
				return true;
			}
			return false;
		}
	}
	
	// Keep the dialog smaller than the total window size
	CreateWorkObjectForm.prototype.resizeWidth = function() {
		var vWidth = $(window).width();
		var dWidth = this.dialog.width();
		
		if (dWidth > vWidth) {
			this.dialog.dialog("option", "width", vWidth - 50);
		}
	};

	return CreateWorkObjectForm;
});