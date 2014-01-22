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
		'BatchCallbackButton' : 'cdr-admin',
		'DestroyBatchButton' : 'cdr-admin',
		'ModalLoadingOverlay' : 'cdr-admin',
		'RemoteStateChangeMonitor' : 'cdr-admin',
		'ConfirmationDialog' : 'cdr-admin',
		'AlertHandler' : 'cdr-admin',
		'SearchMenu' : 'cdr-admin',
		'ResultTableActionMenu' : 'cdr-admin',
		'ResultTableView' : 'cdr-admin',
		'MoveDropLocation' : 'cdr-admin',
		'ResultObjectActionMenu' : 'cdr-admin',
		'ActionEventHandler' : 'cdr-admin',
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

define('trashList', ['module', 'jquery', "tpl!../templates/admin/trashTableHeader", "tpl!../templates/admin/navigationBar",
	'ActionEventHandler', 'AlertHandler', 'SearchMenu', 'ResultTableView'],
	function(module, $, resultTableHeaderTemplate, navigationBarTemplate, ActionEventHandler) {
	//console.profile();
	var alertHandler = $("<div id='alertHandler'></div>");
	alertHandler.alertHandler().appendTo(document.body).hide();
	
	var $resultPage = $('.result_page');
	var $resultView;
	var $columnHeaders;
	var $containerEntry;
	var $tableActionMenu;
	var $resultHeader;
	var $resultTable;
	var $window = $(window);
	var menuOffset = 360;
	
	var resizeResults = function () {
		var wHeight = $window.height(), wWidth = $window.width();
		$resultPage.height(wHeight - 105);
		$resultView.height(wHeight - 105);
		$resultHeader.width(wWidth - menuOffset);
		$resultTable.width(wWidth - menuOffset);
		$columnHeaders.width(wWidth - menuOffset);
		$containerEntry.css('max-width', (wWidth - $tableActionMenu.width() - menuOffset - 105));
	};
	
	//var startTime = new Date();
	
	//console.log("Result table start: " + (new Date()).getTime());
	
	// Keep the result area locked to the size of the view port
	
	// Keep result area the right size when the menu is resized
	
	var pageNavigation = {
		resultUrl : module.config().resultUrl,
		filterParams : module.config().filterParams,
		pagingActive : module.config().pagingActive,
		pageStart : module.config().pageStart,
		pageRows : module.config().pageRows,
		resultCount : module.config().resultCount
	};
	
	var container = module.config().container? module.config().container : null;
	var navigationBar = navigationBarTemplate({pageNavigation : pageNavigation, container : container});
	var resultTableHeader = resultTableHeaderTemplate({container : container, navigationBar : navigationBar})
	
	var actionHandler = new ActionEventHandler();
	
	function postRender(resultTable) {
		$resultView = $('#result_view');
		$columnHeaders = $('.column_headers');
		$resultHeader = $('.result_header');
		$resultTable = $('.result_table');
		$containerEntry = $('.container_header > span > h2');
		$tableActionMenu = $('.result_table_action_menu');
		
		var searchMenu = $("#search_menu").searchMenu({
			filterParams : module.config().filterParams,
			resultTableView : $(".result_area > div"),
			selectedId : module.config().container && /\w+\/uuid:[0-9a-f\-]+($|\?)/.test(document.URL)? module.config().container.id : false,
			queryPath : 'trash'
		}).on("resize", function(){
			menuOffset = searchMenu.position().left + searchMenu.innerWidth() + 40;
			resizeResults.call();
		});
		
		resizeResults.call();
		$window.resize(resizeResults);
	}
	
	$(".result_area > div").resultTableView({
		metadataObjects : module.config().metadataObjects,
		container : module.config().container,
		alertHandler : alertHandler,
		resultUrl : module.config().resultUrl,
		headerHeightClass : 'trash_header',
		resultFields : {
			"select" : {name : "", colClass : "narrow", dataType : "index", sortField : "collection"},
			"resourceType" : {name : "", colClass : "narrow", sortField : "resourceType"},
			"title" : {name : "Title", colClass : "itemdetails", dataType : "title", sortField : "title"},
			"originalPath" : {name : "Original Path", colClass : "original_path", sortField : "ancestorNames"},
			"dateModified" : {name : "Modified", colClass : "date_added", sortField : "dateUpdated"},
		},
		resultEntryTemplate : "tpl!../templates/admin/trashResultEntry",
		resultHeader : resultTableHeader,
		postRender : postRender,
		actionHandler : actionHandler,
		resultActions : [
			{
				actions : [
					{action : 'RestoreBatch', label : 'Restore'}
				]
			}, {
				actions : [
					{action : 'DestroyBatch', label : 'Destroy'}
				]
			}
		]
	});
	
	//console.profileEnd();
	//console.log("Result table finish " + (new Date() - startTime));
});
