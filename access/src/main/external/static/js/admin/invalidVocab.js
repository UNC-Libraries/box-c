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
	pathname = pathname.substring(pathname.indexOf("invalidVocab"));
	if (pathname.indexOf("/") > 0) {
		pathname = pathname.substring(pathname.indexOf("/", pathname) + 1);
	} else {
		pathname = "";
	}
	
	$.get("/admin/getInvalidVocab/" + pathname, function(response){
		var vocabTypes = response.vocabTypes;
		
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
				
				var vocabMap = {};
				var tags = vocabResult.tags;
				for (var index in tags) {
					if (tags[index].label.indexOf("invalid term ") == 0) {
						var vocabName = tags[index].label.substring("invalid term ".length);
						vocabMap[vocabName] = tags[index].details;
					}
				}
				
				if (!$.isEmptyObject(vocabMap)) {
					for (var vocabName in vocabMap) {
						var vocabDetails = vocabMap[vocabName];
						
						var groupedResults = groupedTypes[vocabName];
						if (!groupedResults) {
							groupedResults = {};
							groupedTypes[vocabName] = groupedResults;
						}
						
						for (var detailsIndex in vocabDetails) {
							var detail = vocabDetails[detailsIndex];
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
		}
		
		$("#invalid_vocab").html($(vocabTemplate({"groupedTypes" : groupedTypes, "container" : response.container})));
	});
});
