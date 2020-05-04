require.config({
	urlArgs: "v=4.0-SNAPSHOT",
	baseUrl: "/static/js/",
	paths: {
		"jquery" : "cdr-admin",
		"jquery-ui" : "cdr-admin",
		"text" : "lib/text",
		"underscore" : "lib/underscore",
		"tpl" : "lib/tpl",
		"autosize" : "xmleditor/lib/jquery.autosize-min",
		"json2" : "xmleditor/lib/json2",
		"cycle" : "xmleditor/lib/cycle",
		"ace" : "xmleditor/lib/ace/src-min/ace",
		"vkbeautify" : "xmleditor/lib/vkbeautify",
		"xmleditor" : "xmleditor/jquery.xmleditor"
	},
	shim: {
		"ace" : ["jquery"],
		"autosize" : ["jquery"],
		"xmleditor" : ["jquery-ui", "text", "autosize", "json2", "cycle", "ace", "vkbeautify"],
		"underscore": {
			exports: "_"
		}
	}
});

define("editDescription", ["module", "jquery", "jquery-ui", "ace", "xmleditor", "tpl!../templates/admin/pathTrail"], function(module, $, ui, ace, xmleditor, pathTemplate) {
	
	var pid = window.location.pathname;
	pid = pid.substring(pid.lastIndexOf("/") + 1);

	var loadingIcon = $("#loading-icon");
	loadingIcon.removeClass("hidden");
	
	var fields = "id,title,path,contentStatus,type";
	$.getJSON("/services/api/record/" + pid + "?fields=" + fields, function(data) {
		var resultObject = data;
		
		var containerPath = pathTemplate({
			objectPath : resultObject.objectPath? resultObject.objectPath : [],
			queryMethod : 'list',
			filterParams : "",
			skipLast : false
		});
		
		$(".results_header_hierarchy_path").html(containerPath);
	
		var originalUrl = module.config().originalUrl;
		var recordUrl = module.config().recordUrl;
		var menuEntries = [{
			insertPath : ["View"],
			label : "View in DCR",
			enabled : true,
			binding : null,
			action : recordUrl
		}, {
			label : "View in DCR",
			enabled : true, 
			itemClass : "header_menu_link",
			action : recordUrl
		}, {
			label: "View History",
			enabled: true,
			itemClass: "header_menu_link",
			action: "/services/api/file/" + pid + "/md_descriptive_history"
		}];
		if ((data.type === 'File' || data.type === 'Work') && originalUrl && originalUrl.length > 1) {
			menuEntries.push({
				insertPath : ["View"],
				label : "View original file",
				enabled : true,
				binding : null,
				action : originalUrl
			});
			menuEntries.push({
				label : "View File",
				enabled : true, 
				itemClass : "header_menu_link",
				action : originalUrl
			});
		}
		
		var modsRetrievalPath;
		if ($.inArray('Described', resultObject.contentStatus) != -1) {
			modsRetrievalPath = "/services/api/description/" + pid;
		}
		
		var editorOptions = {
			schema : "../../static/schemas/mods-3-7.json",
			ajaxOptions : {
				xmlRetrievalPath : modsRetrievalPath,
				xmlUploadPath : "/services/api/edit/description/" + pid
			},
			templateOptions : {
				templatePath: '../../static/js/xmleditor/templates/',
				templates: [
					{
						filename: 'mods.xml',
						title: "Blank",
						description: 'An empty MODS document',
						icon_class: 'fa fa-file-o'
					},
					{
						filename: 'generic.xml',
						title: "Generic Object",
						description: 'Generic MODS template prepopulated with common fields',
						icon_class: 'fa fa-file-text-o'
					},
					{
						filename: 'minimal_metadata.xml',
						title: "Minimal Metadata",
						description: 'Simplified MODS template with basic fields',
						icon_class: 'fa fa-file-text-o'
					},
					{
						filename: 'archives.xml',
						title: 'Archival Folder',
						description: 'Wilson MODS template with common fields',
						icon_class: 'fa fa-archive'
					},
					{
						filename: 'serials.xml',
						title: "Serial Title",
						description: 'Template with standard fields to describe serial titles',
						icon_class: 'fa fa-newspaper-o'
					}
				],
				cancelFunction: function() {
					var parentId = "";
					if (resultObject.ancestorPath) {
						parentId = resultObject.ancestorPath[resultObject.ancestorPath.length - 1];
						parentId = parentId.id;
					}
					window.location.href = "/admin/list/" + parentId;
				}
			},
			libPath : "../../static/js/xmleditor/lib/",
			menuEntries: menuEntries,
			enforceOccurs: false,
			xmlEditorLabel : "Form",
			textEditorLabel : "XML",
			submitResponseHandler : function(response) {
				var json;
				if (typeof response == 'object') {
					json = response;
				} else {
					json = JSON.parse(response);
				}
				return json.hasOwnProperty('error');
			},
			submitErrorHandler : function(jqXHR, exception) {
				var error_msg;
				try {
					var error_json = JSON.parse(jqXHR.responseText);
					error_msg = error_json['error'];
				} catch (e) {
					alert("An unexpected error occurred while updating the description, please contact administrators.");
					return;
				}
				
				if (error_msg) {
					alert(error_msg);
				} else if (jqXHR.status === 0) {
					alert('Could not connect.\n Verify Network.');
				} else if (jqXHR.status == 404) {
					alert('Requested page not found. [404]');
				} else if (jqXHR.status == 500) {
					alert('Internal Server Error [500].');
				} else if (exception === 'parsererror') {
					alert('Requested JSON parse failed.');
				} else if (exception === 'timeout') {
					alert('Time out error.');
				} else if (exception === 'abort') {
					alert('Ajax request aborted.');
				} else {
					alert('Uncaught Error.\n' + jqXHR.responseText);
				}
			}
		};

		loadingIcon.addClass("hidden");

		$("#xml_editor").xmlEditor(editorOptions);
		
		$(window).resize();
	});
});
