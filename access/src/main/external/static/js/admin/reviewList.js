require.config({
	urlArgs: "v=3.4-SNAPSHOT",
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
		'DestroyBatchButton' : 'admin/src/DestroyBatchButton'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'jquery.preload' : ['jquery'],
		'thumbnail' : ['jquery'],
		'qtip' : ['jquery'],
		'adminCommon' : ['jquery']
	}
});

define('reviewList', ['module', 'jquery', 'ResultObjectList', 'PublishBatchButton', 'UnpublishBatchButton', 'DestroyBatchButton'], function(module, $, ResultObjectList) {
	$("#select_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", true);
		$(".browseitem").addClass("selected");
	});
	
	$("#deselect_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", false);
		$(".browseitem").removeClass("selected");
	});
	
	var resultObjectList = new ResultObjectList({'metadataObjects' : module.config().metadataObjects});
	
	
	$("#publish_selected").publishBatchButton({'resultObjectList' : resultObjectList});
	$("#unpublish_selected").unpublishBatchButton({'resultObjectList' : resultObjectList});
	$("#delete_selected").deleteBatchButton({'resultObjectList' : resultObjectList});
});
