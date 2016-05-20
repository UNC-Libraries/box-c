require.config({
	urlArgs: "v=4.0-SNAPSHOT",
	baseUrl: "/static/js/",
	paths: {
		"jquery" : "cdr-admin",
		"jquery-ui" : "cdr-admin",
		"text" : "lib/text",
		"underscore" : "lib/underscore",
		"tpl" : "lib/tpl",
		"qtip" : "lib/jquery.qtip.min",
		"PID" : "cdr-admin",
		"AjaxCallbackButton" : "cdr-admin",
		"ResultObject" : "cdr-admin",
		"ResultObjectList" : "cdr-admin",
		"ParentResultObject" : "cdr-admin",
		"BatchCallbackButton" : "cdr-admin",
		"UnpublishBatchButton" : "cdr-admin",
		"PublishBatchButton" : "cdr-admin",
		"DestroyBatchButton" : "cdr-admin",
		"ModalLoadingOverlay" : "cdr-admin",
		"EditAccessControlForm" : "cdr-admin",
		"RemoteStateChangeMonitor" : "cdr-admin",
		"ConfirmationDialog" : "cdr-admin",
		"AlertHandler" : "cdr-admin",
		"SearchMenu" : "cdr-admin",
		"ResultTableActionMenu" : "cdr-admin",
		"ResultTableView" : "cdr-admin",
		"ResultView" : "cdr-admin",
		"MoveDropLocation" : "cdr-admin",
		"CreateContainerForm" : "cdr-admin",
		"AbstractFileUploadForm" : "cdr-admin",
		"IngestPackageForm" : "cdr-admin",
		"CreateSimpleObjectForm" : "cdr-admin",
		"ResultObjectActionMenu" : "cdr-admin",
		"ActionEventHandler" : "cdr-admin",
		"AddMenu" : "cdr-admin",
		"contextMenu" : "admin/lib/jquery.contextMenu",
		"detachplus" : "cdr-admin",
		
		"StructureEntry" : "cdr-admin",
		"StructureView" : "cdr-admin",
		"URLUtilities" : "cdr-admin",
		"StringUtilities" : "cdr-admin",
		
		"editable" : "admin/lib/jqueryui-editable.min",
		"moment" : "cdr-admin"
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

define("reviewList", ["module", "jquery", "ResultView", "qtip"], function(module, $) {

	$(".result_page").resultView({
		metadataObjects : module.config().metadataObjects,
		container : module.config().container,
		containerPath : module.config().containerPath,
		resultUrl : module.config().resultUrl,
		filterParams : module.config().filterParams,
		queryPath : "review",
		pagingActive : module.config().pagingActive,
		pageStart : module.config().pageStart,
		pageRows : module.config().pageRows,
		resultCount : module.config().resultCount,
		resultTableHeaderTemplate : "tpl!../templates/admin/reviewTableHeader",
		invalidVocabCount : module.config().invalidVocabCount
	});
});
