require.config({
	urlArgs: "v=3.4-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access',
		'text' : 'lib/text',
		'tpl' : 'lib/tpl',
		'underscore' : 'lib/underscore',
		'StructureEntry' : 'cdr-access',
		'StructureView' : 'cdr-access'
	},
	shim: {
		'jquery-ui' : ['jquery'],
		'underscore': {
			exports: '_'
		}
	}
});
define('containerRecord', ['module', 'jquery', 'StructureView'], function(module, $) {
	var $structureView = $(".structure");
	
	if ($structureView.length > 0) {
		$.ajax({
			url: "/structure/" + $structureView.attr('data-pid') + "/json?files=true",
			dataType : 'json',
			success: function(data){
				$structureView.structureView({
					showResourceIcons : true,
					showParentLink : false,
					rootNode : data.root,
					queryPath : 'list',
					retrieveFiles : true
				});
			},
			error: function(e){
				console.log("Failed to load", e);
			}
		});
	}
});
