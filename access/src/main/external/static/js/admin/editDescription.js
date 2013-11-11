require.config({
	urlArgs: "v=4.3.1",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-admin',
		'jquery-ui' : 'cdr-admin',
		'text' : 'xmleditor/lib/text',
		'autosize' : 'xmleditor/lib/jquery.autosize-min',
		'json2' : 'xmleditor/lib/json2',
		'cycle' : 'xmleditor/lib/cycle',
		'ace' : 'xmleditor/lib/ace/src-min/ace',
		'vkbeautify' : 'xmleditor/lib/vkbeautify',
		'xmleditor' : 'xmleditor/jquery.xmleditor'
	},
	shim: {
		'ace' : ['jquery'],
		'autosize' : ['jquery'],
		'xmleditor' : ['jquery-ui', 'text', 'autosize', 'json2', 'cycle', 'ace', 'vkbeautify']
	}
});

define('editDescription', ['module', 'jquery', 'jquery-ui', 'ace', 'xmleditor'], function(module, $, ui, ace) {
	var resultObject = module.config().resultObject;
	var originalUrl = module.config().originalUrl;
	var menuEntries = (originalUrl)? [{
		insertPath : ["View"],
		label : 'View original document',
		enabled : true,
		binding : null,
		action : originalUrl
	}, {
		label : 'View Document',
		enabled : true, 
		itemClass : 'header_mode_tab',
		action : originalUrl
	}]: null;
	
	$("#xml_editor").xmlEditor({
		schema : "../../static/schemas/mods-3-4/mods.json",
		ajaxOptions : {
			xmlRetrievalPath : "/admin/" + resultObject.id + "/mods",
			xmlUploadPath : "/admin/describe/" + resultObject.id
		},
		libPath : "../../static/js/xmleditor/lib/",
		menuEntries: menuEntries
	});
	$(window).resize();
});
