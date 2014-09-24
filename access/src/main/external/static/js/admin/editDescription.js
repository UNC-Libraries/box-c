require.config({
	urlArgs: "v=3.4-SNAPSHOT",
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
	
	var tags = resultObject.tags;
	if (tags) {
		var validationProblem = "";
		for (var index in tags) {
			if (tags[index].label == "invalid affiliation") {
				var details = tags[index].details;
				for (var index in details) {
					validationProblem +=  "<li><span class='warning_symbol'>!</span>" + details[index] + "</li>";
				}
			}
		}
		
		if (validationProblem) {
			$(".edit_desc_page .results_header_hierarchy_path").after("<div id='vocab_issues'><h3>Invalid affiliation terms</h3><ul>" + validationProblem + "</ul></div>");
		}
	}
	
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
