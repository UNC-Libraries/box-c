require.config({
	urlArgs: "v=3.4-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-admin',
		'jquery-ui' : 'cdr-admin',
		'text' : 'lib/text',
		'underscore' : 'lib/underscore',
		'tpl' : 'lib/tpl',
		'qtip' : 'lib/jquery.qtip.min',
		'PID' : 'cdr-admin',
		'AjaxCallbackButton' : 'cdr-admin',
		'ResultObject' : 'cdr-admin',
		'ResultObjectList' : 'cdr-admin',
		'ParentResultObject' : 'cdr-admin',
		'BatchCallbackButton' : 'cdr-admin',
		'UnpublishBatchButton' : 'cdr-admin',
		'PublishBatchButton' : 'cdr-admin',
		'DeleteBatchButton' : 'cdr-admin',
		'ModalLoadingOverlay' : 'cdr-admin',
		'EditAccessControlForm' : 'cdr-admin',
		'RemoteStateChangeMonitor' : 'cdr-admin',
		'ConfirmationDialog' : 'cdr-admin',
		'AlertHandler' : 'cdr-admin',
		'SearchMenu' : 'cdr-admin',
		'ResultTableActionMenu' : 'cdr-admin',
		'ResultTableView' : 'cdr-admin',
		'MoveDropLocation' : 'cdr-admin',
		'CreateContainerForm' : 'cdr-admin',
		'AbstractFileUploadForm' : 'cdr-admin',
		'IngestPackageForm' : 'cdr-admin',
		'CreateSimpleObjectForm' : 'cdr-admin',
		'ResultObjectActionMenu' : 'cdr-admin',
		'ActionEventHandler' : 'cdr-admin',
		'AddMenu' : 'cdr-admin',
		'contextMenu' : 'admin/lib/jquery.contextMenu',
		'detachplus' : 'cdr-admin',
		
		'StructureEntry' : 'cdr-admin',
		'StructureView' : 'cdr-admin',
		'URLUtilities' : 'cdr-admin',
		
		'editable' : 'admin/lib/jqueryui-editable.min',
		'moment' : 'cdr-admin'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'qtip' : ['jquery'],
		'contextMenu' : ['jquery', 'jquery-ui'],
		'underscore': {
			exports: '_'
		}
	}
});

define('resultList', ['module', 'jquery', "tpl!../templates/admin/resultTableHeader", "tpl!../templates/admin/navigationBar", "tpl!../templates/admin/pathTrail", 
	'AddMenu', 'ParentResultObject', 'URLUtilities', 'ActionEventHandler', 'AlertHandler', 'SearchMenu', 'ResultTableView'],
	function(module, $, resultTableHeaderTemplate, navigationBarTemplate, pathTrailTemplate, AddMenu, ParentResultObject, URLUtilities, ActionEventHandler) {
	//console.profile();
	var alertHandler = $("<div id='alertHandler'></div>");
	alertHandler.alertHandler().appendTo(document.body).hide();
	
	
	//var startTime = new Date();
	
	//console.log("Result table start: " + (new Date()).getTime());
	
	// Keep the result area locked to the size of the view port
	var $resultPage = $('.result_page');
	var $resultView;
	var $columnHeaders;
	var $containerEntry;
	var $tableActionMenu;
	var $resultHeader;
	var $resultTable;
	var $window = $(window);
	var menuOffset = 360;
	
	// Keep result area the right size when the menu or window is resized
	var resizeResults = function () {
		var wHeight = $window.height(), wWidth = $window.width();
		$resultPage.height(wHeight - 105);
		$resultView.height(wHeight - 105);
		$resultHeader.width(wWidth - menuOffset);
		$resultTable.width(wWidth - menuOffset);
		$columnHeaders.width(wWidth - menuOffset);
		$containerEntry.css('max-width', (wWidth - $tableActionMenu.width() - menuOffset - 105));
	};
	
	var pageNavigation = {
		resultUrl : module.config().resultUrl,
		filterParams : module.config().filterParams,
		pagingActive : module.config().pagingActive,
		pageStart : module.config().pageStart,
		pageRows : module.config().pageRows,
		resultCount : module.config().resultCount
	};
	
	var container = module.config().container? module.config().container : null;
	var navigationBar = navigationBarTemplate({pageNavigation : pageNavigation, container : container, URLUtilities : URLUtilities});
	var containerPath = null;
	if (container)
		containerPath = pathTrailTemplate({ancestorPath : container.ancestorPath, queryMethod : 'list', filterParams : module.config().filterParams, skipLast : true});
	var resultTableHeader = resultTableHeaderTemplate({container : container, navigationBar : navigationBar, containerPath : containerPath})
	
	
	var actionHandler = new ActionEventHandler();
	
	function postRender(resultTable) {
		$resultView = $('#result_view');
		$columnHeaders = $('.column_headers');
		$resultHeader = $('.result_header');
		$resultTable = $('.result_table');
		$containerEntry = $('.container_header > span > h2');
		$tableActionMenu = $('.result_table_action_menu');
		
		// Keep result area the right size when the menu is resized
		var searchMenu = $("#search_menu").searchMenu({
			filterParams : module.config().filterParams,
			resultTableView : $(".result_area > div"),
			selectedId : module.config().container && /\w+\/uuid:[0-9a-f\-]+($|\?)/.test(document.URL)? module.config().container.id : false,
		}).on("resize", function(){
			menuOffset = searchMenu.position().left + searchMenu.innerWidth() + 40;
			resizeResults.call();
		});
		
		if (container) {
			var containerObject = new ParentResultObject({metadata : container, 
					element : $(".container_entry")});
		
			new AddMenu({
				container : container,
				selector : "#add_menu",
				alertHandler : alertHandler
			});
		}
		
		resizeResults.call();
		$window.resize(resizeResults);
	}
	
	$(".result_area > div").resultTableView({
		metadataObjects : module.config().metadataObjects,
		container : module.config().container,
		alertHandler : alertHandler,
		resultUrl : module.config().resultUrl,
		resultFields : {
			"select" : {name : "", colClass : "narrow", dataType : "index", sortField : "collection"},
			"resourceType" : {name : "", colClass : "narrow", sortField : "resourceType"},
			"title" : {name : "Title", colClass : "itemdetails", dataType : "title", sortField : "title"},
			"creator" : {name : "Creator", colClass : "creator", sortField : "creator"},
			"dateAdded" : {name : "Added", colClass : "date_added", sortField : "dateAdded"},
			"dateModified" : {name : "Modified", colClass : "date_added", sortField : "dateUpdated"},
			"actionMenu" : {name : "", colClass : "narrow"}
		},
		resultHeader : resultTableHeader,
		postRender : postRender,
		postInit : resizeResults,
		actionHandler : actionHandler,
		resultActions : {
			1 : ['restoreBatch', 'deleteBatch'],
			2 : ['publish', 'unpublish']/*,
			'more' : ['reindex']*/
		}
	});
	
	//console.profileEnd();
	//console.log("Result table finish " + (new Date() - startTime));
});
