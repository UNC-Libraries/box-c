require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'PID' : 'admin/src/PID',
		'text' : 'xmleditor/lib/text',
		'jquery-xmlns' : 'xmleditor/lib/jquery.xmlns',
		'expanding' : 'expanding',
		'json2' : 'xmleditor/lib/json2',
		'cycle' : 'xmleditor/lib/cycle',
		'ace' : 'xmleditor/lib/ace/src-min/ace',
		'vkbeautify' : 'xmleditor/lib/vkbeautify',
		'mods-schema' : '/static/schemas/mods-3-4/mods-3-4',
		'xmleditor' : 'xmleditor/jquery.xmleditor'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'qtip' : ['jquery'],
		'adminCommon' : ['jquery'],
		'jquery-xmlns' : ['jquery'],
		'mods-schema' : {
			exports : 'Mods'
		},
		'ace' : ['jquery'],
		'xmleditor' : ['jquery', 'jquery-xmlns', 'text', 'expanding', 'json2', 'cycle', 'ace', 'vkbeautify', 'mods-schema']
	}
});

define('editDescription', ['module', 'jquery', 'jquery-ui', 'ace', 'PID', 'mods-schema', 'xmleditor'], function(module, $, ui, ace, PID, Mods) {
	var resultObject = module.config().resultObject;
	var originalUrl = module.config().originalUrl;
	
	var pid = new PID(resultObject.id);
	
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
		schemaObject : Mods,
		ajaxOptions : {
			xmlRetrievalPath : "/admin/" + pid.getPath() + "/mods",
			xmlRetrievalParams : {'pid' : pid.getPid()},
			xmlUploadPath : "/admin/describe/" + pid.getPath()
		},
		'menuEntries': menuEntries
	});
	$(window).resize();
});