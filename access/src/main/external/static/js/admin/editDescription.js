require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'adminCommon' : 'admin/adminCommon',
		'PID' : 'admin/src/PID',
		'text' : 'modseditor/lib/text',
		'jquery-xmlns' : 'modseditor/lib/jquery.xmlns',
		'expanding' : 'expanding',
		'json2' : 'modseditor/lib/json2',
		'cycle' : 'modseditor/lib/cycle',
		'ace' : 'modseditor/lib/ace/src-min/ace',
		'vkbeautify' : 'modseditor/lib/vkbeautify.0.98.01.beta',
		'mods-schema' : '/static/schemas/mods-3-4/mods-3-4',
		'modseditor' : 'modseditor/jquery.modseditor'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'qtip' : ['jquery'],
		'adminCommon' : ['jquery'],
		'jquery-xmlns' : ['jquery'],
		'mods-schema' : {
			exports : 'Mods'
		},
		'modseditor' : ['jquery', 'jquery-xmlns', 'text', 'expanding', 'json2', 'cycle', 'ace', 'vkbeautify', 'mods-schema']
	}
});

define('editDescription', ['module', 'jquery', 'jquery-ui', 'ace', 'PID', 'mods-schema', 'modseditor'], function(module, $, ui, ace, PID, Mods) {
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
	
	$("#mods_editor").modsEditor({
		schemaObject : Mods,
		ajaxOptions : {
			modsRetrievalPath : "/admin/" + pid.getPath() + "/mods",
			modsRetrievalParams : {'pid' : pid.getPid()},
			modsUploadPath : "/admin/describe/" + pid.getPath()
		},
		'menuEntries': menuEntries
	});
	$(window).resize();
});