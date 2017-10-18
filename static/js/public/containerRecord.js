require.config({
	urlArgs: "v=4.0-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access',
		'text' : 'lib/text',
		'tpl' : 'lib/tpl',
		'underscore' : 'lib/underscore',
		'StructureEntry' : 'cdr-access',
		'StructureView' : 'cdr-access'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'underscore': {
			exports: '_'
		}
	}
});
define('containerRecord', ['module', 'jquery', 'StructureView', 'jquery-ui'], function(module, $) {
	var $structureView = $(".structure");
	
	if ($structureView.length > 0) {
		$.ajax({
			url: "/structure/" + $structureView.attr('data-pid') + "/json?files=true",
			dataType : 'json',
			success: function(data){
				$structureView.structureView({
					showResourceIcons : true,
					showParentLink : false,
					rootNode : data.root,
					queryPath : 'list',
					retrieveFiles : true
				});
			},
			error: function(e){
				console.log("Failed to load", e);
			}
		});
	}
	
	var containerSettings = module.config().containerSettings;
	
	function selectTab(tabs, tabid) {
		var tabHeader;
		if (!tabid) {
			tabHeader = $(".tab_headers li:first", tabs);
			tabid = tabHeader.data("tabid");
		} else {
			tabHeader = $(".tab_headers li[data-tabid = '" + tabid + "']", tabs);
			if (tabHeader.length == 0) {
				tabHeader = $(".tab_headers li:first", tabs);
				tabid = tabHeader.data("tabid");
			}
		}
		tabHeader.addClass("selected").siblings().removeClass("selected");
		collectionTabs.children("div[data-tabid != '" + tabid + "']").hide();
		collectionTabs.children("div[data-tabid = '" + tabid + "']").show();
	}
	
	var collectionTabs = $("#collection_tabs");
	collectionTabs.on("click", ".tab_headers li, .tab_headers li a", function(e) {
		var tabid = $(this).closest("li").data("tabid");
		selectTab(collectionTabs, tabid);
		e.preventDefault();
	});
	selectTab(collectionTabs, containerSettings.defaultView);
});
