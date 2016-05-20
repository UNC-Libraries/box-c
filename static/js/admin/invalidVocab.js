require.config({
	urlArgs: "v=4.0-SNAPSHOT",
	baseUrl: "/static/js/",
	paths: {
		"jquery" : "cdr-admin",
		"jquery-ui" : "cdr-admin",
		"text" : "lib/text",
		"underscore" : "lib/underscore",
		"tpl" : "lib/tpl",
		"qtip" : "lib/jquery.qtip.min",

		"URLUtilities" : "cdr-admin",
		"moment" : "cdr-admin"
	},
	shim: {
		"qtip" : ["jquery"],
		"underscore": {
			exports: "_"
		}
	}
});

define("invalidVocab", ["jquery", "tpl!../templates/admin/invalidVocab"], function($, vocabTemplate) {
	
	var pathname = window.location.pathname;
	pathname = pathname.substring(pathname.indexOf("invalidVocab"));
	if (pathname.indexOf("/") > 0) {
		pathname = pathname.substring(pathname.indexOf("/", pathname) + 1);
	} else {
		pathname = "";
	}
	
	$.get("/admin/getInvalidVocab/" + pathname, function(response){
		var vocabTypes = response.vocabTypes;
		
		var groupedTypes = {};
		
		// Group results around each vocab term within a vocabulary
		for (var index in vocabTypes) {
			var vocabTypeList = vocabTypes[index];
			var groupedResults = {};
			groupedTypes[index] = groupedResults;
			
			for (var resultIndex in vocabTypeList) {
				var vocabResult = vocabTypeList[resultIndex];
				
				for (var termIndex in vocabResult.invalidTerms) {
					var term = vocabResult.invalidTerms[termIndex];
					
					var vocabResults = groupedResults[term];
					
					if (!vocabResults) {
						vocabResults = [];
						groupedResults[term] = vocabResults;
					}
				
					vocabResults.push(vocabResult);
				}
			}
		}
		
		$("#invalid_vocab").html($(vocabTemplate({"groupedTypes" : groupedTypes, "container" : response.container})));
	});
});
