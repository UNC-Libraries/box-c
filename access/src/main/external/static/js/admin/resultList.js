require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'jquery.preload': 'jquery.preload-1.0.8-unc',
		'thumbnail' : 'thumbnail',
		'adminCommon' : 'admin/adminCommon',
		'PID' : 'admin/src/PID',
		'MetadataObject' : 'admin/src/MetadataObject',
		'AjaxCallbackButton' : 'admin/src/AjaxCallbackButton',
		'PublishObjectButton' : 'admin/src/PublishObjectButton',
		'DeleteObjectButton' : 'admin/src/DeleteObjectButton',
		'ResultObject' : 'admin/src/ResultObject',
		'ResultObjectList' : 'admin/src/ResultObjectList',
		'BatchCallbackButton' : 'admin/src/BatchCallbackButton',
		'UnpublishBatchButton' : 'admin/src/UnpublishBatchButton',
		'PublishBatchButton' : 'admin/src/PublishBatchButton',
		'DeleteBatchButton' : 'admin/src/DeleteBatchButton',
		'ModalLoadingOverlay' : 'admin/src/ModalLoadingOverlay',
		'EditAccessControlForm' : 'admin/src/EditAccessControlForm',
		'RemoteStateChangeMonitor' : 'admin/src/RemoteStateChangeMonitor',
		'ConfirmationDialog' : 'admin/src/ConfirmationDialog',
		'AlertHandler' : 'admin/src/AlertHandler',
		'ResizableAcccordionMenu' : 'admin/src/ResizableAccordionMenu',
		'sortElements' : 'admin/lib/jquery.sortElements',
		'ResultTableView' : 'admin/src/ResultTableView',
		'editable' : 'jqueryui-editable.min',
		'moment' : 'moment.min'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'jquery.preload' : ['jquery'],
		'thumbnail' : ['jquery'],
		'qtip' : ['jquery'],
		'adminCommon' : ['jquery'],
		'editable' : ['jquery']
	}
});

define('resultList', ['module', 'jquery', 'AlertHandler', 'ResultTableView'], function(module, $) {
	var alertHandler = $("<div id='alertHandler'></div>");
	alertHandler.alertHandler().appendTo(document.body).hide();
	
	$("#select_all").click(function(){
		$(".result_table .entry").resultObject('select');
	});
	
	$("#deselect_all").click(function(){
		$(".result_table .entry").resultObject('unselect');
	});
	
	/*$("#search_menu").resizableAccordionMenu({
		alsoResize : '#facet_field_path_structure'
	});*/
	
	$(".result_table").resultTableView({
		'metadataObjects' : module.config().metadataObjects
	});
});