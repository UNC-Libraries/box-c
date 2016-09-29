/**
 * Implements functionality and UI for the generic Ingest Package form
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
	//CreateWorkObjectForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	CreateWorkObjectForm.prototype.open = function(resultObject) {
		var self = this;
		this.closed = false;

		var dialogBox = new ModalCreate(this.options);
		var formContents = dialogBox.formContents(resultObject);

		this.dialog = $("<div class='containingDialog'>" + formContents + "</div>");
		this.$form = this.dialog.first();
		this.dialog.dialog = dialogBox.modalDialog(this.dialog, self);

		$("select[name='name']").on("change", function() {
			var ajaxIcon = $(".loading-icon");
			var iframe = $("#work_submission_form");
			var formUrl = iframe.attr("src").split("/");
			var protocol = formUrl[0];
			var host = formUrl[2];
			var collectionId = iframe.attr("title");

			iframe.attr("src", protocol + "//" + host + "/" + $(this).val() + "?collection=" + collectionId + "&adminOnly=true");

			$(".admin-forms").addClass("in-admin-iframe");
			ajaxIcon.removeClass("in-admin-iframe");

			iframe.on("load", function () {
				$(this).removeClass("addwork");
				ajaxIcon.addClass("in-admin-iframe");
			});
		});

		$(".containingDialog").on("dialogclose", function(e, ui) {
			$(this).dialog("destroy");
		});
	};

	return CreateWorkObjectForm;
});