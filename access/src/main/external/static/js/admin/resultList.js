require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui',
		'text' : 'text',
		'underscore' : 'underscore',
		'tpl' : 'tpl',
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
		'CreateContainerForm' : 'squish',
		'AbstractFileUploadForm' : 'squish',
		'IngestPackageForm' : 'squish',
		'CreateSimpleObjectForm' : 'squish',
		'ResultObjectActionMenu' : 'squish',
		'AddMenu' : 'squish',
		'contextMenu' : 'jquery.contextMenu',
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
		'editable' : ['jquery'],
		'fileupload' : ['jquery'],
		'contextMenu' : ['jquery', 'jquery-ui'],
		'underscore': {
			exports: '_'
		}
	}
});

define('resultList', ['module', 'jquery', 'AlertHandler', 'ResultTableView', 'SearchMenu'], function(module, $) {
	//console.profile();
	var alertHandler = $("<div id='alertHandler'></div>");
	alertHandler.alertHandler().appendTo(document.body).hide();
	
	
	//var startTime = new Date();
	
	//console.log("Result table start: " + (new Date()).getTime());
	
	// Keep the result area locked to the size of the view port
	var $resultPage = $('.result_page');
	var $resultView = $('#result_view');
	var $columnHeaders = $('.column_headers');
	var $resultHeader = $('.result_header');
	var $resultTable = $('.result_table');
	var $window = $(window);
	var menuOffset = 360;
	var resizeResults = function () {
		var wHeight = $window.height(), wWidth = $window.width();
		$resultPage.height(wHeight - 105);
		$resultView.height(wHeight - 105);
		$resultHeader.width(wWidth - menuOffset);
		$resultTable.width(wWidth - menuOffset);
		$columnHeaders.width(wWidth - menuOffset);
	};
	resizeResults.call();
	$window.resize(resizeResults);
	
	// Keep result area the right size when the menu is resized
	var searchMenu = $("#search_menu").searchMenu({
		filterParams : module.config().filterParams
	}).on("resize", function(){
		menuOffset = searchMenu.position().left + searchMenu.innerWidth() + 40;
		resizeResults.call();
	});
	
	$("#result_view").resultTableView({
		'metadataObjects' : module.config().metadataObjects,
		'resultUrl' : module.config().resultUrl,
		'pagingActive' : module.config().pagingActive,
		'container' : module.config().container,
		alertHandler : alertHandler
	});
	
	//console.profileEnd();
	//console.log("Result table finish " + (new Date() - startTime));
});