/**
 * Implements functionality and UI for adding an aggregate work to a collection or folder
 */
define('CreateWorkObjectForm', [ 'jquery', 'jquery-ui', 'underscore', 'RemoteStateChangeMonitor', 'tpl!../templates/admin/createWorkObjectForm',
		'ModalCreate', 'ModalLoadingOverlay', 'ConfirmationDialog', 'AbstractFileUploadForm', 'ResultObject'],
		function($, ui, _, RemoteStateChangeMonitor, workObjectTemplate, ModalCreate, ModalLoadingOverlay, ConfirmationDialog, AbstractFileUploadForm, ResultObject) {
	
	var defaultOptions = {
		title : 'Add New Work',
		createFormTemplate : workObjectTemplate
	};

	function CreateWorkObjectForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	CreateWorkObjectForm.prototype.constructor = CreateWorkObjectForm;

	CreateWorkObjectForm.prototype.open = function(resultObject) {
		var self = this;
		this.closed = false;

		var dialogBox = new ModalCreate(this.options);
		var formContents = dialogBox.formContents(resultObject);

		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.$form = this.dialog.first();
		this.dialog.dialog = dialogBox.modalDialog(this.dialog, self);

		var containingDialog = $(".containingDialog");
		var iframe = $("#work_submission_form");
		var formSelected = false;

		$("select[name='name']").on("change", function() {
			var ajaxIcon = $(".loading-icon");

			var formUrl = location.href.split("/");
			var protocol = formUrl[0];
			var collectionId = iframe.attr("title");
			var host, subDirectory;

			if (/cdr/.test(formUrl[2])) {
				host = formUrl[2];
				subDirectory = "forms2/";
			} else {
				host = "localhost:4200";
				subDirectory = "";
			}

			iframe.attr("src", protocol + "//" + host + "/" + subDirectory + $(this).val() + "?collection=" + collectionId + "&adminOnly=true");

			$(".admin-forms").addClass("in-admin-iframe");
			ajaxIcon.removeClass("in-admin-iframe");

			iframe.on("load", function () {
				$(this).removeClass("addwork");
				ajaxIcon.addClass("in-admin-iframe");
			});

			formSelected = true;
		});

		containingDialog.on( "dialogbeforeclose", function(e, ui) {
			if(formSelected) {
				if (confirm("Are you sure you'd like to exit")) {
					return true;
				}
				return false;
			}
		}).on("dialogclose", function(e, ui) {
			$(this).dialog("destroy");
		}).on("dialogresizestop", function(e, ui) {
			var height = $(this).dialog("option", "height") - 50;
			iframe.attr("height", height);
		});
	};

	return CreateWorkObjectForm;
});