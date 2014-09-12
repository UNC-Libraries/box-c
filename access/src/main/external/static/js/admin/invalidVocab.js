require.config({
	urlArgs: "v=3.4-SNAPSHOT",
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
	pathname = pathname.substring(pathname.indexOf("/", pathname.indexOf("invalidVocab")) + 1);
	
	$.get("/admin/getInvalidVocab/" + pathname, function(response){
		var vocabTypes = response;
		
		var groupedTypes = {};
		
		for (var index in vocabTypes) {
			
			var vocabTypeList = vocabTypes[index];
			for (var resultIndex in vocabTypeList) {
				var vocabResult = vocabTypeList[resultIndex];
				
				var affilTag;
				for (var tagIndex in vocabResult.tags) {
					var tag = vocabResult.tags[tagIndex];
					if (tag.label == "invalid affiliation") {
						affilTag = tag;
						break;
					}
				}
				
				if (affilTag) {
					var groupedResults = groupedTypes["affiliation"];
					if (!groupedResults) {
						groupedResults = {};
						groupedTypes["affiliation"] = groupedResults;
					}
					
					for (var index in affilTag.details) {
						var detail = affilTag.details[index];
						var vocabResults = groupedResults[detail];
						if (!vocabResults) {
							vocabResults = [];
							groupedResults[detail] = vocabResults;
						}
						
						vocabResults.push(vocabResult);
					}
					
				}
			}
		}
		
		$("#invalid_vocab").html($(vocabTemplate({"groupedTypes" : groupedTypes})));
	});
});
