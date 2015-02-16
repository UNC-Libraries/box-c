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
	
	var pid = window.location.pathname;
	pid = pid.substring(pid.lastIndexOf("/") + 1);
	
	$.getJSON("describeInfo/" + pid, function(data) {
		var resultObject = data.resultObject;
		var vocabTerms = data.vocabTerms;
	
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
			label : 'View in CDR',
			enabled : true,
			binding : null,
			action : recordUrl
		}, {
			label : 'View in CDR',
			enabled : true, 
			itemClass : 'header_mode_tab',
			action : recordUrl
		}];
		if (originalUrl && originalUrl.length > 1){
			menuEntries.push({
				insertPath : ["View"],
				label : 'View original document',
				enabled : true,
				binding : null,
				action : originalUrl
			});
			menuEntries.push({
				label : 'Document',
				enabled : true, 
				itemClass : 'header_mode_tab',
				action : originalUrl
			});
		}
		
		var editorOptions = {
			schema : "../../static/schemas/mods-3-5.json",
			ajaxOptions : {
				xmlRetrievalPath : "/admin/" + resultObject.id + "/mods",
				xmlUploadPath : "/admin/describe/" + resultObject.id
			},
			libPath : "../../static/js/xmleditor/lib/",
			menuEntries: menuEntries,
			enforceOccurs: false,
			xmlEditorLabel : 'Form',
			textEditorLabel : 'XML'
		};
		
		if (vocabTerms) {
			var terms = vocabTerms["http://cdr.unc.edu/vocabulary/Affiliation"];
			if (terms) {
				terms = terms.sort();
				editorOptions["elementUpdated"] = function(event) {
					if (event.action == 'render' && this.objectType.localName == 'affiliation') {
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
