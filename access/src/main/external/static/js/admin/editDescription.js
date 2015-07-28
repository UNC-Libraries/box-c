require.config({
	urlArgs: "v=3.4-SNAPSHOT",
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
	
	$.getJSON("describeInfo/" + pid, function(data) {
		var resultObject = data.resultObject;
		var vocabTerms = data.vocabTerms;
		
		var containerPath = pathTemplate({
			objectPath : resultObject.objectPath.entries,
			queryMethod : 'list',
			filterParams : "",
			skipLast : false
		});
		
		$(".results_header_hierarchy_path").html(containerPath);
	
		var tags = resultObject.tags;
		if (tags) {
			var validationProblem = "";
			var vocabMap = {};
			for (var index in tags) {
				if (tags[index].label == "invalid term") {
					var details = tags[index].details;
					for (var detailIndex in details) {
						var detail = details[detailIndex]
						var vocabName = detail.substring(0, detail.indexOf("|"));
						var term = detail.substring(detail.indexOf("|") + 1);
						
						if (!(vocabName in vocabMap)) {
							vocabMap[vocabName] = "";
						}
						
						vocabMap[vocabName] +=  "<li><span class='warning_symbol'>!</span>" + term + "</li>";
					}
					break;
				}
			}
		
			if (!$.isEmptyObject(vocabMap)) {
				$.each(vocabMap, function(vocabName, value){
					$(".edit_desc_page .results_header_hierarchy_path").after("<div class='result_warning'><h3>Invalid " + vocabName + " terms</h3><ul>" + value + "</ul></div>");
				});
				
				if (resultObject.parentCollection) {
					$(".result_warning").append("<p>View all <a href='/admin/invalidVocab/" 
							+ resultObject.parentCollection + "'>invalid vocabulary terms</a> in the same collection</p>");
				} else {
					$(".result_warning").append("<p>View all <a href='/admin/invalidVocab/'>invalid vocabulary terms</a></p>");
				}
				
			}
		}
	
		var originalUrl = module.config().originalUrl;
		var recordUrl = module.config().recordUrl;
		var menuEntries = [{
			insertPath : ["View"],
			label : "View in CDR",
			enabled : true,
			binding : null,
			action : recordUrl
		}, {
			label : "View in CDR",
			enabled : true, 
			itemClass : "header_menu_link",
			action : recordUrl
		}];
		if (originalUrl && originalUrl.length > 1){
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
		
		var editorOptions = {
			schema : "../../static/schemas/mods-3-5.json",
			ajaxOptions : {
				xmlRetrievalPath : "/admin/" + resultObject.id + "/mods",
				xmlUploadPath : "/admin/describe/" + resultObject.id
			},
			templateOptions : {
				templatePath: '../../static/js/xmleditor/templates/',
				templates: [
					{ filename: 'mods.xml', title: "Blank", description: 'An empty MODS document', icon_class: 'fa fa-file-text-o' }
				]
			},
			libPath : "../../static/js/xmleditor/lib/",
			menuEntries: menuEntries,
			enforceOccurs: false,
			xmlEditorLabel : "Form",
			textEditorLabel : "XML",
			submitErrorHandler : function(jqXHR, exception) {
				var error_response,
					error_full_text,
					schematron_error_text,
					schematron_text,
					schematron_array;
			
				var error_response_msg = function(error_msg) {
					// Remove leading SaxParser error boilerplate
					var error_msg_array = error_msg[0].split(':');
					var user_msg = error_msg_array.slice(1, error_msg_array.length);
					
					alert($.trim(user_msg.join(' ')));
				};
				
				if (jqXHR.status === 0) {
					alert('Could not connect.\n Verify Network.');
				} else if (jqXHR.status == 404) {
					alert('Requested page not found. [404]');
				} else if (jqXHR.status == 500) {			
					error_response = $(jqXHR.responseText).find("sword\\:verboseDescription").text();
					error_full_text = error_response.match(/SAXParseException.*/);
					
					if (error_full_text === null) {
						// There's nothing very useful to match on. So go to start of next stack trace line
						schematron_error_text = error_response.match(/UIPException[\s\S]*?edu/);
						
						if (schematron_error_text !== null) {
							// Transform error text into nicer format.
							schematron_text = schematron_error_text[0].split(/\s{2,}/);
							schematron_array = $.map(schematron_text, function(d) {
								return (d === '') ? null : d;
							});
							
							schematron_error_text[0] = schematron_array.join(' ').replace(/.{3}edu$/, '');
						}
					}
					
					if (error_full_text !== null && error_full_text.length) {
						error_response_msg(error_full_text);
					} else if (schematron_error_text !== null && schematron_error_text.length) {
						error_response_msg(schematron_error_text);
					} else {
						alert('Internal Server Error [500].');
					}
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
		
		if (vocabTerms) {
			var terms = vocabTerms["http://cdr.unc.edu/vocabulary/Affiliation"];
			if (terms) {
				terms = terms.sort();
				editorOptions["elementUpdated"] = function(event) {
					if (event.action == "render" && this.objectType.localName == "affiliation") {
						this.textInput.autocomplete({
							source : terms,
							minLength : 0
						});
					}
				}
			}
		}
	
		$("#xml_editor").xmlEditor(editorOptions);
		
		$(window).resize();
	});
});
