require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui',
		'qtip' : 'jquery.qtip.min',
		'PID' : 'squish',
		'MetadataObject' : 'squish',
		'AjaxCallbackButton' : 'squish',
		'PublishObjectButton' : 'squish',
		'DeleteObjectButton' : 'squish',
		'ResultObject' : 'squish',
		'ResultObjectList' : 'squish',
		'BatchCallbackButton' : 'squish',
		'UnpublishBatchButton' : 'squish',
		'PublishBatchButton' : 'squish',
		'DeleteBatchButton' : 'squish',
		'ModalLoadingOverlay' : 'squish',
		'EditAccessControlForm' : 'squish',
		'RemoteStateChangeMonitor' : 'squish',
		'ConfirmationDialog' : 'squish',
		'AlertHandler' : 'squish',
		'SearchMenu' : 'squish',
		'ResultTableView' : 'squish',
		'detachplus' : 'admin/lib/jquery.detachplus',
		
		'StructureEntry' : 'squish',
		'StructureView' : 'squish',
		'URLUtilities' : 'squish',
		
		'editable' : 'jqueryui-editable.min',
		'moment' : 'moment.min'
			
			/*
			 * 
			 * 'PID' : 'squish',
		'MetadataObject' : 'squish',
		'AjaxCallbackButton' : 'squish',
		'PublishObjectButton' : 'squish',
		'DeleteObjectButton' : 'squish',
		'ResultObject' : 'squish',
		'ResultObjectList' : 'squish',
		'BatchCallbackButton' : 'squish',
		'UnpublishBatchButton' : 'squish',
		'PublishBatchButton' : 'squish',
		'DeleteBatchButton' : 'squish',
		'ModalLoadingOverlay' : 'squish',
		'EditAccessControlForm' : 'squish',
		'RemoteStateChangeMonitor' : 'squish',
		'ConfirmationDialog' : 'squish',
		'AlertHandler' : 'squish',
		'SearchMenu' : 'squish',
		'ResultTableView' : 'squish',
			 */
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

define('resultList', ['module', 'jquery', 'AlertHandler', 'ResultTableView', 'SearchMenu'], function(module, $) {
	//console.profile();
	var alertHandler = $("<div id='alertHandler'></div>");
	alertHandler.alertHandler().appendTo(document.body).hide();
	
	$("#search_menu").searchMenu();
	
	//var startTime = new Date();
	
	//console.log("Result table start: " + (new Date()).getTime());
	
	$(".result_table").resultTableView({
		'metadataObjects' : module.config().metadataObjects,
		'resultUrl' : module.config().resultUrl,
		'pagingActive' : module.config().pagingActive,
	});
	//console.profileEnd();
	//console.log("Result table finish " + (new Date() - startTime));
});