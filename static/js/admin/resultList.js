require.config({
	urlArgs: "v=5.0-SNAPSHOT",
	baseUrl: "/static/",
	paths: {
		"jquery" : "assets/admin/cdr-admin",
		"jquery-ui" : "assets/admin/cdr-admin",
		"text" : "js/admin/lib/text",
		"underscore" : "js/admin/lib/underscore",
		"cycle": "js/admin/lib/cycle",
		"tpl" : "js/admin/lib/tpl",
		"qtip" : "js/admin/lib/jquery.qtip.min",
		"PID" : "assets/admin/cdr-admin",
		"AjaxCallbackButton" : "assets/admin/cdr-admin",
		"ResultObject" : "assets/admin/cdr-admin",
		"ResultObjectList" : "assets/admin/cdr-admin",
		"ParentResultObject" : "assets/admin/cdr-admin",
		"ModalCreate": "assets/admin/cdr-admin",
		"ModalLoadingOverlay" : "assets/admin/cdr-admin",
		"EditAccessControlForm" : "assets/admin/cdr-admin",
		"EditFilenameForm" : "assets/admin/cdr-admin",
		"RemoteStateChangeMonitor" : "assets/admin/cdr-admin",
		"ConfirmationDialog" : "assets/admin/cdr-admin",
		"AlertHandler" : "assets/admin/cdr-admin",
		"SearchMenu" : "assets/admin/cdr-admin",
		"ResultTableActionMenu" : "assets/admin/cdr-admin",
		"ResultTableView" : "assets/admin/cdr-admin",
		"ResultView" : "assets/admin/cdr-admin",
		"MoveDropLocation" : "assets/admin/cdr-admin",
		"CreateContainerForm" : "assets/admin/cdr-admin",
		"AbstractFileUploadForm" : "assets/admin/cdr-admin",
		"IngestPackageForm" : "assets/admin/cdr-admin",
		"CreateSimpleObjectForm" : "assets/admin/cdr-admin",
		"ResultObjectActionMenu" : "assets/admin/cdr-admin",
		"ActionEventHandler" : "assets/admin/cdr-admin",
		"AddMenu" : "assets/admin/cdr-admin",
		"contextMenu" : "js/admin/lib/jquery.contextMenu",
		"detachplus" : "assets/admin/cdr-admin",
		
		"StructureEntry" : "assets/admin/cdr-admin",
		"StructureView" : "assets/admin/cdr-admin",
		"URLUtilities" : "assets/admin/cdr-admin",
		"StringUtilities" : "assets/admin/cdr-admin",
		
		"editable" : "js/admin/lib/jqueryui-editable.min",
		"moment" : "assets/admin/cdr-admin"
	},
	shim: {
		"jquery-ui" : ["jquery"],
		"qtip" : ["jquery"],
		"contextMenu" : ["jquery", "jquery-ui"],
		"underscore": {
			exports: "_"
		}
	}
});

define("resultList", ["module", "jquery", "ResultView"], function(module, $) {

	$(".result_page").resultView({
		resultUrl : module.config().resultUrl,
		accessBaseUrl : module.config().accessBaseUrl,
		adminBaseUrl : module.config().adminBaseUrl,
		formsBaseUrl : module.config().formsBaseUrl
	});
});
