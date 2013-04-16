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

define('resultList', ['module', 'jquery', 'ResultObjectList', 'AlertHandler', 'PublishBatchButton', 'UnpublishBatchButton', 
                      'DeleteBatchButton', 'EditAccessControlForm'], function(module, $, ResultObjectList) {
	var alertHandler = $("<div id='alertHandler'></div>");
	alertHandler.alertHandler().appendTo(document.body).hide();
	//alertHandler.alertHandler('addMessage', "hello world");
	
	$("#select_all").click(function(){
		$(".result_table .entry").resultObject('select');
	});
	
	$("#deselect_all").click(function(){
		$(".result_table .entry").resultObject('unselect');
	});
	
	var resultObjectList = new ResultObjectList({'metadataObjects' : module.config().metadataObjects});
	
	
	$("#publish_selected").publishBatchButton({
		'resultObjectList' : resultObjectList, 
		'workFunction' : function() {
				this.resultObject('setStatusText', 'Publishing...');
				this.resultObject('updateOverlay', 'show');
			}, 
		'followupFunction' : function() {
			this.resultObject('setStatusText', 'Publishing....');
		}, 
		'completeFunction' : function(){
			this.resultObject('refresh', true);
		}
	});
	$("#unpublish_selected").unpublishBatchButton({
		'resultObjectList' : resultObjectList, 
		'workFunction' : function() {
			this.resultObject('setStatusText', 'Unpublishing...');
			this.resultObject('updateOverlay', 'show');
			}, 
		'followupFunction' : function() {
			this.resultObject('setStatusText', 'Unpublishing....');
		}, 
		'completeFunction' : function(){
			this.resultObject('refresh', true);
		}
	});
	$("#delete_selected").deleteBatchButton({
		'resultObjectList' : resultObjectList, 
		'workFunction' : function() {
			this.resultObject('setStatusText', 'Deleting...');
			this.resultObject('updateOverlay', 'show');
			}, 
		'followupFunction' : function() {
			this.resultObject('setStatusText', 'Cleaning up...');
		}, 
		'completeFunction' : 'deleteElement'
	});
});